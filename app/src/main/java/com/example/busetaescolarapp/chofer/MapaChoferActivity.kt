package com.example.busetaescolarapp.chofer

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.busetaescolarapp.NavigationUtils
import com.example.busetaescolarapp.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import android.widget.TextView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions

import android.location.Geocoder
import com.example.busetaescolarapp.network.ApiClient
import com.example.busetaescolarapp.network.EstudianteResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class MapaChoferActivity : AppCompatActivity(), OnMapReadyCallback {

    private var mMap: GoogleMap? = null
    private var driverEmail: String = ""
    private var busMarker: com.google.android.gms.maps.model.Marker? = null
    private var isMapReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mapa_chofer)
        
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapChofer) as SupportMapFragment
        mapFragment.getMapAsync(this)

        NavigationUtils.setupChoferBottomNavigation(this)
        
        val sessionManager = com.example.busetaescolarapp.utils.SessionManager(this)
        driverEmail = sessionManager.getUserEmail() ?: ""
        
        if (driverEmail.isNotEmpty()) {
            fetchRoute()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        isMapReady = true
        
        val limiteCuenca = LatLngBounds(
            LatLng(-2.9300, -79.0500),
            LatLng(-2.8700, -78.9500)
        )
        mMap?.setLatLngBoundsForCameraTarget(limiteCuenca)
        mMap?.setMinZoomPreference(13.0f)
        
        val cuencaCentro = LatLng(-2.9000, -79.0000)
        mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(cuencaCentro, 13f))
        
        setupTrackerListener()
    }

    private fun setupTrackerListener() {
        // Dibujar posición actual si ya existe
        DriverTracker.currentLatLng?.let { pos ->
            updateBusMarker(pos)
        }
        
        // Escuchar actualizaciones
        DriverTracker.onLocationUpdate = { pos ->
            runOnUiThread {
                updateBusMarker(pos)
            }
        }
    }

    private fun updateBusMarker(pos: LatLng) {
        if (mMap == null) return
        if (busMarker == null) {
            busMarker = mMap?.addMarker(
                MarkerOptions()
                    .position(pos)
                    .title("Mi Ubicación (Buseta)")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            )
            mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f))
        } else {
            busMarker?.position = pos
            mMap?.animateCamera(CameraUpdateFactory.newLatLng(pos))
        }
    }

    private fun fetchRoute() {
        ApiClient.apiService.getRuta(driverEmail).enqueue(object : Callback<List<EstudianteResponse>> {
            override fun onResponse(call: Call<List<EstudianteResponse>>, response: Response<List<EstudianteResponse>>) {
                if (response.isSuccessful) {
                    val children = response.body() ?: emptyList()
                    setupNextStopListener(children)
                    drawRealRoute(children)
                }
            }
            override fun onFailure(call: Call<List<EstudianteResponse>>, t: Throwable) {}
        })
    }

    private fun setupNextStopListener(children: List<EstudianteResponse>) {
        val tvProximaParada = findViewById<TextView>(R.id.tvProximaParada)
        val tvLlegadaEstimada = findViewById<TextView>(R.id.tvLlegadaEstimada)
        
        val updateUI = { index: Int ->
            runOnUiThread {
                if (index < children.size) {
                    val child = children[index]
                    tvProximaParada.text = child.direccion
                    tvLlegadaEstimada.text = child.hora_estimada ?: "--:--"
                } else {
                    tvProximaParada.text = "Ruta finalizada"
                    tvLlegadaEstimada.text = "--:--"
                }
            }
        }
        
        DriverTracker.onNextStopUpdate = updateUI
        
        // Disparar inmediatamente con el estado actual si la ruta ya está activa
        if (DriverTracker.isTracking()) {
            updateUI(DriverTracker.currentPositionIndex)
        }
    }

    private fun drawRealRoute(children: List<EstudianteResponse>) {
        Thread {
            val geocoder = Geocoder(this, Locale.getDefault())
            val rutaCoordenadas = mutableListOf<LatLng>()

            for (child in children) {
                val pos = if (child.lat != null && child.lng != null) {
                    LatLng(child.lat, child.lng)
                } else {
                    try {
                        val addresses = geocoder.getFromLocationName("${child.direccion}, Cuenca, Ecuador", 1)
                        if (!addresses.isNullOrEmpty()) LatLng(addresses[0].latitude, addresses[0].longitude) else null
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                if (pos != null) {
                    rutaCoordenadas.add(pos)
                    runOnUiThread {
                        mMap?.addMarker(
                            MarkerOptions()
                                .position(pos)
                                .title(child.nombre_completo)
                                .snippet(child.direccion)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                        )
                    }
                }
            }

            if (rutaCoordenadas.isNotEmpty()) {
                runOnUiThread {
                    val polylineOptions = PolylineOptions()
                        .addAll(rutaCoordenadas)
                        .width(12f)
                        .color(Color.parseColor("#FBC02D"))
                        .geodesic(true)
                    mMap?.addPolyline(polylineOptions)
                }
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        DriverTracker.onLocationUpdate = null // Remove listener to avoid leaks
    }
}
