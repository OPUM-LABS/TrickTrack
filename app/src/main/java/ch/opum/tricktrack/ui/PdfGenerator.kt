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
import ch.opum.tricktrack.data.Trip
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

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

    fun generateTripReport(
        context: Context,
        trips: List<Trip>,
        columns: Set<String>,
        isExpenseEnabled: Boolean,
        expenseRate: Float,
        expenseCurrency: String
    ): File? {
        if (trips.isEmpty()) return null
        this.context = context

        // First pass: Calculate total pages
        totalPages = calculateTotalPages(trips, columns, isExpenseEnabled)

        document = PdfDocument()

        // Second pass: Draw the document
        // 1. Data Preparation
        val groupedByMonth = trips.groupBy {
            val cal = Calendar.getInstance()
            cal.time = it.date
            SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
        }
        val totalDistance = trips.sumOf { it.distance }
        val totalExpenses = if (isExpenseEnabled) totalDistance * expenseRate else 0.0
        val minDate = trips.minOf { it.date }
        val maxDate = trips.maxOf { it.date }
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        dateRange = "${dateFormat.format(minDate)} - ${dateFormat.format(maxDate)}"

        // 2. Page 1: The Summary
        drawSummaryPage(
            totalDistance = totalDistance,
            totalExpenses = totalExpenses,
            isExpenseEnabled = isExpenseEnabled,
            expenseCurrency = expenseCurrency
        )

        // Force the trip list to start on a new page
        startNewPage()

        // 3. Page 2+: The Monthly Lists
        val orderedColumns = listOf("DATE", "START_TIME", "END_TIME", "START_LOCATION", "END_LOCATION", "DISTANCE", "TYPE", "EXPENSES")
            .filter { columns.contains(it.replace("_TIME", "")) }
        val columnWidths = getColumnWidths(orderedColumns, isExpenseEnabled)
        groupedByMonth.forEach { (month, monthTrips) ->
            startNewPageIfNeeded(60f) // Space for month header
            drawMonthHeader(month)
            drawTableHeader(orderedColumns, columnWidths, isExpenseEnabled)

            monthTrips.forEach { trip ->
                drawTripRow(trip, orderedColumns, columnWidths, isExpenseEnabled, expenseRate, expenseCurrency)
            }
        }

        finishPage()

        // 4. Save the document
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

    private fun calculateTotalPages(
        trips: List<Trip>,
        columns: Set<String>,
        isExpenseEnabled: Boolean
    ): Int {
        // This is a simplified calculation. A more accurate one would require a full layout pass.
        var pageCount = 2 // Summary page + at least one trip page
        val groupedByMonth = trips.groupBy {
            val cal = Calendar.getInstance()
            cal.time = it.date
            SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
        }
        val orderedColumns = listOf("DATE", "START_TIME", "END_TIME", "START_LOCATION", "END_LOCATION", "DISTANCE", "TYPE", "EXPENSES")
            .filter { columns.contains(it.replace("_TIME", "")) }
        val columnWidths = getColumnWidths(orderedColumns, isExpenseEnabled)

        var tempY = margin + 60f
        groupedByMonth.forEach { (_, monthTrips) ->
            tempY += 60f // Month header
            tempY += 15f // Table header
            monthTrips.forEach { trip ->
                var maxRowHeight = 0f
                orderedColumns.forEach { column ->
                    val text = when (column) {
                        "DATE" -> "01.01"
                        "START_TIME" -> "00:00"
                        "END_TIME" -> "00:00"
                        "START_LOCATION" -> trip.startLoc.replace(", ", "\n")
                        "END_LOCATION" -> trip.endLoc.replace(", ", "\n")
                        "TYPE" -> trip.type
                        "DISTANCE" -> "0.00 km"
                        "EXPENSES" -> if (isExpenseEnabled) "0.00" else ""
                        else -> ""
                    }
                    if (text.isNotEmpty()) {
                        val colWidth = (columnWidths[column] ?: 0f).toInt()
                        val textPaint = TextPaint()
                        val layout = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, colWidth).build()
                        if (layout.height > maxRowHeight) {
                            maxRowHeight = layout.height.toFloat()
                        }
                    }
                }
                tempY += maxRowHeight + 20f
                if (tempY > pageBottom) {
                    pageCount++
                    tempY = margin + 60f
                }
            }
        }
        return pageCount
    }

    private fun startNewPage() {
        finishPage()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, document.pages.size + 1).create()
        currentPage = document.startPage(pageInfo)
        canvas = currentPage!!.canvas
        currentY = margin
        drawPageHeader()
    }

    private fun finishPage() {
        currentPage?.let {
            drawPageFooter(it.info.pageNumber)
            document.finishPage(it)
        }
    }

    private fun startNewPageIfNeeded(neededHeight: Float) {
        if (canvas == null || currentY + neededHeight > pageBottom) {
            startNewPage()
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
        expenseCurrency: String
    ) {
        startNewPage()

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
        val titleY = pageHeight / 2f - 100 // Adjust as needed

        canvas?.drawText(context.getString(R.string.pdf_trip_report_title), titleX, titleY, titlePaint)

        // Summary Box
        val boxHeight = if (isExpenseEnabled) 120f else 90f
        val summaryBoxTop = titleY + 20
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

        var summaryY = summaryBoxTop + 40f
        canvas?.drawText(context.getString(R.string.pdf_date_range), margin + 20f, summaryY, labelPaint)
        canvas?.drawText(dateRange, margin + 150f, summaryY, valuePaint)
        summaryY += 30f
        canvas?.drawText(context.getString(R.string.pdf_total_distance), margin + 20f, summaryY, labelPaint)
        canvas?.drawText("%.2f km".format(totalDistance), margin + 150f, summaryY, valuePaint)

        if (isExpenseEnabled) {
            summaryY += 30f
            canvas?.drawText(context.getString(R.string.pdf_total_expenses), margin + 20f, summaryY, labelPaint)
            canvas?.drawText("%.2f %s".format(totalExpenses, expenseCurrency), margin + 150f, summaryY, valuePaint)
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
        trip: Trip,
        columns: List<String>,
        columnWidths: Map<String, Float>,
        isExpenseEnabled: Boolean,
        expenseRate: Float,
        expenseCurrency: String
    ) {
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

        // Prepare cell content and layouts
        val cellLayouts = mutableMapOf<String, StaticLayout>()
        var maxRowHeight = 0f

        columns.forEach { column ->
            val text = when (column) {
                "DATE" -> dateFormat.format(trip.date)
                "START_TIME" -> timeFormat.format(trip.date)
                "END_TIME" -> timeFormat.format(Date(trip.endDate))
                "START_LOCATION" -> trip.startLoc.replace(", ", "\n")
                "END_LOCATION" -> trip.endLoc.replace(", ", "\n")
                "TYPE" -> trip.type
                "DISTANCE" -> "%.2f km".format(trip.distance)
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
        maxRowHeight += 20f // Add padding

        startNewPageIfNeeded(maxRowHeight)

        // Draw the cells
        var currentX = margin
        columns.forEach { column ->
            cellLayouts[column]?.let { layout ->
                canvas?.withTranslation(currentX, currentY + 10f) {
                    layout.draw(this)
                }
                currentX += columnWidths[column] ?: 0f
            }
        }

        currentY += maxRowHeight
        canvas?.drawLine(margin, currentY - 5f, pageWidth - margin, currentY - 5f, linePaint)
    }
}
