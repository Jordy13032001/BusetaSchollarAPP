package com.example.busetaescolarapp.padre

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.view.View
import com.example.busetaescolarapp.R
import com.example.busetaescolarapp.NavigationUtils

import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.busetaescolarapp.network.ApiClient
import com.example.busetaescolarapp.network.IncidentRequest
import com.example.busetaescolarapp.network.IncidentResponse
import com.example.busetaescolarapp.ui.viewmodel.PadreViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class IncidenteActivity : AppCompatActivity() {

    private lateinit var rvIncidentes: RecyclerView
    private lateinit var tvNoIncidentes: TextView
    private lateinit var parentEmail: String
    private lateinit var viewModel: PadreViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incidente)
        NavigationUtils.setupPadreBottomNavigation(this)

        rvIncidentes = findViewById(R.id.rvIncidentes)
        tvNoIncidentes = findViewById(R.id.tvNoIncidentes)
        rvIncidentes.layoutManager = LinearLayoutManager(this)

        val sessionManager = com.example.busetaescolarapp.utils.SessionManager(this)
        parentEmail = sessionManager.getUserEmail() ?: ""

        viewModel = ViewModelProvider(this)[PadreViewModel::class.java]

        if (parentEmail.isNotEmpty()) {
            viewModel.incidentes(parentEmail).observe(this) { incidents ->
                if (incidents.isEmpty()) {
                    tvNoIncidentes.visibility = View.VISIBLE
                    rvIncidentes.visibility = View.GONE
                } else {
                    tvNoIncidentes.visibility = View.GONE
                    rvIncidentes.visibility = View.VISIBLE
                    rvIncidentes.adapter = IncidentAdapter(incidents)
                }
            }
            viewModel.sincronizarIncidentes(parentEmail)
        }

        findViewById<FloatingActionButton>(R.id.fabReportarIncidente)?.setOnClickListener {
            showReportDialog()
        }
    }

    private fun showReportDialog() {
        val input = EditText(this)
        input.hint = "Ej. Mi hijo está enfermo hoy"

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Reportar Incidente")
            .setView(input)
            .setPositiveButton("Enviar") { _, _ ->
                val text = input.text.toString()
                if (text.isNotEmpty()) {
                    val request = IncidentRequest(text, parentEmail)
                    ApiClient.apiService.reportIncident(request).enqueue(object : Callback<IncidentResponse> {
                        override fun onResponse(call: Call<IncidentResponse>, response: Response<IncidentResponse>) {
                            if (response.isSuccessful) {
                                Toast.makeText(this@IncidenteActivity, "Incidente reportado", Toast.LENGTH_SHORT).show()
                                viewModel.sincronizarIncidentes(parentEmail) // Recargar lista desde el backend hacia Room
                            }
                        }
                        override fun onFailure(call: Call<IncidentResponse>, t: Throwable) {
                            Toast.makeText(this@IncidenteActivity, "Error al enviar", Toast.LENGTH_SHORT).show()
                        }
                    })
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
