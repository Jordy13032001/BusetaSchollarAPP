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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.busetaescolarapp.NavigationUtils
import com.example.busetaescolarapp.NotificationHelper
import com.example.busetaescolarapp.R
import com.example.busetaescolarapp.network.ApiClient
import com.example.busetaescolarapp.network.EstudianteResponse
import com.example.busetaescolarapp.network.ViajeResponse
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chofer_home)

        com.example.busetaescolarapp.utils.TextToSpeechManager.init(this)
        com.example.busetaescolarapp.utils.VoiceRecognitionManager.init(this)
        viewModel = androidx.lifecycle.ViewModelProvider(this).get(com.example.busetaescolarapp.ui.viewmodel.ChoferViewModel::class.java)

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
        
        viewModel.ruta.observe(this) { ruta ->
            childrenList = ruta
            presentChildrenList.clear()
            presentIds.clear()
            val adapter = AsistenciaAdapter(presentChildrenList, presentIds, isReadOnly = true)
            rvAsistencia.adapter = adapter
            presentKidsCount = 0
            tvKidsCount.text = "${childrenList.size} / $presentKidsCount"
            tvRouteName.text = "Ruta Asignada"
        }

        viewModel.viajeActivo.observe(this) { viaje ->
            if (viaje != null) {
                startRouteSimulation(viaje.id_viaje)
            } else {
                DriverTracker.stopTracking()
                tvRouteStatus.text = "Finalizada"
                tvRouteStatus.setBackgroundColor(android.graphics.Color.RED)
                Toast.makeText(this@ChoferHomeActivity, "Ruta finalizada. ¡Buen trabajo!", Toast.LENGTH_SHORT).show()
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

        btnFinalizar?.setOnClickListener {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            val fechaCelular = sdf.format(java.util.Date())
            viewModel.finalizarViaje(driverEmail, fechaCelular)
            
            // Reiniciar la lista visual de asistencia al finalizar
            presentKidsCount = 0
            tvKidsCount.text = "${childrenList.size} / 0"
            presentChildrenList.clear()
            presentIds.clear()
            val resetAdapter = AsistenciaAdapter(presentChildrenList, presentIds, isReadOnly = true)
            rvAsistencia.adapter = resetAdapter
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
        }
    }
    
    private fun startRouteSimulation(idViaje: Int) {
        Toast.makeText(this, "Preparando simulación GPS...", Toast.LENGTH_SHORT).show()
        tvRouteStatus.text = "En progreso"
        tvRouteStatus.setBackgroundColor(android.graphics.Color.parseColor("#F57F17"))

        Thread {
            val geocoder = android.location.Geocoder(this, java.util.Locale.getDefault())
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
                        // Fallback a coordenadas genericas de Cuenca si no encuentra la direccion,
                        // asi no nos saltamos al niño en la simulacion.
                        points.add(com.google.android.gms.maps.model.LatLng(-2.9000, -79.0000))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Fallback tambien en caso de excepcion
                    points.add(com.google.android.gms.maps.model.LatLng(-2.9000, -79.0000))
                }
            }
            runOnUiThread {
                if (points.isNotEmpty()) {
                    DriverTracker.onStopArrived = { index ->
                        handleStopArrived(index, idViaje)
                    }
                    DriverTracker.startTracking(driverEmail, points, idViaje)
                    com.example.busetaescolarapp.utils.TextToSpeechManager.speak("Iniciando ruta escolar. Que tenga un buen viaje.")
                    Toast.makeText(this, "Ruta iniciada correctamente", Toast.LENGTH_SHORT).show()
                    
                    // Redirigir al mapa
                    startActivity(android.content.Intent(this@ChoferHomeActivity, MapaChoferActivity::class.java))
                } else {
                    Toast.makeText(this, "Error obteniendo coordenadas GPS", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun handleStopArrived(index: Int, idViaje: Int) {
        val child = childrenList[index]
        promptVoice(child, index, idViaje, "Parada de ${child.nombre_completo}. ¿Subió a la buseta?")
    }

    private fun promptVoice(child: EstudianteResponse, index: Int, idViaje: Int, message: String) {
        Toast.makeText(this, "🗣️ Hablando: $message", Toast.LENGTH_LONG).show()
        com.example.busetaescolarapp.utils.TextToSpeechManager.speak(message)
        
        // Esperamos a que termine de hablar (aprox 3 segundos) para empezar a escuchar
        window.decorView.postDelayed({
            Toast.makeText(this, "🎤 Escuchando...", Toast.LENGTH_SHORT).show()
            com.example.busetaescolarapp.utils.VoiceRecognitionManager.startListening(
                onResult = { result ->
                    val text = result.lowercase()
                    val isYes = text.contains("sí") || text.contains("si") || text.contains("claro")
                    val isNo = text.contains("no") || text.contains("nunca") || text.contains("tampoco")
                    
                    if (!isYes && !isNo) {
                        Toast.makeText(this, "Respuesta no reconocida: $text", Toast.LENGTH_SHORT).show()
                        promptVoice(child, index, idViaje, "Por favor registre la asistencia de nuevo.")
                        return@startListening
                    }
                    
                    val subio = isYes
                    Toast.makeText(this, "Entendido: $text -> Asistencia: $subio", Toast.LENGTH_SHORT).show()
                    viewModel.marcarAsistencia(idViaje, child.id_estudiante, subio, if (subio) null else "No respondió (Voz)")
                    
                    if (subio) {
                        presentKidsCount++
                        tvKidsCount.text = "${childrenList.size} / $presentKidsCount"
                        
                        // Agregar niño a la lista de "Sí asistieron"
                        presentChildrenList.add(child)
                        presentIds.add(child.id_estudiante)
                        rvAsistencia.adapter?.notifyItemInserted(presentChildrenList.size - 1)
                    }
                    
                    com.example.busetaescolarapp.utils.TextToSpeechManager.speak(if (subio) "Asistencia guardada." else "Falta registrada.")
                    
                    // Continuamos a la siguiente parada
                    window.decorView.postDelayed({
                        DriverTracker.resumeTracking()
                    }, 2000)
                },
                onError = { error ->
                    Toast.makeText(this, "Error de voz: $error. ¿Repetir?", Toast.LENGTH_SHORT).show()
                    promptVoice(child, index, idViaje, "Por favor registre la asistencia de nuevo.")
                }
            )
        }, 3500)
    }

    override fun onDestroy() {
        super.onDestroy()
        com.example.busetaescolarapp.utils.TextToSpeechManager.shutdown()
        com.example.busetaescolarapp.utils.VoiceRecognitionManager.shutdown()
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