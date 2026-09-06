package ch.opum.tricktrack.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale
import androidx.core.graphics.withTranslation
import ch.opum.tricktrack.R
import ch.opum.tricktrack.data.DistanceUnit
import ch.opum.tricktrack.data.TripWithVehicle
import ch.opum.tricktrack.util.DistanceFormatter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PdfGenerator {

    private val pageWidth = 595
    private val pageHeight = 842
    private val margin = 40f
    private val pageBottom = pageHeight - margin

    private lateinit var document: PdfDocument
    private var currentPage: PdfDocument.Page? = null
    private var canvas: Canvas? = null
    private var currentY = 0f
    private var dateRange: String = ""
    private var totalPages = 0
    private lateinit var context: Context
    private var distanceUnit: DistanceUnit = DistanceUnit.KM

    fun generateTripReport(
        context: Context,
        tripsWithVehicle: List<TripWithVehicle>,
        columns: Set<String>,
        isExpenseEnabled: Boolean,
        expenseRate: Float,
        expenseCurrency: String,
        driverName: String?,
        companyName: String?,
        vehicleName: String?,
        distanceUnit: DistanceUnit
    ): File? {
        if (tripsWithVehicle.isEmpty()) return null
        this.context = context
        this.distanceUnit = distanceUnit

        // Pass 1: Dry run to measure exact total page count
        totalPages = countPages(
            tripsWithVehicle = tripsWithVehicle,
            columns = columns,
            isExpenseEnabled = isExpenseEnabled,
            expenseRate = expenseRate,
            expenseCurrency = expenseCurrency,
            driverName = driverName,
            companyName = companyName,
            vehicleName = vehicleName
        )

        // Pass 2: Actual rendering with exact totalPages
        return buildPdf(
            tripsWithVehicle = tripsWithVehicle,
            columns = columns,
            isExpenseEnabled = isExpenseEnabled,
            expenseRate = expenseRate,
            expenseCurrency = expenseCurrency,
            driverName = driverName,
            companyName = companyName,
            vehicleName = vehicleName,
            isDryRun = false
        )
    }

    private fun countPages(
        tripsWithVehicle: List<TripWithVehicle>,
        columns: Set<String>,
        isExpenseEnabled: Boolean,
        expenseRate: Float,
        expenseCurrency: String,
        driverName: String?,
        companyName: String?,
        vehicleName: String?
    ): Int {
        buildPdf(
            tripsWithVehicle = tripsWithVehicle,
            columns = columns,
            isExpenseEnabled = isExpenseEnabled,
            expenseRate = expenseRate,
            expenseCurrency = expenseCurrency,
            driverName = driverName,
            companyName = companyName,
            vehicleName = vehicleName,
            isDryRun = true
        )
        return totalPages
    }

    private fun buildPdf(
        tripsWithVehicle: List<TripWithVehicle>,
        columns: Set<String>,
        isExpenseEnabled: Boolean,
        expenseRate: Float,
        expenseCurrency: String,
        driverName: String?,
        companyName: String?,
        vehicleName: String?,
        isDryRun: Boolean
    ): File? {
        document = PdfDocument()
        currentPage = null
        canvas = null
        currentY = 0f

        val groupedByMonth = tripsWithVehicle.groupBy {
            val cal = Calendar.getInstance()
            cal.time = it.trip.date
            SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
        }
        val totalDistance = tripsWithVehicle.sumOf { it.trip.distance }
        val totalExpenses = if (isExpenseEnabled) totalDistance * expenseRate else 0.0
        val minDate = tripsWithVehicle.minOf { it.trip.date }
        val maxDate = tripsWithVehicle.maxOf { it.trip.date }
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        dateRange = "${dateFormat.format(minDate)} - ${dateFormat.format(maxDate)}"

        // 1. Page 1: Summary
        drawSummaryPage(
            totalDistance = totalDistance,
            totalExpenses = totalExpenses,
            isExpenseEnabled = isExpenseEnabled,
            expenseCurrency = expenseCurrency,
            driverName = driverName,
            companyName = companyName,
            vehicleName = vehicleName,
            isDryRun = isDryRun
        )

        // Force trip list onto new page
        startNewPage(isDryRun = isDryRun)

        // 2. Monthly Lists
        val orderedColumns = listOf("DATE", "START_TIME", "END_TIME", "START_LOCATION", "END_LOCATION", "DISTANCE", "TYPE", "EXPENSES")
            .filter { columns.contains(it.replace("_TIME", "")) }
        val columnWidths = getColumnWidths(orderedColumns, isExpenseEnabled)

        groupedByMonth.forEach { (month, monthTrips) ->
            startNewPageIfNeeded(60f, isDryRun = isDryRun)
            drawMonthHeader(month)
            drawTableHeader(orderedColumns, columnWidths, isExpenseEnabled)

            monthTrips.forEach { item ->
                drawTripRow(item, orderedColumns, columnWidths, isExpenseEnabled, expenseRate, expenseCurrency, columns.contains("VEHICLE"), isDryRun = isDryRun)
            }
        }

        finishPage(isDryRun = isDryRun)

        if (isDryRun) {
            totalPages = document.pages.size
            document.close()
            return null
        }

        // Save the document
        return try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())
            val fileName = "tricktrack-trips_$timestamp.pdf"
            val file = File(context.cacheDir, fileName)
            val fos = FileOutputStream(file)
            document.writeTo(fos)
            document.close()
            fos.close()
            file
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun startNewPage(isDryRun: Boolean = false) {
        finishPage(isDryRun = isDryRun)
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, document.pages.size + 1).create()
        currentPage = document.startPage(pageInfo)
        canvas = currentPage!!.canvas
        currentY = margin
        drawPageHeader()
    }

    private fun finishPage(isDryRun: Boolean = false) {
        currentPage?.let {
            if (!isDryRun) {
                drawPageFooter(it.info.pageNumber)
            }
            document.finishPage(it)
        }
    }

    private fun startNewPageIfNeeded(neededHeight: Float, isDryRun: Boolean = false) {
        if (canvas == null || currentY + neededHeight > pageBottom) {
            startNewPage(isDryRun = isDryRun)
        }
    }

    private fun drawPageHeader() {
        val headerPaint = TextPaint().apply {
            color = Color.DKGRAY
            textSize = 10f
        }
        canvas?.drawText(dateRange, margin, margin, headerPaint)
    }

    private fun drawPageFooter(pageNumber: Int) {
        val footerPaint = TextPaint().apply {
            color = Color.DKGRAY
            textSize = 10f
            textAlign = Paint.Align.RIGHT
        }
        canvas?.drawText(context.getString(R.string.pdf_page_x_of_y, pageNumber, totalPages), pageWidth - margin, pageHeight - margin + 20, footerPaint)
    }

    private fun drawSummaryPage(
        totalDistance: Double,
        totalExpenses: Double,
        isExpenseEnabled: Boolean,
        expenseCurrency: String,
        driverName: String?,
        companyName: String?,
        vehicleName: String?,
        isDryRun: Boolean
    ) {
        startNewPage(isDryRun = isDryRun)

        // App Logo and Name (Top Right)
        val appNamePaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 16f
            textAlign = Paint.Align.RIGHT
        }
        val appName = "TrickTrack"
        val logoWidth = 40
        val spacing = 10

        val drawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
        drawable?.let {
            val logoBitmap = it.toBitmap(it.intrinsicWidth, it.intrinsicHeight, Bitmap.Config.ARGB_8888)
            val scaledLogo = logoBitmap.scale(logoWidth, logoWidth)
            val logoX = pageWidth - margin - scaledLogo.width
            val textX = logoX - spacing
            canvas?.drawBitmap(scaledLogo, logoX, margin, null)
            canvas?.drawText(appName, textX, margin + 25, appNamePaint)
        }

        // Centered Content
        val titlePaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 32f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val titleX = pageWidth / 2f
        val titleY = pageHeight / 2f - 200

        canvas?.drawText(context.getString(R.string.pdf_trip_report_title), titleX, titleY, titlePaint)

        // Summary Box
        val lineSpacing = 28f
        val topPadding = 30f
        val bottomPadding = 20f
        val dividerPadding = 15f
        val leftTextMargin = margin + 20f
        val valueTextMargin = margin + 150f

        var lineCount = 2
        if (isExpenseEnabled) lineCount++
        val hasDriverInfo = driverName != null || companyName != null || vehicleName != null
        if (driverName != null) lineCount++
        if (companyName != null) lineCount++
        if (vehicleName != null) lineCount++

        val dividerHeight = if (hasDriverInfo) (dividerPadding * 2) else 0f
        val contentHeight = (lineCount - 1) * lineSpacing
        val boxHeight = topPadding + contentHeight + dividerHeight + bottomPadding

        val summaryBoxTop = titleY + 50
        val summaryBox = Rect(margin.toInt(), summaryBoxTop.toInt(), (pageWidth - margin).toInt(), (summaryBoxTop + boxHeight).toInt())

        val boxPaint = Paint().apply {
            color = Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas?.drawRect(summaryBox, boxPaint)

        val labelPaint = TextPaint().apply {
            color = Color.DKGRAY
            textSize = 12f
        }
        val valuePaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 14f
            isFakeBoldText = true
        }

        var summaryY = summaryBoxTop + topPadding

        if (hasDriverInfo) {
            driverName?.let {
                canvas?.drawText(context.getString(R.string.pdf_label_driver), leftTextMargin, summaryY, labelPaint)
                canvas?.drawText(it, valueTextMargin, summaryY, valuePaint)
                summaryY += lineSpacing
            }
            companyName?.let {
                canvas?.drawText(context.getString(R.string.pdf_label_company), leftTextMargin, summaryY, labelPaint)
                canvas?.drawText(it, valueTextMargin, summaryY, valuePaint)
                summaryY += lineSpacing
            }
            vehicleName?.let {
                canvas?.drawText(context.getString(R.string.pdf_label_vehicle), leftTextMargin, summaryY, labelPaint)
                canvas?.drawText(it, valueTextMargin, summaryY, valuePaint)
                summaryY += lineSpacing
            }

            summaryY += dividerPadding - (lineSpacing / 2)
            canvas?.drawLine(margin + 10, summaryY, pageWidth - margin - 10, summaryY, boxPaint)
            summaryY += dividerPadding + (lineSpacing / 2)
        }

        canvas?.drawText(context.getString(R.string.pdf_date_range), leftTextMargin, summaryY, labelPaint)
        canvas?.drawText(dateRange, valueTextMargin, summaryY, valuePaint)
        summaryY += lineSpacing

        canvas?.drawText(context.getString(R.string.pdf_total_distance), leftTextMargin, summaryY, labelPaint)
        canvas?.drawText(DistanceFormatter.format(totalDistance, distanceUnit), valueTextMargin, summaryY, valuePaint)

        if (isExpenseEnabled) {
            summaryY += lineSpacing
            canvas?.drawText(context.getString(R.string.pdf_total_expenses), leftTextMargin, summaryY, labelPaint)
            canvas?.drawText("%.2f %s".format(totalExpenses, expenseCurrency), valueTextMargin, summaryY, valuePaint)
        }
    }

    private fun drawMonthHeader(month: String) {
        val headerPaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 18f
            isFakeBoldText = true
        }
        currentY += 40f
        canvas?.drawText(month, margin, currentY, headerPaint)
        currentY += 25f
    }

    private fun getColumnWidths(columns: List<String>, isExpenseEnabled: Boolean): Map<String, Float> {
        val availableWidth = pageWidth - (2 * margin)
        val weights = mutableMapOf<String, Float>()
        columns.forEach {
            when (it) {
                "DATE" -> weights[it] = 1.5f
                "START_TIME", "END_TIME" -> weights[it] = 1.5f
                "START_LOCATION", "END_LOCATION" -> weights[it] = 3f
                "DISTANCE", "TYPE" -> weights[it] = 1.5f
                "EXPENSES" -> if (isExpenseEnabled) weights[it] = 1.5f
            }
        }

        val totalWeight = weights.values.sum()
        return weights.mapValues { (it.value / totalWeight) * availableWidth }
    }

    private fun drawTableHeader(columns: List<String>, columnWidths: Map<String, Float>, isExpenseEnabled: Boolean) {
        val headerPaint = TextPaint().apply {
            color = Color.DKGRAY
            textSize = 10f
            isFakeBoldText = true
        }
        var currentX = margin
        columns.forEach { column ->
            val title = when (column) {
                "DATE" -> context.getString(R.string.pdf_header_date)
                "START_TIME" -> context.getString(R.string.pdf_header_start_time)
                "END_TIME" -> context.getString(R.string.pdf_header_end_time)
                "START_LOCATION" -> context.getString(R.string.pdf_header_from)
                "END_LOCATION" -> context.getString(R.string.pdf_header_to)
                "TYPE" -> context.getString(R.string.pdf_header_purpose)
                "DISTANCE" -> context.getString(R.string.pdf_header_distance)
                "EXPENSES" -> if (isExpenseEnabled) context.getString(R.string.pdf_header_expenses) else ""
                else -> ""
            }
            if (title.isNotEmpty()) {
                canvas?.drawText(title, currentX, currentY, headerPaint)
                currentX += columnWidths[column] ?: 0f
            }
        }
        currentY += 15f
    }

    private fun drawTripRow(
        item: TripWithVehicle,
        columns: List<String>,
        columnWidths: Map<String, Float>,
        isExpenseEnabled: Boolean,
        expenseRate: Float,
        expenseCurrency: String,
        includeVehicle: Boolean,
        isDryRun: Boolean
    ) {
        val trip = item.trip
        val textPaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 10f
        }
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        val dateFormat = SimpleDateFormat("dd.MM", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        val cellLayouts = mutableMapOf<String, StaticLayout>()
        var maxRowHeight = 0f

        columns.forEach { column ->
            val text = when (column) {
                "DATE" -> {
                    if (includeVehicle && item.vehicle != null) {
                        dateFormat.format(trip.date) + "\n" + item.vehicle.licensePlate
                    } else {
                        dateFormat.format(trip.date)
                    }
                }
                "START_TIME" -> timeFormat.format(trip.date)
                "END_TIME" -> timeFormat.format(Date(trip.endDate))
                "START_LOCATION" -> trip.startLoc.replace(", ", "\n")
                "END_LOCATION" -> trip.endLoc.replace(", ", "\n")
                "TYPE" -> trip.type
                "DISTANCE" -> DistanceFormatter.formatShort(trip.distance, distanceUnit)
                "EXPENSES" -> if (isExpenseEnabled) "%.2f %s".format(trip.distance * expenseRate, expenseCurrency) else ""
                else -> ""
            }
            if (text.isNotEmpty()) {
                val colWidth = (columnWidths[column] ?: 0f).toInt()
                val layout = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, colWidth).build()
                cellLayouts[column] = layout
                if (layout.height > maxRowHeight) {
                    maxRowHeight = layout.height.toFloat()
                }
            }
        }
        maxRowHeight += 20f

        startNewPageIfNeeded(maxRowHeight, isDryRun = isDryRun)

        if (!isDryRun) {
            var currentX = margin
            columns.forEach { column ->
                cellLayouts[column]?.let { layout ->
                    canvas?.withTranslation(currentX, currentY + 10f) {
                        layout.draw(this)
                    }
                    currentX += columnWidths[column] ?: 0f
                }
            }
            canvas?.drawLine(margin, currentY + maxRowHeight - 5f, pageWidth - margin, currentY + maxRowHeight - 5f, linePaint)
        }

        currentY += maxRowHeight
    }
}
