package com.example.busetaescolarapp.admin

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.busetaescolarapp.R
import com.example.busetaescolarapp.network.ApiClient
import com.example.busetaescolarapp.network.ApiResponse
import com.example.busetaescolarapp.network.SolicitudResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AdminSolicitudesActivity : AppCompatActivity() {

    private lateinit var rvSolicitudes: RecyclerView
    private var solicitudes = emptyList<SolicitudResponse>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_solicitudes)
        
        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbarAdminSolicitudes).setNavigationOnClickListener {
            finish()
        }

        rvSolicitudes = findViewById(R.id.rvSolicitudes)
        rvSolicitudes.layoutManager = LinearLayoutManager(this)

        cargarSolicitudes()
    }

    private fun cargarSolicitudes() {
        ApiClient.apiService.getSolicitudesPendientes().enqueue(object : Callback<List<SolicitudResponse>> {
            override fun onResponse(call: Call<List<SolicitudResponse>>, response: Response<List<SolicitudResponse>>) {
                if (response.isSuccessful) {
                    solicitudes = response.body() ?: emptyList()
                    val adapter = SolicitudesAdapter(solicitudes, { aprobar(it) }, { rechazar(it) })
                    rvSolicitudes.adapter = adapter
                } else {
                    Toast.makeText(this@AdminSolicitudesActivity, "Error al cargar solicitudes", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<List<SolicitudResponse>>, t: Throwable) {
                Toast.makeText(this@AdminSolicitudesActivity, "Fallo de red: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun aprobar(solicitud: SolicitudResponse) {
        ApiClient.apiService.aprobarSolicitud(solicitud.id_solicitud).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@AdminSolicitudesActivity, "Chofer Aprobado", Toast.LENGTH_SHORT).show()
                    cargarSolicitudes() // Recargar lista
                } else {
                    Toast.makeText(this@AdminSolicitudesActivity, "Error al aprobar", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Toast.makeText(this@AdminSolicitudesActivity, "Fallo de red", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun rechazar(solicitud: SolicitudResponse) {
        ApiClient.apiService.rechazarSolicitud(solicitud.id_solicitud).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@AdminSolicitudesActivity, "Solicitud Rechazada", Toast.LENGTH_SHORT).show()
                    cargarSolicitudes() // Recargar lista
                } else {
                    Toast.makeText(this@AdminSolicitudesActivity, "Error al rechazar", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Toast.makeText(this@AdminSolicitudesActivity, "Fallo de red", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
