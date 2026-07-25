package com.example.busetaescolarapp.data.local

import androidx.room.Entity

/**
 * Un tramo = el camino real (siguiendo calles) entre una parada y la siguiente,
 * obtenido UNA vez de la Google Directions API y cacheado aquí para no volver
 * a gastar tokens en el mismo día.
 */
@Entity(tableName = "tramo_ruta", primaryKeys = ["choferEmail", "fecha", "orden"])
data class TramoEntity(
    val choferEmail: String,
    val fecha: String, // yyyy-MM-dd
    val orden: Int,
    val origenDireccion: String,
    val destinoDireccion: String,
    val distanciaMetros: Int,
    val duracionSegundos: Int,
    val puntosTramo: String // "lat1,lng1;lat2,lng2;..." (ver PolylineUtils)
)
