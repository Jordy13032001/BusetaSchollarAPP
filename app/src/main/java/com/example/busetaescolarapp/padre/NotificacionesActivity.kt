package com.example.busetaescolarapp.padre

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.busetaescolarapp.R
import com.example.busetaescolarapp.NavigationUtils
import com.example.busetaescolarapp.NotificationHelper

import android.widget.Toast
import com.example.busetaescolarapp.data.local.NotificacionEntity
import com.example.busetaescolarapp.network.ApiClient
import com.example.busetaescolarapp.network.EstudianteResponse
import com.example.busetaescolarapp.ui.viewmodel.PadreViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NotificacionesActivity : AppCompatActivity() {
    private lateinit var recycler: RecyclerView
    private lateinit var parentEmail: String
    private lateinit var viewModel: PadreViewModel
    private var poller: NotificacionPoller? = null

    // Tras pagar o reenviar hay que resincronizar: el estado del hijo cambió
    private val accionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (parentEmail.isNotEmpty()) {
            viewModel.sincronizarNotificaciones(parentEmail)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notificaciones)

        recycler = findViewById(R.id.recyclerNotificaciones)
        recycler.layoutManager = LinearLayoutManager(this)

        val sessionManager = com.example.busetaescolarapp.utils.SessionManager(this)
        parentEmail = sessionManager.getUserEmail() ?: ""

        viewModel = ViewModelProvider(this)[PadreViewModel::class.java]

        // También aquí se avisa por notificación del sistema: el padre puede estar
        // en esta pantalla justo cuando el chofer resuelve la solicitud.
        NotificationHelper.createNotificationChannel(this)
        poller = NotificacionPoller(this, parentEmail)

        if (parentEmail.isNotEmpty()) {
            // Room es la fuente de la UI: se actualiza sola en cuanto llegan datos nuevos
            viewModel.notificaciones(parentEmail).observe(this) { entities ->
                recycler.adapter = NotificacionAdapter(
                    entities.map { it.toDomain() },
                    onPagar = { abrirPago(it) },
                    onReenviar = { abrirSeleccionDeChofer(it) }
                )
            }
            // Dispara la sincronización con el backend (si falla, queda lo último guardado en Room)
            viewModel.sincronizarNotificaciones(parentEmail)
        } else {
            Toast.makeText(this, "Error de sesión", Toast.LENGTH_SHORT).show()
        }

        NavigationUtils.setupPadreBottomNavigation(this)
        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbarNotificaciones)?.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        if (parentEmail.isNotEmpty()) {
            viewModel.sincronizarNotificaciones(parentEmail)
            poller?.iniciar()
        }
    }

    override fun onPause() {
        super.onPause()
        poller?.detener()
    }

    private fun abrirPago(notificacion: Notificacion) {
        val id = notificacion.idEstudiante ?: return
        val intent = Intent(this, PagoActivity::class.java).apply {
            putExtra(PagoActivity.EXTRA_ID_ESTUDIANTE, id)
            putExtra(
                PagoActivity.EXTRA_NOMBRE_ESTUDIANTE,
                notificacion.nombreEstudiante ?: "Estudiante"
            )
        }
        accionLauncher.launch(intent)
    }

    /**
     * Para reenviar hace falta la dirección del niño, que no viaja en la notificación:
     * se busca en la lista de hijos. Si no se encuentra, se abre igual y el backend
     * conserva la parada anterior.
     */
    private fun abrirSeleccionDeChofer(notificacion: Notificacion) {
        val id = notificacion.idEstudiante ?: return
        val nombre = notificacion.nombreEstudiante ?: "Estudiante"

        ApiClient.apiService.getParentChildren(parentEmail)
            .enqueue(object : Callback<List<EstudianteResponse>> {
                override fun onResponse(
                    call: Call<List<EstudianteResponse>>,
                    response: Response<List<EstudianteResponse>>
                ) {
                    val hijo = response.body()
                        ?.takeIf { response.isSuccessful }
                        ?.firstOrNull { it.id_estudiante == id }
                    lanzarReenvio(id, hijo?.nombre_completo ?: nombre, hijo?.direccion)
                }

                override fun onFailure(call: Call<List<EstudianteResponse>>, t: Throwable) {
                    lanzarReenvio(id, nombre, null)
                }
            })
    }

    private fun lanzarReenvio(idEstudiante: Int, nombre: String, direccion: String?) {
        val intent = Intent(this, AddChildActivity::class.java).apply {
            putExtra(AddChildActivity.EXTRA_REASIGNAR_ID, idEstudiante)
            putExtra(AddChildActivity.EXTRA_REASIGNAR_NOMBRE, nombre)
            putExtra(AddChildActivity.EXTRA_REASIGNAR_DIRECCION, direccion.orEmpty())
        }
        accionLauncher.launch(intent)
    }

    private fun NotificacionEntity.toDomain(): Notificacion {
        val typeEnum = try {
            TipoNotificacion.valueOf(tipo)
        } catch (e: Exception) {
            TipoNotificacion.CERCA
        }
        return Notificacion(
            titulo = titulo,
            mensaje = mensaje,
            hora = hora,
            tipo = typeEnum,
            idEstudiante = idEstudiante,
            nombreEstudiante = nombreEstudiante,
            estadoEstudiante = estadoEstudiante
        )
    }
}
