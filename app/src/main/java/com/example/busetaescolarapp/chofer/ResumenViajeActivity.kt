package com.example.busetaescolarapp.chofer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.busetaescolarapp.LoginActivity
import com.example.busetaescolarapp.NavigationUtils
import com.example.busetaescolarapp.R
import com.example.busetaescolarapp.network.ApiClient
import com.example.busetaescolarapp.network.ViajeResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ResumenViajeActivity : AppCompatActivity() {

    private lateinit var tvTotalKids: TextView
    private lateinit var tvKidsOnBoard: TextView
    private lateinit var tvKidsMissed: TextView
    private var driverEmail: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resumen_viaje)
        NavigationUtils.setupChoferBottomNavigation(this)

        val sessionManager = com.example.busetaescolarapp.utils.SessionManager(this)
        driverEmail = sessionManager.getUserEmail() ?: ""

        val btnCerrarSesion = findViewById<Button>(R.id.btnCerrarSesionChofer)
        btnCerrarSesion?.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
        val btnFinalizarTotal = findViewById<androidx.cardview.widget.CardView>(R.id.btnFinalizarTotal)
        btnFinalizarTotal?.setOnClickListener {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            val fechaCelular = sdf.format(java.util.Date())
            
            val request = com.example.busetaescolarapp.network.FinalizarRequest(fechaCelular)
            ApiClient.apiService.finalizarViaje(driverEmail, request).enqueue(object : Callback<ViajeResponse> {
                override fun onResponse(call: Call<ViajeResponse>, response: Response<ViajeResponse>) {
                    if (response.isSuccessful) {
                        android.widget.Toast.makeText(this@ResumenViajeActivity, "Ruta guardada el $fechaCelular", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
                override fun onFailure(call: Call<ViajeResponse>, t: Throwable) {
                    android.widget.Toast.makeText(this@ResumenViajeActivity, "Error al guardar", android.widget.Toast.LENGTH_SHORT).show()
                }
            })
        }
        
        loadRutaStats()
    }

    private fun loadRutaStats() {
        if (driverEmail.isEmpty()) return

        ApiClient.apiService.getViajeActual(driverEmail).enqueue(object : Callback<ViajeResponse> {
            override fun onResponse(call: Call<ViajeResponse>, response: Response<ViajeResponse>) {
                if (response.isSuccessful) {
                    val viaje = response.body() ?: return
                    updateUI(viaje.total, viaje.subieron, viaje.no_subieron)
                }
            }
            override fun onFailure(call: Call<ViajeResponse>, t: Throwable) {}
        })
    }
    
    private fun updateUI(total: Int, onBoard: Int, missed: Int) {
        // Encontraremos los textviews por nombre buscando dentro del layout
        // ya que la estructura es estática
        try {
            val layoutDetalles = findViewById<android.widget.LinearLayout>(R.id.layoutDetalles) ?: return
            
            // Fila 1: Total
            val row1 = layoutDetalles.getChildAt(0) as android.widget.RelativeLayout
            val tvTot = row1.getChildAt(1) as TextView
            tvTot.text = total.toString()
            
            // Fila 2: Subieron
            val row2 = layoutDetalles.getChildAt(2) as android.widget.RelativeLayout
            val tvSub = row2.getChildAt(1) as TextView
            tvSub.text = onBoard.toString()
            
            // Fila 3: No subieron
            val row3 = layoutDetalles.getChildAt(4) as android.widget.RelativeLayout
            val tvNoSub = row3.getChildAt(1) as TextView
            tvNoSub.text = missed.toString()
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
