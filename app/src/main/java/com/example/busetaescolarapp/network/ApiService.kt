package com.example.busetaescolarapp.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.GET
import retrofit2.http.Path

// Modelos de datos para las peticiones y respuestas
data class RegistroRequest(
    val name: String,
    val email: String,
    val phone: String,
    val password: String,
    val role: String
)

data class DriverRequest(
    val email: String,
    val licencia: String,
    val placa: String,
    val modelo: String,
    val capacidad: Int,
    val tarifa_mensual: Double
)

data class EstadoVehiculoResponse(
    val estado: String,
    val placa: String?,
    val modelo: String?,
    val capacidad: Int?
)

data class ApiResponse(
    val message: String?,
    val error: String?,
    val user: User?
)

data class User(
    val id: Int,
    val name: String,
    val email: String,
    val phone: String?,
    val role: String,
    val roles: List<String> = emptyList()
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class ChoferResponse(
    val id_chofer: Int,
    val nombre_completo: String,
    val correo: String,
    val tarifa_mensual: Double?,
    val placa: String?,
    val modelo: String?,
    // Ruta que definió el chofer: el padre la revisa antes de contratarlo
    val nombre_ruta: String?,
    val turno: String?,
    val sectores: String?,
    val hora_salida: String?,
    val colegio: String?
)

data class RutaInfoResponse(
    val id_ruta: Int,
    val nombre: String,
    val turno: String,
    val sectores: String?,
    val hora_salida: String?,
    val colegio: String?,
    val id_colegio: Int?,
    val lat_colegio: Double?,
    val lng_colegio: Double?
)

data class RutaInfoRequest(
    val nombre: String,
    val turno: String,
    val sectores: String?,
    val hora_salida: String?,
    val id_colegio: Int? = null
)

data class ColegioResponse(
    val id_colegio: Int,
    val nombre: String
)


data class SolicitudResponse(
    val id_solicitud: Int,
    val nombre_completo: String,
    val correo: String,
    val placa: String,
    val modelo: String,
    val capacidad: Int,
    val tarifa_mensual: Double,
    val fecha_creacion: String
)

data class EstudianteRequest(
    val nombre_completo: String,
    val direccion: String,
    val lat: Double?,
    val lng: Double?,
    val correo_padre: String,
    val correo_chofer: String
)

data class EstudianteResponse(
    val id_estudiante: Int,
    val nombre_completo: String,
    val direccion: String,
    val hora_estimada: String?,
    val lat: Double?,
    val lng: Double?,
    val correo_padre: String,
    val correo_chofer: String,
    val subio: Boolean?,
    val estado: String? = null,
    val nombre_chofer: String? = null
)

data class NotificationResponse(
    val id: Int,
    val title: String,
    val message: String,
    val timestamp: String,
    val type: String,
    val parent_email: String,
    // Solo vienen en las notificaciones de solicitud aceptada/rechazada
    val id_estudiante: Int? = null,
    val nombre_estudiante: String? = null,
    val estado_estudiante: String? = null
)

// Reenvío de la solicitud de un hijo rechazado a otro chofer.
// La dirección es opcional: si no se manda, el backend conserva la parada anterior.
data class ReasignarRequest(
    val correo_chofer: String,
    val direccion: String? = null,
    val lat: Double? = null,
    val lng: Double? = null
)

data class IncidentRequest(
    val description: String,
    val parent_email: String
)

data class IncidentResponse(
    val id_incidente: Int,
    val mensaje: String,
    val estado: String,
    val fecha_hora: String
)

data class DriverLocationRequest(
    val driver_email: String,
    val lat: Double,
    val lng: Double,
    val id_viaje: Int
)

data class DriverLocationResponse(
    val driver_email: String,
    val lat: Double,
    val lng: Double,
    val updated_at: String
)

data class ViajeResponse(
    val id_viaje: Int,
    val id_ruta: Int,
    val fecha: String,
    val estado: String,
    val total: Int,
    val subieron: Int,
    val no_subieron: Int
)

data class AsistenciaRequest(
    val id_estudiante: Int,
    val subio: Boolean,
    val motivo: String? = null
)

data class FinalizarRequest(
    val fecha_celular: String
)

data class AsistenciaResponse(
    val id_asistencia: Int,
    val id_viaje: Int,
    val id_estudiante: Int,
    val subio: Boolean,
    val hora_registro: String,
    val motivo: String?,
    val observacion: String?
)

data class CercaRequest(
    val id_viaje: Int,
    val id_estudiante: Int
)

interface ApiService {
    @POST("registro")
    fun registerUser(@Body request: RegistroRequest): Call<ApiResponse>

    @POST("unirse-conductor")
    fun joinAsDriver(@Body request: DriverRequest): Call<ApiResponse>

    @POST("login")
    fun login(@Body request: LoginRequest): Call<ApiResponse>

    @GET("choferes")
    fun getChoferes(): Call<List<ChoferResponse>>

    @POST("estudiantes")
    fun addEstudiante(@Body request: EstudianteRequest): Call<ApiResponse>

    @GET("chofer/{correo}/ruta")
    fun getRuta(@Path("correo") driverEmail: String): Call<List<EstudianteResponse>>

    // Solicitudes de estudiantes que el chofer debe aceptar o rechazar
    @GET("chofer/{correo}/estudiantes/solicitudes")
    fun getSolicitudesEstudiantes(@Path("correo") driverEmail: String): Call<List<EstudianteResponse>>

    @POST("estudiantes/{id}/aceptar")
    fun aceptarEstudiante(@Path("id") idEstudiante: Int): Call<ApiResponse>

    @POST("estudiantes/{id}/rechazar")
    fun rechazarEstudiante(@Path("id") idEstudiante: Int): Call<ApiResponse>

    // El padre reenvía a otro chofer la solicitud que le rechazaron
    @PUT("estudiantes/{id}/reasignar")
    fun reasignarEstudiante(
        @Path("id") idEstudiante: Int,
        @Body request: ReasignarRequest
    ): Call<ApiResponse>

    // Ruta descriptiva del chofer
    @GET("colegios")
    fun getColegios(): Call<List<ColegioResponse>>

    @GET("chofer/{correo}/ruta-info")
    fun getRutaInfo(@Path("correo") driverEmail: String): Call<RutaInfoResponse>

    @PUT("chofer/{correo}/ruta-info")
    fun updateRutaInfo(
        @Path("correo") driverEmail: String,
        @Body request: RutaInfoRequest
    ): Call<ApiResponse>

    @GET("padre/{correo}/hijos")
    fun getParentChildren(@Path("correo") parentEmail: String): Call<List<EstudianteResponse>>

    @GET("notificaciones/{parent_email}")
    fun getNotifications(@Path("parent_email") parentEmail: String): Call<List<NotificationResponse>>

    @POST("incidentes")
    fun reportIncident(@Body request: IncidentRequest): Call<IncidentResponse>

    @GET("incidentes/{parent_email}")
    fun getIncidents(@Path("parent_email") parentEmail: String): Call<List<IncidentResponse>>

    @POST("chofer/ubicacion")
    fun updateDriverLocation(@Body request: DriverLocationRequest): Call<DriverLocationResponse>

    @GET("chofer/ubicacion/{driver_email}")
    fun getDriverLocation(@Path("driver_email") driverEmail: String): Call<DriverLocationResponse>

    @POST("chofer/{correo}/viajes/iniciar")
    fun iniciarViaje(@Path("correo") driverEmail: String): Call<ViajeResponse>

    @POST("chofer/{correo}/viajes/finalizar")
    fun finalizarViaje(@Path("correo") driverEmail: String, @Body request: FinalizarRequest): Call<ViajeResponse>

    @GET("chofer/{correo}/viajes/actual")
    fun getViajeActual(@Path("correo") driverEmail: String): Call<ViajeResponse>

    @POST("viajes/{id_viaje}/asistencia")
    fun marcarAsistencia(@Path("id_viaje") idViaje: Int, @Body request: AsistenciaRequest): Call<AsistenciaResponse>

    @DELETE("estudiantes/{id}/ruta")
    fun quitarEstudianteDeRuta(@Path("id") idEstudiante: Int): Call<ApiResponse>

    @GET("chofer/{correo}/estado-vehiculo")
    fun getEstadoVehiculo(@Path("correo") correo: String): Call<EstadoVehiculoResponse>

    @POST("notificaciones/cerca")
    fun enviarNotifCerca(@Body request: CercaRequest): Call<ApiResponse>

    @POST("viajes/{id_viaje}/notificar-llegada")
    fun notificarLlegada(@Path("id_viaje") idViaje: Int): Call<ApiResponse>

    // ADMIN ENDPOINTS
    @GET("solicitudes")
    fun getSolicitudesPendientes(): Call<List<SolicitudResponse>>

    @POST("solicitudes/{id}/aprobar")
    fun aprobarSolicitud(@Path("id") id: Int): Call<ApiResponse>

    @POST("solicitudes/{id}/rechazar")
    fun rechazarSolicitud(@Path("id") id: Int): Call<ApiResponse>
}
