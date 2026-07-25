package com.example.busetaescolarapp.utils

import com.google.android.gms.maps.model.LatLng

object PolylineUtils {

    /** Decodifica el formato "encoded polyline" que devuelve la Google Directions API. */
    fun decode(encoded: String): List<LatLng> {
        val points = mutableListOf<LatLng>()
        var index = 0
        var lat = 0
        var lng = 0

        while (index < encoded.length) {
            var result = 1
            var shift = 0
            var b: Int
            do {
                b = encoded[index++].code - 63 - 1
                result += b shl shift
                shift += 5
            } while (b >= 0x1f)
            lat += if (result and 1 != 0) (result shr 1).inv() else (result shr 1)

            result = 1
            shift = 0
            do {
                b = encoded[index++].code - 63 - 1
                result += b shl shift
                shift += 5
            } while (b >= 0x1f)
            lng += if (result and 1 != 0) (result shr 1).inv() else (result shr 1)

            points.add(LatLng(lat / 1e5, lng / 1e5))
        }
        return points
    }

    /** Serializa una lista de puntos a un texto plano para guardarlo en Room (no requiere re-codificar). */
    fun serialize(points: List<LatLng>): String =
        points.joinToString(";") { "${it.latitude},${it.longitude}" }

    fun deserialize(text: String): List<LatLng> {
        if (text.isBlank()) return emptyList()
        return text.split(";").mapNotNull { par ->
            val partes = par.split(",")
            if (partes.size == 2) {
                val lat = partes[0].toDoubleOrNull()
                val lng = partes[1].toDoubleOrNull()
                if (lat != null && lng != null) LatLng(lat, lng) else null
            } else null
        }
    }
}
