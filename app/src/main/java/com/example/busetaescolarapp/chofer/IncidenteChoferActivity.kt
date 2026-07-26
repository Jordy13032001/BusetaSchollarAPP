package com.example.busetaescolarapp.chofer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.busetaescolarapp.NavigationUtils
import com.example.busetaescolarapp.R
import com.example.busetaescolarapp.network.ApiClient
import com.example.busetaescolarapp.network.IncidentRequest
import com.example.busetaescolarapp.network.IncidentResponse
import com.example.busetaescolarapp.utils.SessionManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class IncidenteChoferActivity : AppCompatActivity() {

    private lateinit var rvIncidentes: RecyclerView
    private lateinit var tvNoIncidentes: TextView
    private lateinit var choferEmail: String
    private val incidentes = mutableListOf<IncidentResponse>()
    private lateinit var adapter: IncidenteChoferAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incidente_chofer)
        NavigationUtils.setupChoferBottomNavigation(this)

        rvIncidentes = findViewById(R.id.rvIncidentes)
        tvNoIncidentes = findViewById(R.id.tvNoIncidentes)
        rvIncidentes.layoutManager = LinearLayoutManager(this)

        choferEmail = SessionManager(this).getUserEmail() ?: ""

        adapter = IncidenteChoferAdapter(incidentes)
        rvIncidentes.adapter = adapter

        if (choferEmail.isNotEmpty()) cargarIncidentes()

        findViewById<FloatingActionButton>(R.id.fabReportarIncidente)?.setOnClickListener {
            mostrarDialogoReporte()
        }
    }

    private fun cargarIncidentes() {
        ApiClient.apiService.getIncidents(choferEmail)
            .enqueue(object : Callback<List<IncidentResponse>> {
                override fun onResponse(
                    call: Call<List<IncidentResponse>>,
                    response: Response<List<IncidentResponse>>
                ) {
                    if (!response.isSuccessful) return
                    incidentes.clear()
                    incidentes.addAll(response.body() ?: emptyList())
                    adapter.notifyDataSetChanged()
                    actualizarVista()
                }

                override fun onFailure(call: Call<List<IncidentResponse>>, t: Throwable) {}
            })
    }

    private fun mostrarDialogoReporte() {
        val input = EditText(this).apply { hint = "Ej. Retraso por tráfico en Av. Principal" }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Reportar Incidente")
            .setView(input)
            .setPositiveButton("Enviar") { _, _ ->
                val texto = input.text.toString().trim()
                if (texto.isEmpty()) return@setPositiveButton
                ApiClient.apiService.reportIncident(IncidentRequest(texto, choferEmail))
                    .enqueue(object : Callback<IncidentResponse> {
                        override fun onResponse(
                            call: Call<IncidentResponse>,
                            response: Response<IncidentResponse>
                        ) {
                            if (response.isSuccessful) {
                                Toast.makeText(this@IncidenteChoferActivity, "Incidente reportado", Toast.LENGTH_SHORT).show()
                                cargarIncidentes()
                            }
                        }

                        override fun onFailure(call: Call<IncidentResponse>, t: Throwable) {
                            Toast.makeText(this@IncidenteChoferActivity, "Error al enviar", Toast.LENGTH_SHORT).show()
                        }
                    })
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun actualizarVista() {
        val vacio = incidentes.isEmpty()
        tvNoIncidentes.visibility = if (vacio) View.VISIBLE else View.GONE
        rvIncidentes.visibility = if (vacio) View.GONE else View.VISIBLE
    }
}

class IncidenteChoferAdapter(
    private val items: List<IncidentResponse>
) : RecyclerView.Adapter<IncidenteChoferAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMensaje: TextView = view.findViewById(R.id.tvIncidentDescription)
        val tvFecha: TextView = view.findViewById(R.id.tvIncidentDate)
        val tvEstado: TextView = view.findViewById(R.id.tvIncidentStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_incident, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvMensaje.text = item.mensaje
        holder.tvFecha.text = item.fecha_hora
        holder.tvEstado.text = item.estado
    }

    override fun getItemCount() = items.size
}
