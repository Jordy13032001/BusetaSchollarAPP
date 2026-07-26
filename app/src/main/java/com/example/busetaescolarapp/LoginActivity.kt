package com.example.busetaescolarapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.busetaescolarapp.chofer.ChoferHomeActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etUsuario = findViewById<TextInputEditText>(R.id.etUsuario)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnIngresar = findViewById<MaterialButton>(R.id.btnIngresar)
        val tvRegistro = findViewById<android.widget.TextView>(R.id.tvRegistro)

        tvRegistro.setOnClickListener {
            startActivity(Intent(this, RegistroActivity::class.java))
        }

        btnIngresar.setOnClickListener {
            val email = etUsuario.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor ingrese correo y contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = com.example.busetaescolarapp.network.LoginRequest(email, password)
            com.example.busetaescolarapp.network.ApiClient.apiService.login(request)
                .enqueue(object : retrofit2.Callback<com.example.busetaescolarapp.network.ApiResponse> {
                    override fun onResponse(
                        call: retrofit2.Call<com.example.busetaescolarapp.network.ApiResponse>,
                        response: retrofit2.Response<com.example.busetaescolarapp.network.ApiResponse>
                    ) {
                        if (response.isSuccessful && response.body() != null) {
                            val user = response.body()?.user
                            if (user != null) {
                                val sessionManager = com.example.busetaescolarapp.utils.SessionManager(this@LoginActivity)
                                sessionManager.saveUserSession(user.email, user.role, user.name, user.phone, user.roles)

                                when (user.role) {
                                    "admin" -> startActivity(Intent(this@LoginActivity, com.example.busetaescolarapp.admin.AdminDashboardActivity::class.java))
                                    "chofer" -> startActivity(Intent(this@LoginActivity, ChoferHomeActivity::class.java))
                                    else -> startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                                }
                                finish()
                            }
                        } else {
                            Toast.makeText(this@LoginActivity, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(
                        call: retrofit2.Call<com.example.busetaescolarapp.network.ApiResponse>,
                        t: Throwable
                    ) {
                        Toast.makeText(this@LoginActivity, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }
}
