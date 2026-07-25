package com.example.busetaescolarapp.chofer

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.busetaescolarapp.R
import com.example.busetaescolarapp.data.repository.ChoferRepository
import com.example.busetaescolarapp.data.repository.RutaRepository
import com.example.busetaescolarapp.network.EstudianteResponse
import com.example.busetaescolarapp.utils.SessionManager
import kotlinx.coroutines.launch

/**
 * El chofer revisa los estudiantes que los padres le solicitaron y decide
 * cuáles acepta. Con los aceptados se arma su ruta.
 */
class SolicitudesEstudiantesActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var tvSinSolicitudes: TextView
    private lateinit var adapter: SolicitudEstudianteAdapter
    private lateinit var rutaRepository: RutaRepository
    private val choferRepository = ChoferRepository()
    private val solicitudes = mutableListOf<EstudianteResponse>()
    private var driverEmail: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_solicitudes_estudiantes)

        recycler = findViewById(R.id.rvSolicitudesEstudiantes)
        tvSinSolicitudes = findViewById(R.id.tvSinSolicitudes)
        recycler.layoutManager = LinearLayoutManager(this)

        rutaRepository = RutaRepository(applicationContext)
        driverEmail = SessionManager(this).getUserEmail() ?: ""

        adapter = SolicitudEstudianteAdapter(
            solicitudes,
            onAceptar = { responder(it, aceptar = true) },
            onRechazar = { responder(it, aceptar = false) }
        )
        recycler.adapter = adapter

        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbarSolicitudesEstudiantes)
            ?.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        if (driverEmail.isNotEmpty()) cargarSolicitudes()
    }

    private fun cargarSolicitudes() {
        choferRepository.getSolicitudesEstudiantes(driverEmail) { resultado ->
            solicitudes.clear()
            if (resultado != null) solicitudes.addAll(resultado)
            adapter.notifyDataSetChanged()
            actualizarEstadoVacio()
        }
    }

    private fun responder(estudiante: EstudianteResponse, aceptar: Boolean) {
        val alTerminar: (Boolean) -> Unit = { exito ->
            if (exito) {
                adapter.quitar(estudiante)
                actualizarEstadoVacio()
                Toast.makeText(
                    this,
                    if (aceptar) "${estudiante.nombre_completo} agregado a tu ruta" else "Solicitud rechazada",
                    Toast.LENGTH_SHORT
                ).show()

                // La ruta cambió: los tramos cacheados en Room ya no sirven y deben recalcularse.
                lifecycleScope.launch { rutaRepository.invalidarCache(driverEmail) }
            } else {
                Toast.makeText(this, "No se pudo procesar la solicitud", Toast.LENGTH_SHORT).show()
            }
        }

        if (aceptar) {
            choferRepository.aceptarEstudiante(estudiante.id_estudiante, alTerminar)
        } else {
            choferRepository.rechazarEstudiante(estudiante.id_estudiante, alTerminar)
        }
    }

    private fun actualizarEstadoVacio() {
        val vacio = solicitudes.isEmpty()
        tvSinSolicitudes.visibility = if (vacio) View.VISIBLE else View.GONE
        recycler.visibility = if (vacio) View.GONE else View.VISIBLE
    }
}
