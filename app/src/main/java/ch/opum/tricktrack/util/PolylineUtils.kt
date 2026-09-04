package ch.opum.tricktrack.util

import org.osmdroid.util.GeoPoint
import kotlin.math.round

object PolylineUtils {

    /**
     * Encodes a list of GeoPoints into a Google Polyline format string.
     */
    fun encode(points: List<GeoPoint>): String {
        val result = StringBuilder()
        var lastLat = 0
        var lastLng = 0

        for (point in points) {
            val lat = round(point.latitude * 1e5).toInt()
            val lng = round(point.longitude * 1e5).toInt()

            val dLat = lat - lastLat
            val dLng = lng - lastLng

            encodeValue(dLat, result)
            encodeValue(dLng, result)

            lastLat = lat
            lastLng = lng
        }
        return result.toString()
    }

    /**
     * Decodes a Google Polyline format string into a list of GeoPoints.
     * Safely handles truncated or malformed polyline strings without throwing StringIndexOutOfBoundsException.
     */
    fun decode(encoded: String): List<GeoPoint> {
        if (encoded.isBlank()) return emptyList()
        return try {
            val poly = mutableListOf<GeoPoint>()
            var index = 0
            val len = encoded.length
            var lat = 0
            var lng = 0

            while (index < len) {
                var b: Int
                var shift = 0
                var result = 0
                do {
                    if (index >= len) return poly
                    b = encoded[index++].code - 63
                    result = result or (b and 0x1f shl shift)
                    shift += 5
                } while (b >= 0x20)
                val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
                lat += dlat

                shift = 0
                result = 0
                do {
                    if (index >= len) return poly
                    b = encoded[index++].code - 63
                    result = result or (b and 0x1f shl shift)
                    shift += 5
                } while (b >= 0x20)
                val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
                lng += dlng

                val p = GeoPoint(lat.toDouble() / 1E5, lng.toDouble() / 1E5)
                poly.add(p)
            }
            poly
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun encodeValue(value: Int, result: StringBuilder) {
        var v = if (value < 0) (value shl 1).inv() else value shl 1
        while (v >= 0x20) {
            result.append((((v and 0x1f) or 0x20) + 63).toChar())
            v = v shr 5
        }
        result.append((v + 63).toChar())
    }
}
