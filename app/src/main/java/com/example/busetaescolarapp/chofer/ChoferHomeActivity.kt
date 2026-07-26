package com.example.busetaescolarapp.chofer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.busetaescolarapp.NavigationUtils
import com.example.busetaescolarapp.NotificationHelper
import com.example.busetaescolarapp.R
import com.example.busetaescolarapp.data.repository.ChoferRepository
import com.example.busetaescolarapp.data.repository.RutaRepository
import com.example.busetaescolarapp.network.ApiClient
import com.example.busetaescolarapp.network.EstudianteResponse
import com.example.busetaescolarapp.network.ViajeResponse
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChoferHomeActivity : AppCompatActivity() {

    private lateinit var rvAsistencia: RecyclerView
    private lateinit var tvDriverName: TextView
    private lateinit var tvRouteName: TextView
    private lateinit var tvKidsCount: TextView
    private lateinit var tvRouteStatus: TextView
    private var driverEmail: String = ""
    private var driverName: String = ""
    private var childrenList = emptyList<EstudianteResponse>()
    private val presentChildrenList = mutableListOf<EstudianteResponse>()
    private val presentIds = mutableSetOf<Int>()
    private var presentKidsCount = 0
    private lateinit var viewModel: com.example.busetaescolarapp.ui.viewmodel.ChoferViewModel
    private lateinit var rutaRepository: RutaRepository
    private var colegioLatLng = com.google.android.gms.maps.model.LatLng(-2.9065, -79.0040)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chofer_home)

        com.example.busetaescolarapp.utils.TextToSpeechManager.init(this)
        com.example.busetaescolarapp.utils.VoiceRecognitionManager.init(this)
        viewModel = androidx.lifecycle.ViewModelProvider(this).get(com.example.busetaescolarapp.ui.viewmodel.ChoferViewModel::class.java)
        rutaRepository = RutaRepository(applicationContext)

        NotificationHelper.createNotificationChannel(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 101)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 101)
            }
        }
        tvDriverName = findViewById(R.id.tvDriverName)
        tvRouteName = findViewById(R.id.tvRouteName)
        tvKidsCount = findViewById(R.id.tvKidsCount)
        tvRouteStatus = findViewById(R.id.tvRouteStatus)
        
        val btnIniciar = findViewById<Button>(R.id.btnIniciarRuta)
        val btnFinalizar = findViewById<Button>(R.id.btnFinalizarRuta)
        rvAsistencia = findViewById(R.id.rvAsistencia)
        rvAsistencia.layoutManager = LinearLayoutManager(this)

        val sessionManager = com.example.busetaescolarapp.utils.SessionManager(this)
        driverEmail = sessionManager.getUserEmail() ?: ""
        driverName = sessionManager.getUserName() ?: "Conductor"
        
        tvDriverName.text = "Bienvenido, $driverName"

        NavigationUtils.setupChoferBottomNavigation(this)

        findViewById<android.widget.ImageView>(R.id.btnToolbarIncidente)?.setOnClickListener {
            startActivity(android.content.Intent(this, IncidenteChoferActivity::class.java))
        }

        // La confirmación de voz ocurre en MapaChoferActivity (pantalla visible durante el viaje);
        // aquí solo escuchamos el resultado para mantener esta lista de asistencia sincronizada.
        DriverTracker.onAsistenciaRegistrada = { index, subio ->
            runOnUiThread {
                val child = childrenList.getOrNull(index)
                if (subio && child != null && !presentIds.contains(child.id_estudiante)) {
                    presentKidsCount++
                    presentChildrenList.add(child)
                    presentIds.add(child.id_estudiante)
                    rvAsistencia.adapter?.notifyItemInserted(presentChildrenList.size - 1)
                }
            }
        }

        viewModel.ruta.observe(this) { ruta ->
            childrenList = ruta
            presentChildrenList.clear()
            presentIds.clear()
            val adapter = AsistenciaAdapter(presentChildrenList, presentIds, isReadOnly = true)
            rvAsistencia.adapter = adapter
            presentKidsCount = 0
            tvKidsCount.text = "${childrenList.size}"
            tvRouteName.text = "Ruta Asignada"
        }

        viewModel.viajeActivo.observe(this) { viaje ->
            if (viaje != null) {
                if (!DriverTracker.isTracking()) {
                    startRouteSimulation(viaje.id_viaje)
                } else {
                    tvRouteStatus.text = "En progreso"
                    tvRouteStatus.setBackgroundColor(android.graphics.Color.parseColor("#F57F17"))
                }
            } else {
                tvRouteStatus.text = "Sin iniciar"
                tvRouteStatus.setBackgroundColor(android.graphics.Color.parseColor("#757575"))
            }
        }

        btnIniciar?.setOnClickListener {
            if (childrenList.isEmpty()) {
                Toast.makeText(this, "No tienes niños asignados", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (DriverTracker.isTracking()) {
                Toast.makeText(this, "La ruta ya está en progreso", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.iniciarViaje(driverEmail)
        }

        // btnFinalizarRuta está oculto (visibility=gone); la finalización ocurre en MapaChoferActivity
        
        findViewById<androidx.cardview.widget.CardView>(R.id.cardSolicitudesEstudiantes)?.setOnClickListener {
            startActivity(android.content.Intent(this, SolicitudesEstudiantesActivity::class.java))
        }

        val btnSwitchToPadre = findViewById<Button>(R.id.btnSwitchToPadre)
        if (sessionManager.hasRole("padre")) {
            btnSwitchToPadre?.visibility = View.VISIBLE
            btnSwitchToPadre?.setOnClickListener {
                sessionManager.setCurrentRole("padre")
                val intent = android.content.Intent(this, com.example.busetaescolarapp.MainActivity::class.java)
                intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
        
        if (driverEmail.isNotEmpty()) {
            viewModel.loadRuta(driverEmail)
            cargarColegio()
        }
    }

    private fun cargarColegio() {
        ChoferRepository().getRutaInfo(driverEmail) { info ->
            val lat = info?.lat_colegio ?: return@getRutaInfo
            val lng = info.lng_colegio ?: return@getRutaInfo
            colegioLatLng = com.google.android.gms.maps.model.LatLng(lat, lng)
        }
    }

    override fun onResume() {
        super.onResume()
        if (driverEmail.isNotEmpty()) {
            viewModel.loadRuta(driverEmail)
            actualizarContadorSolicitudes()
        }
        // Si el tracker no está corriendo, el viaje terminó (o nunca inició).
        // Resetear el ViewModel para que el observer refleje "Sin iniciar".
        if (!DriverTracker.isTracking()) {
            viewModel.resetViajeActivo()
        }
    }

    private fun actualizarContadorSolicitudes() {
        val tvContador = findViewById<TextView>(R.id.tvContadorSolicitudes) ?: return
        ChoferRepository().getSolicitudesEstudiantes(driverEmail) { pendientes ->
            val cantidad = pendientes?.size ?: 0
            tvContador.text = if (cantidad > 0) "Solicitudes ($cantidad)" else "Solicitudes"
        }
    }

    private fun startRouteSimulation(idViaje: Int) {
        Toast.makeText(this, "Preparando simulación GPS...", Toast.LENGTH_SHORT).show()
        tvRouteStatus.text = "En progreso"
        tvRouteStatus.setBackgroundColor(android.graphics.Color.parseColor("#F57F17"))

        lifecycleScope.launch {
            val points = geocodificarParadas()

            if (points.isNotEmpty()) {
                // Ruta real siguiendo calles + tiempo estimado por tramo (Directions API, cacheado en Room)
                val tramos = rutaRepository.obtenerTramos(driverEmail, points)

                DriverTracker.startTracking(driverEmail, points, idViaje, tramos)
                com.example.busetaescolarapp.utils.TextToSpeechManager.speak("Iniciando ruta escolar. Que tenga un buen viaje.")
                Toast.makeText(this@ChoferHomeActivity, "Ruta iniciada correctamente", Toast.LENGTH_SHORT).show()

                // Redirigir al mapa
                startActivity(android.content.Intent(this@ChoferHomeActivity, MapaChoferActivity::class.java))
            } else {
                Toast.makeText(this@ChoferHomeActivity, "Error obteniendo coordenadas GPS", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun geocodificarParadas(): List<com.google.android.gms.maps.model.LatLng> =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val geocoder = android.location.Geocoder(this@ChoferHomeActivity, java.util.Locale.getDefault())
            val points = mutableListOf<com.google.android.gms.maps.model.LatLng>()
            for (child in childrenList) {
                if (child.lat != null && child.lng != null) {
                    points.add(com.google.android.gms.maps.model.LatLng(child.lat, child.lng))
                    continue
                }
                try {
                    val addresses = geocoder.getFromLocationName("${child.direccion}, Cuenca, Ecuador", 1)
                    if (!addresses.isNullOrEmpty()) {
                        points.add(com.google.android.gms.maps.model.LatLng(addresses[0].latitude, addresses[0].longitude))
                    } else {
                        points.add(com.google.android.gms.maps.model.LatLng(-2.9000, -79.0000))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    points.add(com.google.android.gms.maps.model.LatLng(-2.9000, -79.0000))
                }
            }
            // El colegio es siempre la última parada de la ruta
            points.add(colegioLatLng)
            points
        }

    override fun onDestroy() {
        super.onDestroy()
        DriverTracker.onAsistenciaRegistrada = null
        // Si la ruta sigue activa, el mapa está usando voz: apagarla aquí la dejaría muda.
        if (!DriverTracker.isTracking()) {
            com.example.busetaescolarapp.utils.TextToSpeechManager.shutdown()
            com.example.busetaescolarapp.utils.VoiceRecognitionManager.shutdown()
        }
    }
}

class AsistenciaAdapter(
    private var children: List<EstudianteResponse>,
    private val checkedIds: Set<Int>,
    private val isReadOnly: Boolean = false,
    private val showCheckbox: Boolean = true,
    private val onCheckedChange: ((EstudianteResponse, Boolean) -> Unit)? = null
) : RecyclerView.Adapter<AsistenciaAdapter.ViewHolder>() {

    fun updateData(newChildren: List<EstudianteResponse>) {
        children = newChildren
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvChildName)
        val tvAddress: TextView = view.findViewById(R.id.tvChildAddress)
        val cbAttendance: CheckBox = view.findViewById(R.id.cbAttendance)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_asistencia, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val child = children[position]
        holder.tvName.text = child.nombre_completo
        holder.tvAddress.text = child.direccion
        
        if (showCheckbox) {
            holder.cbAttendance.visibility = View.VISIBLE
            holder.cbAttendance.setOnCheckedChangeListener(null)
            holder.cbAttendance.isChecked = checkedIds.contains(child.id_estudiante)
            holder.cbAttendance.isClickable = !isReadOnly
            holder.cbAttendance.setOnCheckedChangeListener { _, isChecked ->
                onCheckedChange?.invoke(child, isChecked)
            }
        } else {
            holder.cbAttendance.visibility = View.GONE
        }
    }

    override fun getItemCount() = children.size
}