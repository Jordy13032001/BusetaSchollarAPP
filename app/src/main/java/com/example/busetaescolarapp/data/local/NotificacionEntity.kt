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
    val tipo: String
)
