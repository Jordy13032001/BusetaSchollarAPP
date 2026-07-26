package com.example.busetaescolarapp.padre

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.busetaescolarapp.LoginActivity
import com.example.busetaescolarapp.NavigationUtils
import com.example.busetaescolarapp.R
import com.example.busetaescolarapp.chofer.ChoferHomeActivity
import com.example.busetaescolarapp.network.ApiClient
import com.example.busetaescolarapp.network.ApiResponse
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import android.widget.TextView
import com.example.busetaescolarapp.network.EstudianteResponse

class PerfilNinoActivity : AppCompatActivity() {

    private lateinit var tvToolbarTitle: TextView
    private lateinit var tvParentName: TextView
    private lateinit var tvParentEmail: TextView
    private lateinit var tvChildName: TextView
    private lateinit var tvDriverName: TextView
    private lateinit var tvDriverEmail: TextView
    private lateinit var btnQuitarDeRuta: MaterialButton
    private var idEstudianteConChofer: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil_nino)
        NavigationUtils.setupPadreBottomNavigation(this)

        tvToolbarTitle = findViewById(R.id.tvToolbarTitle)
        tvParentName = findViewById(R.id.tvParentName)
        tvParentEmail = findViewById(R.id.tvParentEmail)
        tvChildName = findViewById(R.id.tvChildName)
        tvDriverName = findViewById(R.id.tvDriverName)
        tvDriverEmail = findViewById(R.id.tvDriverEmail)
        btnQuitarDeRuta = findViewById(R.id.btnQuitarDeRuta)

        val sessionManager = com.example.busetaescolarapp.utils.SessionManager(this)
        val name = sessionManager.getUserName() ?: "Usuario"
        val email = sessionManager.getUserEmail() ?: ""

        tvParentName.text = name
        tvParentEmail.text = email

        if (email.isNotEmpty()) {
            fetchChildData(email)
        }

        val btnCerrarSesion = findViewById<Button>(R.id.btnCerrarSesion)
        btnCerrarSesion.setOnClickListener {
            sessionManager.logout()
            val intent = Intent(this@PerfilNinoActivity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
        
        val btnAddChild = findViewById<Button>(R.id.btnAddChild)
        btnAddChild?.setOnClickListener {
            startActivity(Intent(this, AddChildActivity::class.java))
        }

        btnQuitarDeRuta.setOnClickListener {
            val id = idEstudianteConChofer ?: return@setOnClickListener
            AlertDialog.Builder(this)
                .setTitle("Quitar de la ruta")
                .setMessage("¿Seguro que quieres retirar a ${tvChildName.text} de la ruta? El niño quedará sin chofer asignado y tendrás que solicitar uno nuevo.")
                .setPositiveButton("Sí, quitar") { _, _ -> quitarDeRuta(id) }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        val btnUnirseConductor = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnUnirseConductor)
        if (sessionManager.hasRole("chofer")) {
            btnUnirseConductor?.text = "Cambiar a Modo Chofer"
            btnUnirseConductor?.setOnClickListener {
                sessionManager.setCurrentRole("chofer")
                val intent = Intent(this, ChoferHomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        } else {
            btnUnirseConductor?.setOnClickListener {
                startActivity(Intent(this, SolicitudChoferActivity::class.java))
            }
        }
    }

    private fun fetchChildData(parentEmail: String) {
        ApiClient.apiService.getParentChildren(parentEmail).enqueue(object : Callback<List<EstudianteResponse>> {
            override fun onResponse(call: Call<List<EstudianteResponse>>, response: Response<List<EstudianteResponse>>) {
                if (response.isSuccessful) {
                    val children = response.body() ?: emptyList()
                    if (children.isNotEmpty()) {
                        val child = children[0]
                        tvChildName.text = child.nombre_completo
                        val tieneChofer = !child.correo_chofer.isNullOrEmpty()
                        tvDriverEmail.text = if (tieneChofer) child.correo_chofer else "Sin chofer asignado"
                        tvDriverName.text = if (tieneChofer) "Asignado" else "N/A"
                        if (tieneChofer) {
                            idEstudianteConChofer = child.id_estudiante
                            btnQuitarDeRuta.visibility = View.VISIBLE
                        } else {
                            idEstudianteConChofer = null
                            btnQuitarDeRuta.visibility = View.GONE
                        }
                    } else {
                        tvChildName.text = "Sin hijos registrados"
                        tvDriverName.text = "N/A"
                        tvDriverEmail.text = "N/A"
                        btnQuitarDeRuta.visibility = View.GONE
                    }
                }
            }
            override fun onFailure(call: Call<List<EstudianteResponse>>, t: Throwable) {}
        })
    }

    private fun quitarDeRuta(idEstudiante: Int) {
        ApiClient.apiService.quitarEstudianteDeRuta(idEstudiante)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@PerfilNinoActivity, "Niño retirado de la ruta", Toast.LENGTH_SHORT).show()
                        tvDriverEmail.text = "Sin chofer asignado"
                        tvDriverName.text = "N/A"
                        btnQuitarDeRuta.visibility = View.GONE
                        idEstudianteConChofer = null
                    } else {
                        Toast.makeText(this@PerfilNinoActivity, "Error al retirar. Intenta de nuevo.", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Toast.makeText(this@PerfilNinoActivity, "Sin conexión. Intenta de nuevo.", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
