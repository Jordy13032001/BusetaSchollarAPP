package com.example.busetaescolarapp.chofer

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.busetaescolarapp.NotificationHelper
import com.example.busetaescolarapp.R
import com.example.busetaescolarapp.network.ApiClient
import com.example.busetaescolarapp.network.ApiResponse
import com.example.busetaescolarapp.network.ViajeResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ResumenViajeActivity : AppCompatActivity() {

    private var driverEmail: String = ""
    private var currentViajeId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resumen_viaje)

        val sessionManager = com.example.busetaescolarapp.utils.SessionManager(this)
        driverEmail = sessionManager.getUserEmail() ?: ""

        NotificationHelper.createNotificationChannel(this)

        currentViajeId = DriverTracker.currentViajeId

        // "Cerrar": guarda la ruta y vuelve a la pantalla de inicio del chofer
        findViewById<CardView>(R.id.btnFinalizarTotal)?.setOnClickListener {
            guardarYCerrar()
        }

        findViewById<CardView>(R.id.btnEnviarAPadres)?.setOnClickListener {
            enviarNotificacionesAPadres()
        }

        loadRutaStats()
    }

    private fun guardarYCerrar() {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        val fechaCelular = sdf.format(java.util.Date())
        val request = com.example.busetaescolarapp.network.FinalizarRequest(fechaCelular)
        ApiClient.apiService.finalizarViaje(driverEmail, request).enqueue(object : Callback<ViajeResponse> {
            override fun onResponse(call: Call<ViajeResponse>, response: Response<ViajeResponse>) {
                runOnUiThread { volverAlInicio() }
            }
            override fun onFailure(call: Call<ViajeResponse>, t: Throwable) {
                runOnUiThread { volverAlInicio() }
            }
        })
    }

    private fun volverAlInicio() {
        val intent = Intent(this, ChoferHomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }

    private fun enviarNotificacionesAPadres() {
        val idViaje = currentViajeId ?: run {
            loadRutaStatsYNotificar()
            return
        }
        llamarEndpointNotificar(idViaje)
    }

    private fun llamarEndpointNotificar(idViaje: Int) {
        ApiClient.apiService.notificarLlegada(idViaje).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@ResumenViajeActivity, "Padres notificados ✓", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@ResumenViajeActivity, "Error al notificar", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Toast.makeText(this@ResumenViajeActivity, "Error de red", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadRutaStats() {
        if (driverEmail.isEmpty()) return
        ApiClient.apiService.getViajeActual(driverEmail).enqueue(object : Callback<ViajeResponse> {
            override fun onResponse(call: Call<ViajeResponse>, response: Response<ViajeResponse>) {
                if (response.isSuccessful) {
                    val viaje = response.body() ?: return
                    currentViajeId = viaje.id_viaje
                    updateUI(viaje.total, viaje.subieron, viaje.no_subieron)
                }
            }
            override fun onFailure(call: Call<ViajeResponse>, t: Throwable) {}
        })
    }

    private fun loadRutaStatsYNotificar() {
        if (driverEmail.isEmpty()) return
        ApiClient.apiService.getViajeActual(driverEmail).enqueue(object : Callback<ViajeResponse> {
            override fun onResponse(call: Call<ViajeResponse>, response: Response<ViajeResponse>) {
                if (response.isSuccessful) {
                    val viaje = response.body() ?: return
                    currentViajeId = viaje.id_viaje
                    updateUI(viaje.total, viaje.subieron, viaje.no_subieron)
                    llamarEndpointNotificar(viaje.id_viaje)
                }
            }
            override fun onFailure(call: Call<ViajeResponse>, t: Throwable) {
                Toast.makeText(this@ResumenViajeActivity, "No se pudo obtener el viaje", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateUI(total: Int, onBoard: Int, missed: Int) {
        try {
            val layoutDetalles = findViewById<android.widget.LinearLayout>(R.id.layoutDetalles) ?: return

            val row1 = layoutDetalles.getChildAt(0) as android.widget.RelativeLayout
            (row1.getChildAt(1) as TextView).text = total.toString()

            val row2 = layoutDetalles.getChildAt(2) as android.widget.RelativeLayout
            (row2.getChildAt(1) as TextView).text = onBoard.toString()

            val row3 = layoutDetalles.getChildAt(4) as android.widget.RelativeLayout
            (row3.getChildAt(1) as TextView).text = missed.toString()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
