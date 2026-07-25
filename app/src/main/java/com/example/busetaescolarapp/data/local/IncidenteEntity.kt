package com.example.busetaescolarapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "incidentes")
data class IncidenteEntity(
    @PrimaryKey val idIncidente: Int,
    val correoPadre: String,
    val mensaje: String,
    val estado: String,
    val fechaHora: String
)
