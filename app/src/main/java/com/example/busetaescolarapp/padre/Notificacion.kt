package com.example.busetaescolarapp.padre

data class Notificacion(
    val titulo: String,
    val mensaje: String,
    val hora: String,
    val tipo: TipoNotificacion // Para cambiar color/icono despues si lo desean
)

enum class TipoNotificacion {
    CERCA, SUBIO, FINALIZADA, ALERTA
}
