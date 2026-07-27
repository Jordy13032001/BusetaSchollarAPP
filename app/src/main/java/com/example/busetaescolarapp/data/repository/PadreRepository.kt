package com.example.busetaescolarapp.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.example.busetaescolarapp.data.local.AppDatabase
import com.example.busetaescolarapp.data.local.IncidenteEntity
import com.example.busetaescolarapp.data.local.NotificacionEntity
import com.example.busetaescolarapp.data.local.PagoEntity
import com.example.busetaescolarapp.network.ApiClient
import com.example.busetaescolarapp.network.EstudianteResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class PadreRepository(context: Context) {

    private val notificacionDao = AppDatabase.getInstance(context).notificacionDao()
    private val incidenteDao = AppDatabase.getInstance(context).incidenteDao()
    private val pagoDao = AppDatabase.getInstance(context).pagoDao()

    fun getParentChildren(email: String, onResult: (List<EstudianteResponse>?) -> Unit) {
        ApiClient.apiService.getParentChildren(email).enqueue(object : Callback<List<EstudianteResponse>> {
            override fun onResponse(call: Call<List<EstudianteResponse>>, response: Response<List<EstudianteResponse>>) {
                if (response.isSuccessful) {
                    onResult(response.body())
                } else {
                    onResult(null)
                }
            }
            override fun onFailure(call: Call<List<EstudianteResponse>>, t: Throwable) {
                onResult(null)
            }
        })
    }

    // --- Notificaciones: Room (SQLite) como fuente única de verdad para la UI ---

    fun getNotificacionesLocal(email: String): LiveData<List<NotificacionEntity>> =
        notificacionDao.getByPadre(email)

    suspend fun sincronizarNotificaciones(email: String) = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.getNotifications(email).execute()
            if (response.isSuccessful) {
                val entities = (response.body() ?: emptyList()).map {
                    NotificacionEntity(
                        id = it.id,
                        correoPadre = email,
                        titulo = it.title,
                        mensaje = it.message,
                        hora = formatearHoraLocal(it.timestamp),
                        tipo = it.type,
                        idEstudiante = it.id_estudiante,
                        nombreEstudiante = it.nombre_estudiante,
                        estadoEstudiante = it.estado_estudiante
                    )
                }
                notificacionDao.deleteByPadre(email)
                notificacionDao.insertAll(entities)
            }
        } catch (_: Exception) {
            // Sin conexión: se sigue mostrando la última copia guardada en Room
        }
    }

    // --- Incidentes: mismo patrón ---

    fun getIncidentesLocal(email: String): LiveData<List<IncidenteEntity>> =
        incidenteDao.getByPadre(email)

    suspend fun sincronizarIncidentes(email: String) = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.getIncidents(email).execute()
            if (response.isSuccessful) {
                val entities = (response.body() ?: emptyList()).map {
                    IncidenteEntity(
                        idIncidente = it.id_incidente,
                        correoPadre = email,
                        mensaje = it.mensaje,
                        estado = it.estado,
                        fechaHora = it.fecha_hora
                    )
                }
                incidenteDao.deleteByPadre(email)
                incidenteDao.insertAll(entities)
            }
        } catch (_: Exception) {
            // Sin conexión: se sigue mostrando la última copia guardada en Room
        }
    }

    // --- Pagos: solo locales (Room). El backend no tiene modelo de pagos, la
    // pasarela es simulada y el comprobante le sirve únicamente al padre. ---

    fun getPagosLocal(email: String): LiveData<List<PagoEntity>> = pagoDao.getByPadre(email)

    suspend fun registrarPago(pago: PagoEntity): Long = withContext(Dispatchers.IO) {
        pagoDao.insertar(pago)
    }

    suspend fun estaPagado(idEstudiante: Int): Boolean = withContext(Dispatchers.IO) {
        pagoDao.getUltimoPagoDeEstudiante(idEstudiante) != null
    }

    // Convierte el timestamp UTC del backend (PostgreSQL NOW()) a la hora local del teléfono
    private fun formatearHoraLocal(raw: String): String {
        val formatos = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd HH:mm:ss.SSSSSS",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm"
        )
        val salidaFmt = SimpleDateFormat("HH:mm · dd/MM", Locale.getDefault())
        salidaFmt.timeZone = TimeZone.getDefault()
        for (fmt in formatos) {
            try {
                val parser = SimpleDateFormat(fmt, Locale.US)
                parser.timeZone = TimeZone.getTimeZone("UTC")
                val date = parser.parse(raw) ?: continue
                return salidaFmt.format(date)
            } catch (_: Exception) {}
        }
        return raw
    }
}
