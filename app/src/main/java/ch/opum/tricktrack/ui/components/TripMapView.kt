package ch.opum.tricktrack.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import ch.opum.tricktrack.R
import ch.opum.tricktrack.GeocoderHelper
import ch.opum.tricktrack.data.repository.DistanceRepository
import ch.opum.tricktrack.util.PolylineUtils
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import kotlin.math.abs

@SuppressLint("ClickableViewAccessibility")
@Composable
fun TripMapView(
    startLat: Double?,
    startLon: Double?,
    endLat: Double?,
    endLon: Double?,
    modifier: Modifier = Modifier,
    startAddress: String? = null,
    endAddress: String? = null,
    routePolyline: String? = null,
    isInteractive: Boolean = false,
    onRouteCalculated: ((String) -> Unit)? = null,
    onResolvedCoords: ((Double, Double, Double, Double, String?) -> Unit)? = null,
    onRefresh: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current

    remember {
        Configuration.getInstance().userAgentValue = context.packageName
        true
    }

    fun isValidCoord(lat: Double?, lon: Double?): Boolean {
        return lat != null && lon != null && (abs(lat) > 0.001 || abs(lon) > 0.001)
    }

    var resolvedStart by remember(startLat, startLon, startAddress) {
        mutableStateOf(if (isValidCoord(startLat, startLon)) GeoPoint(startLat!!, startLon!!) else null)
    }
    var resolvedEnd by remember(endLat, endLon, endAddress) {
        mutableStateOf(if (isValidCoord(endLat, endLon)) GeoPoint(endLat!!, endLon!!) else null)
    }

    var osrmPoints by remember(startLat, startLon, endLat, endLon, routePolyline) { mutableStateOf<List<GeoPoint>?>(null) }

    // Asynchronously resolve missing coordinates or route geometries
    LaunchedEffect(startLat, startLon, endLat, endLon, startAddress, endAddress, routePolyline) {
        val geocoder = GeocoderHelper(context)

        var sPoint = resolvedStart
        if (sPoint == null && !startAddress.isNullOrBlank()) {
            val coords = geocoder.getCoordinatesFromAddress(startAddress)
            if (coords != null && isValidCoord(coords.first, coords.second)) {
                sPoint = GeoPoint(coords.first, coords.second)
                resolvedStart = sPoint
            }
        }

        var ePoint = resolvedEnd
        if (ePoint == null && !endAddress.isNullOrBlank()) {
            val coords = geocoder.getCoordinatesFromAddress(
                address = endAddress,
                biasLat = sPoint?.latitude,
                biasLon = sPoint?.longitude
            )
            if (coords != null && isValidCoord(coords.first, coords.second)) {
                ePoint = GeoPoint(coords.first, coords.second)
                resolvedEnd = ePoint
            }
        }

        if (sPoint != null && isValidCoord(sPoint.latitude, sPoint.longitude) && ePoint != null && isValidCoord(ePoint.latitude, ePoint.longitude)) {
            if (routePolyline.isNullOrBlank()) {
                val repository = DistanceRepository(context)
                val roadPoints = if (sPoint != ePoint) {
                    repository.getOsrmRouteGeometry(sPoint.latitude, sPoint.longitude, ePoint.latitude, ePoint.longitude)
                } else null

                if (!roadPoints.isNullOrEmpty()) {
                    osrmPoints = roadPoints
                    val encoded = PolylineUtils.encode(roadPoints)
                    onRouteCalculated?.invoke(encoded)
                    onResolvedCoords?.invoke(sPoint.latitude, sPoint.longitude, ePoint.latitude, ePoint.longitude, encoded)
                } else {
                    onResolvedCoords?.invoke(sPoint.latitude, sPoint.longitude, ePoint.latitude, ePoint.longitude, null)
                }
            } else if (!isValidCoord(startLat, startLon) || !isValidCoord(endLat, endLon)) {
                onResolvedCoords?.invoke(sPoint.latitude, sPoint.longitude, ePoint.latitude, ePoint.longitude, routePolyline)
            }
        }
    }

    val startPoint = resolvedStart
    val endPoint = resolvedEnd ?: startPoint

    if (startPoint == null && endPoint == null) {
        return
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(isInteractive)
                        isClickable = isInteractive

                        if (isInteractive) {
                            setOnTouchListener { v, event ->
                                v.parent?.requestDisallowInterceptTouchEvent(true)
                                if (event.action == MotionEvent.ACTION_UP) {
                                    v.performClick()
                                }
                                false
                            }
                        } else if (onClick != null) {
                            setOnTouchListener { v, _ ->
                                v.performClick()
                                onClick.invoke()
                                true
                            }
                        }
                    }
                },
                update = { mapView ->
                    mapView.onResume()
                    mapView.overlays.clear()

                    val linePoints = mutableListOf<GeoPoint>()

                    // Priority 1: Decoded recorded GPS polyline
                    if (!routePolyline.isNullOrBlank()) {
                        val decoded = PolylineUtils.decode(routePolyline)
                        if (decoded.isNotEmpty()) {
                            linePoints.addAll(decoded)
                        }
                    }

                    // Priority 2: Fetched OSRM road geometry
                    if (linePoints.isEmpty() && !osrmPoints.isNullOrEmpty()) {
                        linePoints.addAll(osrmPoints!!)
                    }

                    // Priority 3: Fallback straight line between start and end
                    if (linePoints.isEmpty()) {
                        startPoint?.let { linePoints.add(it) }
                        endPoint?.let { if (it != startPoint) linePoints.add(it) }
                    }

                    // Add Start Marker
                    val effectiveStart = linePoints.firstOrNull() ?: startPoint
                    if (effectiveStart != null) {
                        val startMarker = Marker(mapView).apply {
                            position = effectiveStart
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = "Start"
                            icon = createMapPinDrawable(context, "#2E7D32".toColorInt())
                        }
                        mapView.overlays.add(startMarker)
                    }

                    // Add End Marker
                    val effectiveEnd = linePoints.lastOrNull() ?: endPoint
                    if (effectiveEnd != null && effectiveEnd != effectiveStart) {
                        val endMarker = Marker(mapView).apply {
                            position = effectiveEnd
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = "End"
                            icon = createMapPinDrawable(context, "#C62828".toColorInt())
                        }
                        mapView.overlays.add(endMarker)
                    }

                    // Draw Route Polyline
                    if (linePoints.size >= 2) {
                        val line = Polyline(mapView).apply {
                            setPoints(linePoints)
                            outlinePaint.color = Color(0xFF1976D2).toArgb()
                            outlinePaint.strokeWidth = 8f
                        }
                        mapView.overlays.add(line)

                        // Compute Bounding Box
                        val lats = linePoints.map { it.latitude }
                        val lons = linePoints.map { it.longitude }
                        val maxLat = lats.maxOrNull() ?: (startPoint?.latitude ?: 0.0)
                        val minLat = lats.minOrNull() ?: (startPoint?.latitude ?: 0.0)
                        val maxLon = lons.maxOrNull() ?: (startPoint?.longitude ?: 0.0)
                        val minLon = lons.minOrNull() ?: (startPoint?.longitude ?: 0.0)

                        val box = BoundingBox(maxLat + 0.003, maxLon + 0.003, minLat - 0.003, minLon - 0.003)
                        mapView.post {
                            mapView.zoomToBoundingBox(box, false)
                        }
                    } else if (effectiveStart != null) {
                        mapView.controller.setZoom(15.0)
                        mapView.controller.setCenter(effectiveStart)
                    }

                    mapView.invalidate()
                }
            )

            if (onRefresh != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                ) {
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.action_refresh_map),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun createMapPinDrawable(context: Context, colorInt: Int): Drawable {
    val density = context.resources.displayMetrics.density
    val size = (32 * density).toInt()
    val bitmap = createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    val cx = size / 2f
    val cy = size * 0.38f
    val radius = size * 0.34f

    // 1. Draw pin head background (white border)
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(cx, cy, radius, paint)

    // 2. Draw pin head fill (colored)
    paint.color = colorInt
    canvas.drawCircle(cx, cy, radius * 0.82f, paint)

    // 3. Draw center white dot
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(cx, cy, radius * 0.35f, paint)

    // 4. Draw pointer tip
    val path = Path().apply {
        moveTo(cx - radius * 0.45f, cy + radius * 0.45f)
        lineTo(cx, size.toFloat() - 1f)
        lineTo(cx + radius * 0.45f, cy + radius * 0.45f)
        close()
    }
    paint.color = colorInt
    canvas.drawPath(path, paint)

    return bitmap.toDrawable(context.resources)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullscreenMapSheet(
    startLat: Double?,
    startLon: Double?,
    endLat: Double?,
    endLon: Double?,
    title: String,
    modifier: Modifier = Modifier,
    startAddress: String? = null,
    endAddress: String? = null,
    routePolyline: String? = null,
    onRouteCalculated: ((String) -> Unit)? = null,
    onResolvedCoords: ((Double, Double, Double, Double, String?) -> Unit)? = null,
    onRefresh: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier.fillMaxHeight(0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onRefresh != null) {
                        IconButton(onClick = onRefresh) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.action_refresh_map),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.button_close))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TripMapView(
                startLat = startLat,
                startLon = startLon,
                endLat = endLat,
                endLon = endLon,
                startAddress = startAddress,
                endAddress = endAddress,
                routePolyline = routePolyline,
                modifier = Modifier.fillMaxSize(),
                isInteractive = true,
                onRouteCalculated = onRouteCalculated,
                onResolvedCoords = onResolvedCoords,
                onRefresh = null
            )
        }
    }
}
