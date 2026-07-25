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
import android.widget.TextView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions

import android.location.Geocoder
import android.location.Location
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.example.busetaescolarapp.data.repository.ChoferRepository
import com.example.busetaescolarapp.data.repository.RutaRepository
import com.example.busetaescolarapp.network.ApiClient
import com.example.busetaescolarapp.network.EstudianteResponse
import com.example.busetaescolarapp.utils.MapIconUtils
import com.example.busetaescolarapp.utils.PolylineUtils
import com.example.busetaescolarapp.utils.TextToSpeechManager
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class MapaChoferActivity : AppCompatActivity(), OnMapReadyCallback {

    private var mMap: GoogleMap? = null
    private var driverEmail: String = ""
    private var busMarker: com.google.android.gms.maps.model.Marker? = null
    private var isMapReady = false
    private var ultimaPosicion: LatLng? = null
    private lateinit var rutaRepository: RutaRepository
    private val choferRepository = ChoferRepository()

    private var childrenList: List<EstudianteResponse> = emptyList()
    private val paradaMarkers = mutableMapOf<Int, com.google.android.gms.maps.model.Marker>()
    private var confirmacionDialog: androidx.appcompat.app.AlertDialog? = null
    private var confirmacionDialogView: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mapa_chofer)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapChofer) as SupportMapFragment
        mapFragment.getMapAsync(this)

        NavigationUtils.setupChoferBottomNavigation(this)

        rutaRepository = RutaRepository(applicationContext)

        // Esta pantalla anuncia por voz cada parada y la confirmación de asistencia.
        TextToSpeechManager.init(this)

        val sessionManager = com.example.busetaescolarapp.utils.SessionManager(this)
        driverEmail = sessionManager.getUserEmail() ?: ""

        // La confirmación de asistencia se maneja desde esta pantalla (es la que el chofer ve
        // mientras la buseta avanza), así el recuadro sí es visible al llegar a cada parada.
        DriverTracker.onStopArrived = { index -> handleStopArrived(index) }
        DriverTracker.onRutaFinalizada = { runOnUiThread { mostrarPantallaRutaCompletada() } }

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
                    .icon(MapIconUtils.vectorToBitmapDescriptor(this, R.drawable.ic_bus_marker))
                    .anchor(0.5f, 0.5f)
                    .flat(true)
            )
            mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 16f))
        } else {
            // Rotamos el ícono hacia la dirección real de avance, para que parezca que "maneja" por la calle
            ultimaPosicion?.let { anterior ->
                if (anterior.latitude != pos.latitude || anterior.longitude != pos.longitude) {
                    busMarker?.rotation = bearingEntre(anterior, pos)
                }
            }
            busMarker?.position = pos
            mMap?.animateCamera(CameraUpdateFactory.newLatLng(pos))
        }
        ultimaPosicion = pos
    }

    private fun bearingEntre(origen: LatLng, destino: LatLng): Float {
        val locOrigen = Location("origen").apply {
            latitude = origen.latitude
            longitude = origen.longitude
        }
        val locDestino = Location("destino").apply {
            latitude = destino.latitude
            longitude = destino.longitude
        }
        return locOrigen.bearingTo(locDestino)
    }

    private fun fetchRoute() {
        ApiClient.apiService.getRuta(driverEmail).enqueue(object : Callback<List<EstudianteResponse>> {
            override fun onResponse(call: Call<List<EstudianteResponse>>, response: Response<List<EstudianteResponse>>) {
                if (response.isSuccessful) {
                    val children = response.body() ?: emptyList()
                    childrenList = children
                    setupNextStopListener(children)
                    drawRealRoute(children)
                    // Si la pantalla se recreó (p. ej. al rotar) con una parada sin confirmar,
                    // volvemos a mostrar el recuadro en lugar de dejar la ruta trabada.
                    DriverTracker.paradaEsperandoConfirmacion?.let { pendiente ->
                        runOnUiThread { handleStopArrived(pendiente) }
                    }
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

                    // Tiempo estimado real (Directions API, cacheado en Room) sumando los tramos que faltan
                    val etaSegundos = DriverTracker.tiempoEstimadoRestanteSegundos()
                    tvLlegadaEstimada.text = if (etaSegundos > 0) {
                        "${(etaSegundos / 60).coerceAtLeast(1)} min"
                    } else {
                        child.hora_estimada ?: "--:--"
                    }
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

            for ((index, child) in children.withIndex()) {
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
                        val yaVisitada = DriverTracker.paradasVisitadas.contains(index)
                        val icono = if (yaVisitada) MapIconUtils.checkmarkMarker() else MapIconUtils.numberedMarker(index + 1)
                        val marker = mMap?.addMarker(
                            MarkerOptions()
                                .position(pos)
                                .title("${index + 1}. ${child.nombre_completo}")
                                .snippet(child.direccion)
                                .icon(icono)
                                .anchor(0.5f, 0.5f)
                        )
                        marker?.let { paradaMarkers[index] = it }
                    }
                }
            }

            if (rutaCoordenadas.isNotEmpty()) {
                // Ya no dibujamos la línea recta de conexión directa: solo se muestra
                // la ruta real (Directions API) cacheada en Room, siguiendo las calles.
                dibujarRutaRealSiExiste(rutaCoordenadas.toList())
            }
        }.start()
    }

    private fun dibujarRutaRealSiExiste(paradas: List<LatLng>) {
        if (paradas.size < 2) return
        lifecycleScope.launch {
            val tramos = rutaRepository.obtenerTramos(driverEmail, paradas)
            if (tramos.isNotEmpty()) {
                val puntosRuta = tramos.flatMap { PolylineUtils.deserialize(it.puntosTramo) }
                if (puntosRuta.isNotEmpty()) {
                    mMap?.addPolyline(
                        PolylineOptions()
                            .addAll(puntosRuta)
                            .width(14f)
                            .color(Color.parseColor("#4FC3F7")) // celeste claro
                            .geodesic(true)
                    )
                }
            }
        }
    }

    // --- Confirmación de asistencia al llegar a cada parada ---
    // El chofer marca manualmente Sí/No. El recuadro no se cierra hasta que marque.

    private fun handleStopArrived(index: Int) {
        val child = childrenList.getOrNull(index) ?: return

        mostrarDialogoConfirmacion(child, index)
        TextToSpeechManager.speak("Parada de ${child.nombre_completo}.")
    }

    private fun registrarYAvanzar(index: Int, child: EstudianteResponse, subio: Boolean, motivo: String?) {
        if (DriverTracker.paradaEsperandoConfirmacion != index) return // evita registrar dos veces

        actualizarDialogoConfirmacion(if (subio) "✅ Estudiante registrado" else "❌ No subió a la buseta")
        DriverTracker.registrarAsistencia(index, child.id_estudiante, subio, motivo)
        marcarParadaComoVisitada(index)
        TextToSpeechManager.speak(
            if (subio) "Estudiante registrado." else "Falta registrada."
        )

        window.decorView.postDelayed({
            cerrarDialogoConfirmacion()
            DriverTracker.resumeTracking()
        }, 1500)
    }

    private fun marcarParadaComoVisitada(index: Int) {
        runOnUiThread {
            paradaMarkers[index]?.setIcon(MapIconUtils.checkmarkMarker())
        }
    }

    private fun mostrarDialogoConfirmacion(child: EstudianteResponse, index: Int) {
        if (confirmacionDialog?.isShowing == true) return

        val view = layoutInflater.inflate(R.layout.dialog_confirmar_asistencia, null)
        view.findViewById<TextView>(R.id.tvNombreParadaDialog).text = child.nombre_completo
        view.findViewById<TextView>(R.id.tvDireccionParadaDialog).text =
            "Parada ${index + 1} · ${child.direccion}"
        confirmacionDialogView = view

        view.findViewById<MaterialButton>(R.id.btnSiSubio).setOnClickListener {
            registrarYAvanzar(index, child, subio = true, motivo = null)
        }
        view.findViewById<MaterialButton>(R.id.btnNoSubio).setOnClickListener {
            registrarYAvanzar(index, child, subio = false, motivo = "No subió")
        }

        confirmacionDialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(false)
            .create()
        confirmacionDialog?.show()
    }

    private fun actualizarDialogoConfirmacion(mensaje: String) {
        confirmacionDialogView?.findViewById<TextView>(R.id.tvEstadoDialog)?.text = mensaje
    }

    private fun cerrarDialogoConfirmacion() {
        confirmacionDialog?.dismiss()
        confirmacionDialog = null
        confirmacionDialogView = null
    }

    // --- Pantalla de ruta completada ---

    private fun mostrarPantallaRutaCompletada() {
        val view = layoutInflater.inflate(R.layout.dialog_ruta_completada, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(false)
            .create()
        view.findViewById<MaterialButton>(R.id.btnVolverInicioRuta).setOnClickListener {
            dialog.dismiss()
            finish()
        }
        dialog.show()

        // Cierra formalmente el viaje en el backend, igual que el botón "Finalizar Ruta"
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        choferRepository.finalizarViaje(driverEmail, sdf.format(java.util.Date())) { }
    }

    override fun onDestroy() {
        super.onDestroy()
        DriverTracker.onLocationUpdate = null // Remove listener to avoid leaks
        DriverTracker.onStopArrived = null
        DriverTracker.onRutaFinalizada = null
        confirmacionDialog?.dismiss()
        confirmacionDialog = null
        confirmacionDialogView = null
    }
}
