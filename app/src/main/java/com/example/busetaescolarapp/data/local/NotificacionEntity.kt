package com.example.busetaescolarapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notificaciones")
data class NotificacionEntity(
    @PrimaryKey val id: Int,
    val correoPadre: String,
    val titulo: String,
    val mensaje: String,
    val hora: String,
    val tipo: String,
    // Solo llegan en las notificaciones de solicitud aceptada/rechazada: el padre
    // los necesita para saber sobre qué hijo va a pagar o reenviar la solicitud.
    val idEstudiante: Int? = null,
    val nombreEstudiante: String? = null,
    val estadoEstudiante: String? = null
)
