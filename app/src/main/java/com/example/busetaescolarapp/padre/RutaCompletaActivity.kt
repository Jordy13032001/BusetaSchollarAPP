package com.example.busetaescolarapp.padre

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.busetaescolarapp.NavigationUtils
import com.example.busetaescolarapp.R
import com.example.busetaescolarapp.network.ApiClient
import com.example.busetaescolarapp.network.DriverLocationResponse
import com.example.busetaescolarapp.network.EstudianteResponse
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Timer
import java.util.TimerTask
import java.util.Locale

class RutaCompletaActivity : AppCompatActivity(), OnMapReadyCallback {

    private var mMap: GoogleMap? = null
    private var parentEmail: String = ""
    private var currentDriverEmail: String = ""
    private var isMapReady = false
    private var driverMarker: com.google.android.gms.maps.model.Marker? = null
    private var locationTimer: Timer? = null

    private var allChildrenList = emptyList<EstudianteResponse>()
    private var driversList = emptyList<String>()

    private lateinit var spinnerChoferes: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ruta_completa)

        spinnerChoferes = findViewById(R.id.spinnerChoferes)
        
        val sessionManager = com.example.busetaescolarapp.utils.SessionManager(this)
        parentEmail = sessionManager.getUserEmail() ?: ""

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapRuta) as SupportMapFragment
        mapFragment.getMapAsync(this)
        
        NavigationUtils.setupPadreBottomNavigation(this)
        
        if (parentEmail.isNotEmpty()) {
            fetchParentData()
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
        mMap?.setMinZoomPreference(12.0f)
        
        val cuencaCentro = LatLng(-2.9000, -79.0000)
        mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(cuencaCentro, 13f))
        
        drawMapForCurrentDriver()
    }

    private fun fetchParentData() {
        ApiClient.apiService.getParentChildren(parentEmail).enqueue(object : Callback<List<EstudianteResponse>> {
            override fun onResponse(call: Call<List<EstudianteResponse>>, response: Response<List<EstudianteResponse>>) {
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    allChildrenList = response.body()!!
                    setupSpinner()
                } else {
                    Toast.makeText(this@RutaCompletaActivity, "No tienes hijos registrados", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<List<EstudianteResponse>>, t: Throwable) {
            }
        })
    }

    private fun setupSpinner() {
        driversList = allChildrenList.map { it.correo_chofer }.distinct()
        
        val driverDisplayNames = driversList.map { email ->
            val name = email.split("@")[0].replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            "Bus de $name"
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, driverDisplayNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerChoferes.adapter = adapter

        spinnerChoferes.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                currentDriverEmail = driversList[position]
                switchDriver()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        if (driversList.isNotEmpty()) {
            currentDriverEmail = driversList[0]
            switchDriver()
        }
    }

    private fun switchDriver() {
        locationTimer?.cancel()
        locationTimer = null
        driverMarker = null
        drawMapForCurrentDriver()
        startLocationUpdates()
    }

    private fun drawMapForCurrentDriver() {
        if (!isMapReady || allChildrenList.isEmpty() || currentDriverEmail.isEmpty()) return

        mMap?.clear()

        // Filtrar solo MIS hijos que van en ESTE bus
        val myKidsOnThisBus = allChildrenList.filter { 
            it.correo_chofer == currentDriverEmail && it.correo_padre == parentEmail 
        }

        // Agrupar por coordenadas
        val kidsByLocation = mutableMapOf<Pair<Double, Double>, MutableList<String>>()

        for (child in myKidsOnThisBus) {
            val lat = child.lat ?: 0.0
            val lng = child.lng ?: 0.0
            if (lat != 0.0 && lng != 0.0) {
                val coord = Pair(lat, lng)
                if (!kidsByLocation.containsKey(coord)) {
                    kidsByLocation[coord] = mutableListOf()
                }
                kidsByLocation[coord]?.add(child.nombre_completo.split(" ")[0]) // Solo el primer nombre
            }
        }

        var firstStop: LatLng? = null

        // Dibujar un marcador por cada grupo
        for ((coord, names) in kidsByLocation) {
            val pos = LatLng(coord.first, coord.second)
            if (firstStop == null) firstStop = pos
            
            val label = "Tu Casa (${names.joinToString(", ")})"
            
            mMap?.addMarker(
                MarkerOptions()
                    .position(pos)
                    .title(label)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            )
        }

        // Marcador del Colegio (Hardcoded por ahora para Cuenca)
        val colegioPos = LatLng(-2.9065, -79.0040)
        mMap?.addMarker(
            MarkerOptions()
                .position(colegioPos)
                .title("Colegio")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        )

        // Centrar mapa
        if (firstStop != null) {
            mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(firstStop, 14f))
        } else {
            mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(colegioPos, 13f))
        }
    }

    private fun startLocationUpdates() {
        if (currentDriverEmail.isEmpty()) return

        locationTimer = Timer()
        locationTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                ApiClient.apiService.getDriverLocation(currentDriverEmail).enqueue(object : Callback<DriverLocationResponse> {
                    override fun onResponse(call: Call<DriverLocationResponse>, response: Response<DriverLocationResponse>) {
                        if (response.isSuccessful && response.body() != null) {
                            val loc = response.body()!!
                            runOnUiThread {
                                updateDriverMarker(LatLng(loc.lat, loc.lng))
                            }
                        }
                    }
                    override fun onFailure(call: Call<DriverLocationResponse>, t: Throwable) {}
                })
            }
        }, 0, 5000)
    }

    private fun updateDriverMarker(pos: LatLng) {
        if (mMap == null) return
        if (driverMarker == null) {
            driverMarker = mMap?.addMarker(
                MarkerOptions()
                    .position(pos)
                    .title("Buseta")
                    .icon(com.example.busetaescolarapp.utils.MapIconUtils.vectorToBitmapDescriptor(
                        this, com.example.busetaescolarapp.R.drawable.ic_bus_marker
                    ))
                    .anchor(0.5f, 0.5f)
                    .flat(true)
            )
        } else {
            driverMarker?.position = pos
        }
        calcularETA(pos)
    }

    private fun calcularETA(busPos: LatLng) {
        val tvETA = findViewById<TextView>(R.id.tvHoraLlegada) ?: return

        val misParadas = allChildrenList.filter {
            it.correo_padre == parentEmail && it.correo_chofer == currentDriverEmail
        }

        if (misParadas.isEmpty()) return

        val distanciaMin = misParadas.mapNotNull { child ->
            val lat = child.lat ?: return@mapNotNull null
            val lng = child.lng ?: return@mapNotNull null
            val result = FloatArray(1)
            android.location.Location.distanceBetween(busPos.latitude, busPos.longitude, lat, lng, result)
            result[0].toDouble()
        }.minOrNull() ?: return

        // Bus escolar ~20 km/h = 5.56 m/s
        val etaSegundos = (distanciaMin / 5.56).toLong()
        val etaMs = System.currentTimeMillis() + etaSegundos * 1000L
        tvETA.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(etaMs))
    }

    override fun onDestroy() {
        super.onDestroy()
        locationTimer?.cancel()
    }
}
