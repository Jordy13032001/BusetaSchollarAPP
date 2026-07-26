package com.example.busetaescolarapp.chofer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.busetaescolarapp.LoginActivity
import com.example.busetaescolarapp.NavigationUtils
import com.example.busetaescolarapp.R
import com.example.busetaescolarapp.network.ApiClient
import com.example.busetaescolarapp.network.EstadoVehiculoResponse
import com.example.busetaescolarapp.padre.SolicitudChoferActivity
import com.example.busetaescolarapp.utils.SessionManager
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PerfilChoferActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil_chofer)

        val sessionManager = SessionManager(this)
        val name = sessionManager.getUserName() ?: "Conductor"
        val email = sessionManager.getUserEmail() ?: ""

        findViewById<TextView>(R.id.tvChoferName).text = name
        findViewById<TextView>(R.id.tvChoferEmail).text = email
        findViewById<TextView>(R.id.tvChoferNameCard).text = name
        findViewById<TextView>(R.id.tvChoferEmailCard).text = email

        NavigationUtils.setupChoferBottomNavigation(this)

        cargarEstadoVehiculo(email)

        findViewById<Button>(R.id.btnCerrarSesionChofer).setOnClickListener {
            sessionManager.logout()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        val email = SessionManager(this).getUserEmail() ?: return
        cargarEstadoVehiculo(email)
    }

    private fun cargarEstadoVehiculo(email: String) {
        val card = findViewById<CardView>(R.id.cardVehiculo)
        val tvTitulo = findViewById<TextView>(R.id.tvVehiculoTitulo)
        val tvDetalle = findViewById<TextView>(R.id.tvVehiculoDetalle)
        val btnCompletar = findViewById<MaterialButton>(R.id.btnCompletarVehiculo)

        ApiClient.apiService.getEstadoVehiculo(email)
            .enqueue(object : Callback<EstadoVehiculoResponse> {
                override fun onResponse(call: Call<EstadoVehiculoResponse>, response: Response<EstadoVehiculoResponse>) {
                    val body = response.body() ?: return
                    when (body.estado) {
                        "APROBADO" -> {
                            card.setCardBackgroundColor(android.graphics.Color.parseColor("#E8F5E9"))
                            tvTitulo.text = "Vehículo aprobado ✓"
                            tvTitulo.setTextColor(android.graphics.Color.parseColor("#2E7D32"))
                            tvDetalle.text = "${body.modelo} · Placa: ${body.placa} · ${body.capacidad} pasajeros"
                            btnCompletar.visibility = View.GONE
                        }
                        "PENDIENTE" -> {
                            card.setCardBackgroundColor(android.graphics.Color.parseColor("#FFF8E1"))
                            tvTitulo.text = "Solicitud en revisión"
                            tvTitulo.setTextColor(android.graphics.Color.parseColor("#F57F17"))
                            tvDetalle.text = "Un administrador está revisando los datos de tu vehículo."
                            btnCompletar.visibility = View.GONE
                        }
                        "RECHAZADA" -> {
                            card.setCardBackgroundColor(android.graphics.Color.parseColor("#FFEBEE"))
                            tvTitulo.text = "Solicitud rechazada"
                            tvTitulo.setTextColor(android.graphics.Color.parseColor("#C62828"))
                            tvDetalle.text = "Tu solicitud fue rechazada. Puedes enviar una nueva."
                            btnCompletar.text = "Enviar nueva solicitud"
                            btnCompletar.visibility = View.VISIBLE
                            btnCompletar.setOnClickListener {
                                startActivity(Intent(this@PerfilChoferActivity, SolicitudChoferActivity::class.java))
                            }
                        }
                        else -> {
                            card.setCardBackgroundColor(android.graphics.Color.parseColor("#FFF8E1"))
                            tvTitulo.text = "Completa tu perfil de conductor"
                            tvTitulo.setTextColor(android.graphics.Color.parseColor("#E65100"))
                            tvDetalle.text = "Registra los datos de tu vehículo para empezar a operar."
                            btnCompletar.text = "Registrar mi vehículo"
                            btnCompletar.visibility = View.VISIBLE
                            btnCompletar.setOnClickListener {
                                startActivity(Intent(this@PerfilChoferActivity, SolicitudChoferActivity::class.java))
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<EstadoVehiculoResponse>, t: Throwable) {
                    tvDetalle.text = "No se pudo cargar el estado del vehículo."
                }
            })
    }
}
