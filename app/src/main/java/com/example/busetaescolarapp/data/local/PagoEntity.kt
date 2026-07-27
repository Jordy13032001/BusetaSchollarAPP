package com.example.busetaescolarapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// Pago simulado del cupo mensual. Se guarda solo en el teléfono (Room):
// no existe un modelo de pagos en el backend y la pasarela es una simulación.
@Entity(tableName = "pagos")
data class PagoEntity(
    @PrimaryKey(autoGenerate = true) val idPago: Int = 0,
    val correoPadre: String,
    val idEstudiante: Int,
    val nombreEstudiante: String,
    val nombreChofer: String,
    val monto: Double,
    val metodo: String,
    // Últimos 4 dígitos: nunca se guarda el número completo de la tarjeta.
    val referencia: String,
    val fechaHora: Long = System.currentTimeMillis()
)
