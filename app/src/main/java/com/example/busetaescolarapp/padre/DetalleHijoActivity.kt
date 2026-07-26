package com.example.busetaescolarapp.padre

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.busetaescolarapp.R
import com.example.busetaescolarapp.network.ApiClient
import com.example.busetaescolarapp.network.ApiResponse
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DetalleHijoActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "id_estudiante"
        const val EXTRA_NOMBRE = "nombre_completo"
        const val EXTRA_DIRECCION = "direccion"
        const val EXTRA_CHOFER = "nombre_chofer"
        const val EXTRA_HORA = "hora_estimada"
        const val EXTRA_ESTADO = "estado"
    }

    private var idEstudiante: Int = -1
    private lateinit var tvNombre: TextView
    private lateinit var btnQuitar: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_hijo)

        idEstudiante = intent.getIntExtra(EXTRA_ID, -1)
        val nombre = intent.getStringExtra(EXTRA_NOMBRE) ?: ""
        val direccion = intent.getStringExtra(EXTRA_DIRECCION) ?: "Sin dirección"
        val chofer = intent.getStringExtra(EXTRA_CHOFER) ?: "Sin chofer asignado"
        val hora = intent.getStringExtra(EXTRA_HORA) ?: "--:--"
        val estado = intent.getStringExtra(EXTRA_ESTADO) ?: ""

        tvNombre = findViewById(R.id.tvDetalleNombre)
        btnQuitar = findViewById(R.id.btnQuitarDeRuta)

        tvNombre.text = nombre
        findViewById<TextView>(R.id.tvDetalleDireccion).text = direccion
        findViewById<TextView>(R.id.tvDetalleChofer).text = chofer
        findViewById<TextView>(R.id.tvDetalleHora).text = hora

        val (estadoTexto, estadoColor, badgeBg) = when (estado) {
            "ACEPTADO" -> Triple("En ruta ✓", "#2E7D32", "#E8F5E9")
            "PENDIENTE" -> Triple("Pendiente de aceptación", "#F57F17", "#FFF8E1")
            "RECHAZADO" -> Triple("Solicitud rechazada", "#C62828", "#FFEBEE")
            else -> Triple(estado, "#757575", "#F5F5F5")
        }
        val tvEstado = findViewById<TextView>(R.id.tvDetalleEstado)
        tvEstado.text = estadoTexto
        tvEstado.setTextColor(android.graphics.Color.parseColor(estadoColor))
        tvEstado.setBackgroundColor(android.graphics.Color.parseColor(badgeBg))

        // Mostrar botón solo si tiene chofer asignado (puede quitar de ACEPTADO o PENDIENTE)
        if (chofer != "Sin chofer asignado" && estado != "RECHAZADO" && idEstudiante != -1) {
            btnQuitar.visibility = View.VISIBLE
        }

        btnQuitar.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Quitar de la ruta")
                .setMessage("¿Seguro que quieres retirar a $nombre de la ruta? El niño quedará sin chofer asignado.")
                .setPositiveButton("Sí, quitar") { _, _ -> quitarDeRuta() }
                .setNegativeButton("Cancelar", null)
                .show()
        }


    }

    private fun quitarDeRuta() {
        ApiClient.apiService.quitarEstudianteDeRuta(idEstudiante)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@DetalleHijoActivity, "${tvNombre.text} fue retirado de la ruta", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        Toast.makeText(this@DetalleHijoActivity, "Error al retirar. Intenta de nuevo.", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Toast.makeText(this@DetalleHijoActivity, "Sin conexión. Intenta de nuevo.", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
