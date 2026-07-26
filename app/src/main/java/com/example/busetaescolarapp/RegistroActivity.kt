package com.example.busetaescolarapp

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.busetaescolarapp.network.ApiClient
import com.example.busetaescolarapp.network.ApiResponse
import com.example.busetaescolarapp.network.RegistroRequest
import com.google.gson.Gson
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegistroActivity : AppCompatActivity() {

    private var selectedRole: String = "padre"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        val layoutRol = findViewById<View>(R.id.layoutRolRegistro)
        val layoutForm = findViewById<View>(R.id.layoutFormRegistro)
        val tvRolSeleccionado = findViewById<TextView>(R.id.tvRolSeleccionadoRegistro)
        val tvCambiarTipo = findViewById<TextView>(R.id.tvCambiarTipo)
        val tvGoToLogin = findViewById<TextView>(R.id.tvGoToLogin)
        val btnRegister = findViewById<MaterialButton>(R.id.btnRegister)
        val etName = findViewById<TextInputEditText>(R.id.etName)
        val etPhone = findViewById<TextInputEditText>(R.id.etPhone)
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)

        tvGoToLogin.setOnClickListener { finish() }

        findViewById<MaterialButton>(R.id.btnRolPadreRegistro).setOnClickListener {
            selectedRole = "padre"
            tvRolSeleccionado.text = "Registrándome como Padre/Madre"
            layoutRol.visibility = View.GONE
            layoutForm.visibility = View.VISIBLE
        }

        findViewById<MaterialButton>(R.id.btnRolChoferRegistro).setOnClickListener {
            selectedRole = "chofer"
            tvRolSeleccionado.text = "Registrándome como Chofer"
            layoutRol.visibility = View.GONE
            layoutForm.visibility = View.VISIBLE
        }

        tvCambiarTipo.setOnClickListener {
            layoutForm.visibility = View.GONE
            layoutRol.visibility = View.VISIBLE
        }

        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (name.isEmpty() || phone.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor llena todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnRegister.isEnabled = false
            btnRegister.text = "Creando cuenta..."

            val request = RegistroRequest(name, email, phone, password, selectedRole)
            ApiClient.apiService.registerUser(request).enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    btnRegister.isEnabled = true
                    btnRegister.text = "Crear mi cuenta"
                    if (response.isSuccessful && response.body() != null) {
                        Toast.makeText(
                            this@RegistroActivity,
                            "¡Cuenta creada! Ya puedes iniciar sesión.",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    } else {
                        val errorMsg = try {
                            val body = response.errorBody()?.string()
                            Gson().fromJson(body, ApiResponse::class.java)?.error ?: "Error al registrar"
                        } catch (_: Exception) { "Error al registrar" }
                        Toast.makeText(this@RegistroActivity, errorMsg, Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    btnRegister.isEnabled = true
                    btnRegister.text = "Crear mi cuenta"
                    Toast.makeText(this@RegistroActivity, "Sin conexión: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}
