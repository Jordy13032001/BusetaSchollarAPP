package com.example.busetaescolarapp.padre

data class Parada(
    val numero: Int,
    val nombre: String,
    val hora: String,
    val esDestinoPropio: Boolean = false,
    val lat: Double = 0.0,
    val lng: Double = 0.0
)
