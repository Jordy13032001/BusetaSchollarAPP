package com.example.busetaescolarapp.padre

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.busetaescolarapp.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PadreHomeActivity : AppCompatActivity() {

    private var parentEmail: String = ""
    private var parentName: String = ""
    private var rvNinos: RecyclerView? = null

    private val detalleHijoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // El padre quitó al niño de la ruta → refrescar la lista
            cargarHijos()
        }
    }

    private var poller: NotificacionPoller? = null

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

        NotificationHelper.createNotificationChannel(this)
        poller = NotificacionPoller(this, parentEmail)

        findViewById<TextView>(R.id.tvGreeting)?.text = "¡Hola, $parentName!"

        rvNinos = findViewById(R.id.rvNinosPadre)
        rvNinos?.layoutManager = LinearLayoutManager(this)

        NavigationUtils.setupPadreBottomNavigation(this)
    }

    override fun onResume() {
        super.onResume()
        if (parentEmail.isNotEmpty()) {
            cargarHijos()
            poller?.iniciar()
        }
    }

    override fun onPause() {
        super.onPause()
        poller?.detener()
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
                    rvNinos?.adapter = NinoPadreAdapter(children) { child ->
                        val intent = Intent(this@PadreHomeActivity, DetalleHijoActivity::class.java).apply {
                            putExtra(DetalleHijoActivity.EXTRA_ID, child.id_estudiante)
                            putExtra(DetalleHijoActivity.EXTRA_NOMBRE, child.nombre_completo)
                            putExtra(DetalleHijoActivity.EXTRA_DIRECCION, child.direccion)
                            putExtra(DetalleHijoActivity.EXTRA_CHOFER, child.nombre_chofer ?: child.correo_chofer)
                            putExtra(DetalleHijoActivity.EXTRA_HORA, child.hora_estimada ?: "--:--")
                            putExtra(DetalleHijoActivity.EXTRA_ESTADO, child.estado ?: "")
                        }
                        detalleHijoLauncher.launch(intent)
                    }
                }

                override fun onFailure(call: Call<List<EstudianteResponse>>, t: Throwable) {}
            })
    }

}

class NinoPadreAdapter(
    private val children: List<EstudianteResponse>,
    private val onItemClick: (EstudianteResponse) -> Unit
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
        holder.itemView.setOnClickListener { onItemClick(child) }
    }

    override fun getItemCount() = children.size
}
