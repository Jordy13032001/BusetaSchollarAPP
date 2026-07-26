package com.example.busetaescolarapp.padre

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.busetaescolarapp.NavigationUtils
import com.example.busetaescolarapp.NotificationHelper
import com.example.busetaescolarapp.R
import com.example.busetaescolarapp.network.ApiClient
import com.example.busetaescolarapp.network.EstudianteResponse
import com.example.busetaescolarapp.network.NotificationResponse
import com.example.busetaescolarapp.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PadreHomeActivity : AppCompatActivity() {

    private var parentEmail: String = ""
    private var parentName: String = ""
    private var lastNotificationId: Int = 0
    private var rvNinos: RecyclerView? = null

    private val handler = Handler(Looper.getMainLooper())
    private val notifRunnable = object : Runnable {
        override fun run() {
            verificarNotificacionesNuevas()
            handler.postDelayed(this, 15_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_padre_home)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val sessionManager = SessionManager(this)
        parentEmail = sessionManager.getUserEmail() ?: ""
        parentName = sessionManager.getUserName() ?: "Padre"

        lastNotificationId = getSharedPreferences("padre_prefs", MODE_PRIVATE)
            .getInt("last_notification_id_$parentEmail", 0)

        NotificationHelper.createNotificationChannel(this)

        findViewById<TextView>(R.id.tvGreeting)?.text = "¡Hola, $parentName!"

        rvNinos = findViewById(R.id.rvNinosPadre)
        rvNinos?.layoutManager = LinearLayoutManager(this)

        NavigationUtils.setupPadreBottomNavigation(this)
    }

    override fun onResume() {
        super.onResume()
        if (parentEmail.isNotEmpty()) {
            cargarHijos()
            handler.post(notifRunnable)
        }
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(notifRunnable)
    }

    private fun cargarHijos() {
        ApiClient.apiService.getParentChildren(parentEmail)
            .enqueue(object : Callback<List<EstudianteResponse>> {
                override fun onResponse(
                    call: Call<List<EstudianteResponse>>,
                    response: Response<List<EstudianteResponse>>
                ) {
                    if (!response.isSuccessful) return
                    val children = response.body() ?: emptyList()
                    rvNinos?.adapter = NinoPadreAdapter(children)
                }

                override fun onFailure(call: Call<List<EstudianteResponse>>, t: Throwable) {}
            })
    }

    private fun verificarNotificacionesNuevas() {
        if (parentEmail.isEmpty()) return
        ApiClient.apiService.getNotifications(parentEmail)
            .enqueue(object : Callback<List<NotificationResponse>> {
                override fun onResponse(
                    call: Call<List<NotificationResponse>>,
                    response: Response<List<NotificationResponse>>
                ) {
                    val notifs = response.body()?.takeIf { response.isSuccessful } ?: return
                    val nuevas = notifs.filter { it.id > lastNotificationId }
                    nuevas.forEach { notif ->
                        NotificationHelper.sendNotification(
                            this@PadreHomeActivity,
                            notif.title,
                            notif.message
                        )
                    }
                    if (nuevas.isNotEmpty()) {
                        lastNotificationId = nuevas.maxOf { it.id }
                        getSharedPreferences("padre_prefs", MODE_PRIVATE).edit()
                            .putInt("last_notification_id_$parentEmail", lastNotificationId)
                            .apply()
                    }
                }

                override fun onFailure(call: Call<List<NotificationResponse>>, t: Throwable) {}
            })
    }
}

class NinoPadreAdapter(
    private val children: List<EstudianteResponse>
) : RecyclerView.Adapter<NinoPadreAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombreNino)
        val tvChofer: TextView = view.findViewById(R.id.tvChofer)
        val tvEstado: TextView = view.findViewById(R.id.tvEstadoSolicitud)
        val tvDireccion: TextView = view.findViewById(R.id.tvDireccion)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_nino_padre, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val child = children[position]
        holder.tvNombre.text = child.nombre_completo
        holder.tvChofer.text = "Chofer: ${child.nombre_chofer ?: child.correo_chofer}"
        holder.tvEstado.text = when (child.estado) {
            "ACEPTADO" -> if (child.subio == true) "Ya subió a la buseta ✅" else "En ruta"
            "PENDIENTE" -> "Pendiente de aceptación"
            "RECHAZADO" -> "Solicitud rechazada"
            else -> child.estado ?: "Sin estado"
        }
        holder.tvDireccion.text = "Parada: ${child.direccion}"
    }

    override fun getItemCount() = children.size
}
