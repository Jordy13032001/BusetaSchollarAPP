package com.example.busetaescolarapp.data.repository

import android.content.Context
import com.example.busetaescolarapp.data.local.AppDatabase
import com.example.busetaescolarapp.data.local.TramoEntity
import com.example.busetaescolarapp.network.DirectionsApiClient
import com.example.busetaescolarapp.utils.MapsKeyProvider
import com.example.busetaescolarapp.utils.PolylineUtils
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RutaRepository(private val context: Context) {

    private val dao = AppDatabase.getInstance(context).tramoDao()

    /**
     * Borra la ruta cacheada del día. Se llama cuando cambia la lista de estudiantes
     * aceptados: los tramos guardados ya no corresponden a la ruta real.
     */
    suspend fun invalidarCache(choferEmail: String) = withContext(Dispatchers.IO) {
        val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        dao.deleteDelDia(choferEmail, hoy)
    }

    /**
     * Devuelve los tramos (ruta real + tiempo estimado) entre cada parada consecutiva.
     * Si ya se guardaron hoy para este chofer, los reutiliza y NO vuelve a llamar
     * la Directions API (así el token limitado solo se gasta una vez por día/ruta).
     */
    suspend fun obtenerTramos(choferEmail: String, paradas: List<LatLng>): List<TramoEntity> =
        withContext(Dispatchers.IO) {
            if (paradas.size < 2) return@withContext emptyList()

            val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            val cache = dao.getTramosDelDia(choferEmail, hoy)
            if (cache.isNotEmpty()) return@withContext cache

            try {
                val apiKey = MapsKeyProvider.getApiKey(context)
                val origin = "${paradas.first().latitude},${paradas.first().longitude}"
                val destination = "${paradas.last().latitude},${paradas.last().longitude}"
                val intermedias = paradas.subList(1, paradas.size - 1)
                val waypoints = if (intermedias.isNotEmpty()) {
                    intermedias.joinToString("|") { "${it.latitude},${it.longitude}" }
                } else null

                val response = DirectionsApiClient.service
                    .getDirections(origin, destination, waypoints, apiKey = apiKey)
                    .execute()

                val body = response.body()
                if (!response.isSuccessful || body == null || body.status != "OK" || body.routes.isEmpty()) {
                    return@withContext emptyList()
                }

                val tramos = body.routes.first().legs.mapIndexed { index, leg ->
                    val puntos = leg.steps.flatMap { PolylineUtils.decode(it.polyline.points) }
                    TramoEntity(
                        choferEmail = choferEmail,
                        fecha = hoy,
                        orden = index,
                        origenDireccion = leg.start_address,
                        destinoDireccion = leg.end_address,
                        distanciaMetros = leg.distance.value,
                        duracionSegundos = leg.duration.value,
                        puntosTramo = PolylineUtils.serialize(puntos)
                    )
                }

                dao.deleteDelDia(choferEmail, hoy)
                dao.insertAll(tramos)
                tramos
            } catch (_: Exception) {
                // Sin conexión o error de la API: se simula solo con las paradas (sin ruta real)
                emptyList()
            }
        }
}
