package com.example.busetaescolarapp.padre

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.busetaescolarapp.R
import com.example.busetaescolarapp.network.ApiClient
import com.example.busetaescolarapp.network.ApiResponse
import com.example.busetaescolarapp.network.DriverRequest
import com.example.busetaescolarapp.utils.SessionManager
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SolicitudChoferActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_solicitud_chofer)

        val etLicencia = findViewById<TextInputEditText>(R.id.etLicencia)
        val etPlaca = findViewById<TextInputEditText>(R.id.etPlaca)
        val etModelo = findViewById<TextInputEditText>(R.id.etModelo)
        val etCapacidad = findViewById<TextInputEditText>(R.id.etCapacidad)
        val etTarifa = findViewById<TextInputEditText>(R.id.etTarifa)
        val btnEnviar = findViewById<Button>(R.id.btnEnviarSolicitud)

        val sessionManager = SessionManager(this)
        val email = sessionManager.getUserEmail() ?: ""

        btnEnviar.setOnClickListener {
            val licencia = etLicencia.text.toString().trim()
            val placa = etPlaca.text.toString().trim()
            val modelo = etModelo.text.toString().trim()
            val capacidadStr = etCapacidad.text.toString().trim()
            val tarifaStr = etTarifa.text.toString().trim()

            if (licencia.isEmpty() || placa.isEmpty() || modelo.isEmpty() || capacidadStr.isEmpty() || tarifaStr.isEmpty()) {
                Toast.makeText(this, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = DriverRequest(
                email = email,
                licencia = licencia,
                placa = placa,
                modelo = modelo,
                capacidad = capacidadStr.toIntOrNull() ?: 0,
                tarifa_mensual = tarifaStr.toDoubleOrNull() ?: 0.0
            )

            btnEnviar.isEnabled = false
            btnEnviar.text = "Enviando..."

            ApiClient.apiService.joinAsDriver(request).enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    btnEnviar.isEnabled = true
                    btnEnviar.text = "Enviar Solicitud"
                    if (response.isSuccessful) {
                        // El vehículo ya no queda aprobado al instante: pasa por el admin.
                        // El estado se sigue en la pantalla de perfil del chofer.
                        Toast.makeText(
                            this@SolicitudChoferActivity,
                            "Solicitud enviada. Un administrador debe revisarla antes de que puedas iniciar rutas.",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    } else {
                        Toast.makeText(this@SolicitudChoferActivity, "Error al registrar el vehículo. Intenta de nuevo.", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    btnEnviar.isEnabled = true
                    btnEnviar.text = "Enviar Solicitud"
                    Toast.makeText(this@SolicitudChoferActivity, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}
