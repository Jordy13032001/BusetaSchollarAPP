package com.example.busetaescolarapp.padre

data class Notificacion(
    val titulo: String,
    val mensaje: String,
    val hora: String,
    val tipo: TipoNotificacion, // Para cambiar color/icono despues si lo desean
    // Solo vienen en SOLICITUD_ACEPTADA / SOLICITUD_RECHAZADA
    val idEstudiante: Int? = null,
    val nombreEstudiante: String? = null,
    val estadoEstudiante: String? = null
)

enum class TipoNotificacion {
    CERCA, SUBIO, FINALIZADA, ALERTA, SOLICITUD_ACEPTADA, SOLICITUD_RECHAZADA
}
