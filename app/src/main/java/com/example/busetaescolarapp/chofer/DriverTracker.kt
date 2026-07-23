package com.example.busetaescolarapp.chofer

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.busetaescolarapp.network.ApiClient
import com.example.busetaescolarapp.network.DriverLocationRequest
import com.example.busetaescolarapp.network.DriverLocationResponse
import com.google.android.gms.maps.model.LatLng
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object DriverTracker {
    private var isTracking = false
    private var routePoints = mutableListOf<LatLng>()
    private var _currentPositionIndex = 0
    val currentPositionIndex: Int
        get() = _currentPositionIndex
        
    private var driverEmail: String = ""
    private var handler = Handler(Looper.getMainLooper())
    private var updateInterval: Long = 2000 // Update every 2 seconds for smooth simulation
    
    // Almacenamos la coordenada actual para que el MapaChofer la dibuje si lo desea
    var currentLatLng: LatLng? = null
    var onLocationUpdate: ((LatLng) -> Unit)? = null

    // Viaje real (tabla viajes) al que se atan los pings de ubicación.
    var currentViajeId: Int? = null

    var onStopArrived: ((Int) -> Unit)? = null
    var onNextStopUpdate: ((Int) -> Unit)? = null

    fun startTracking(email: String, points: List<LatLng>, idViaje: Int? = null) {
        if (isTracking || points.isEmpty()) return

        currentViajeId = idViaje
        driverEmail = email
        routePoints.clear()
        routePoints.addAll(points)
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
        
        _currentPositionIndex++
        if (_currentPositionIndex < routePoints.size) {
            onNextStopUpdate?.invoke(_currentPositionIndex)
            // Ir a la siguiente parada
            handler.postDelayed({
                arriveAtStop(_currentPositionIndex)
            }, updateInterval)
        } else {
            // Terminó la ruta
            stopTracking()
        }
    }

    fun stopTracking() {
        isTracking = false
        handler.removeCallbacksAndMessages(null)
    }

    fun isTracking() = isTracking

    private fun arriveAtStop(index: Int) {
        if (!isTracking || index >= routePoints.size) return
        
        val point = routePoints[index]
        currentLatLng = point
        
        sendLocationToBackend(point)
        onLocationUpdate?.invoke(point)
        
        // Disparar evento de que llegamos a la parada para usar la voz
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
