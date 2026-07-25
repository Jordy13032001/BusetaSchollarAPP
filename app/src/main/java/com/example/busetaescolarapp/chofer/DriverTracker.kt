package com.example.busetaescolarapp.chofer

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.busetaescolarapp.data.local.TramoEntity
import com.example.busetaescolarapp.network.ApiClient
import com.example.busetaescolarapp.network.AsistenciaRequest
import com.example.busetaescolarapp.network.AsistenciaResponse
import com.example.busetaescolarapp.network.DriverLocationRequest
import com.example.busetaescolarapp.network.DriverLocationResponse
import com.example.busetaescolarapp.utils.PolylineUtils
import com.google.android.gms.maps.model.LatLng
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object DriverTracker {
    private var isTracking = false
    private var routePoints = mutableListOf<LatLng>()
    private var tramos: List<TramoEntity> = emptyList()
    private var _currentPositionIndex = 0
    val currentPositionIndex: Int
        get() = _currentPositionIndex

    private var driverEmail: String = ""
    private var handler = Handler(Looper.getMainLooper())
    private var updateInterval: Long = 2000 // Fallback si no hay ruta real (Directions API) disponible
    private const val DURACION_SIMULACION_MS = 6000L // Duración fija de la animación por tramo, para la demo

    // Almacenamos la coordenada actual para que el MapaChofer la dibuje si lo desea
    var currentLatLng: LatLng? = null
    var onLocationUpdate: ((LatLng) -> Unit)? = null

    // Viaje real (tabla viajes) al que se atan los pings de ubicación.
    var currentViajeId: Int? = null

    var onStopArrived: ((Int) -> Unit)? = null
    var onNextStopUpdate: ((Int) -> Unit)? = null
    var onRutaFinalizada: (() -> Unit)? = null

    private val _paradasVisitadas = mutableSetOf<Int>()
    val paradasVisitadas: Set<Int> get() = _paradasVisitadas
    var onAsistenciaRegistrada: ((index: Int, subio: Boolean) -> Unit)? = null

    /**
     * Parada cuya asistencia todavía no se confirma. Vive aquí (y no en la Activity) para que
     * el recuadro se pueda volver a mostrar si la pantalla se recrea, p. ej. al rotar el celular.
     */
    var paradaEsperandoConfirmacion: Int? = null
        private set

    fun startTracking(email: String, points: List<LatLng>, idViaje: Int? = null, tramos: List<TramoEntity> = emptyList()) {
        if (isTracking || points.isEmpty()) return

        currentViajeId = idViaje
        driverEmail = email
        routePoints.clear()
        routePoints.addAll(points)
        this.tramos = tramos
        _currentPositionIndex = 0
        isTracking = true
        onNextStopUpdate?.invoke(_currentPositionIndex)

        // Simular que el viaje arranca y llegamos a la primera parada
        handler.postDelayed({
            arriveAtStop(0)
        }, 1500)
    }

    fun resumeTracking() {
        if (!isTracking) return

        val tramoIndex = _currentPositionIndex
        _currentPositionIndex++
        if (_currentPositionIndex < routePoints.size) {
            onNextStopUpdate?.invoke(_currentPositionIndex)

            val puntosTramo = tramos.getOrNull(tramoIndex)?.puntosTramo?.let { PolylineUtils.deserialize(it) }
            if (!puntosTramo.isNullOrEmpty()) {
                // Ruta real obtenida de la Directions API: simulamos el recorrido siguiendo las calles
                animarTramo(puntosTramo) { arriveAtStop(_currentPositionIndex) }
            } else {
                // Sin datos de ruta real (sin conexión, etc.): salto directo a la siguiente parada
                handler.postDelayed({
                    arriveAtStop(_currentPositionIndex)
                }, updateInterval)
            }
        } else {
            // Se recorrieron todas las paradas: ruta completada de forma natural
            onRutaFinalizada?.invoke()
            stopTracking()
        }
    }

    private fun animarTramo(puntos: List<LatLng>, alTerminar: () -> Unit) {
        val stepMs = (DURACION_SIMULACION_MS / puntos.size).coerceAtLeast(150L)
        var i = 0
        fun step() {
            if (!isTracking) return
            if (i >= puntos.size) {
                alTerminar()
                return
            }
            currentLatLng = puntos[i]
            onLocationUpdate?.invoke(puntos[i])
            i++
            handler.postDelayed(::step, stepMs)
        }
        step()
    }

    /** Suma la duración real (Directions API) de los tramos que faltan por recorrer. */
    fun tiempoEstimadoRestanteSegundos(): Int {
        if (tramos.isEmpty() || _currentPositionIndex >= tramos.size) return 0
        return tramos.drop(_currentPositionIndex).sumOf { it.duracionSegundos }
    }

    fun stopTracking() {
        isTracking = false
        tramos = emptyList()
        _paradasVisitadas.clear()
        paradaEsperandoConfirmacion = null
        handler.removeCallbacksAndMessages(null)
    }

    /** Registra la asistencia en el backend y marca la parada como ya recorrida. */
    fun registrarAsistencia(index: Int, idEstudiante: Int, subio: Boolean, motivo: String?) {
        if (!_paradasVisitadas.add(index)) return // ya se registró (p. ej. doble toque)
        paradaEsperandoConfirmacion = null
        onAsistenciaRegistrada?.invoke(index, subio)

        val idViaje = currentViajeId ?: return
        val request = AsistenciaRequest(idEstudiante, subio, motivo)
        ApiClient.apiService.marcarAsistencia(idViaje, request).enqueue(object : Callback<AsistenciaResponse> {
            override fun onResponse(call: Call<AsistenciaResponse>, response: Response<AsistenciaResponse>) {
                Log.d("DriverTracker", "Asistencia registrada: estudiante=$idEstudiante subio=$subio")
            }
            override fun onFailure(call: Call<AsistenciaResponse>, t: Throwable) {
                Log.e("DriverTracker", "Error registrando asistencia: ${t.message}")
            }
        })
    }

    fun isTracking() = isTracking

    private fun arriveAtStop(index: Int) {
        if (!isTracking || index >= routePoints.size) return
        
        val point = routePoints[index]
        currentLatLng = point
        
        sendLocationToBackend(point)
        onLocationUpdate?.invoke(point)

        // Disparar evento de que llegamos a la parada para usar la voz
        paradaEsperandoConfirmacion = index
        onStopArrived?.invoke(index)
        
        // IMPORTANTE: Ya no llamamos a simulateMovement() o handler automáticamente.
        // Esperamos a que la actividad llame a resumeTracking() después de la voz.
    }

    private fun sendLocationToBackend(location: LatLng) {
        val idViaje = currentViajeId ?: return
        val request = DriverLocationRequest(driverEmail, location.latitude, location.longitude, idViaje)
        ApiClient.apiService.updateDriverLocation(request).enqueue(object : Callback<DriverLocationResponse> {
            override fun onResponse(call: Call<DriverLocationResponse>, response: Response<DriverLocationResponse>) {
                Log.d("DriverTracker", "Ubicación actualizada: ${location.latitude}, ${location.longitude}")
            }
            override fun onFailure(call: Call<DriverLocationResponse>, t: Throwable) {
                Log.e("DriverTracker", "Error actualizando ubicación: ${t.message}")
            }
        })
    }
}
