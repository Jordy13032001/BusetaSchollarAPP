package com.example.busetaescolarapp

import android.content.Intent
import android.os.Bundle
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.busetaescolarapp.network.ApiClient
import com.example.busetaescolarapp.network.ApiResponse
import com.example.busetaescolarapp.network.RegistroRequest
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegistroActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        val tvGoToLogin = findViewById<TextView>(R.id.tvGoToLogin)
        val btnRegister = findViewById<MaterialButton>(R.id.btnRegister)
        val etName = findViewById<TextInputEditText>(R.id.etName)
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val rgRole = findViewById<RadioGroup>(R.id.rgRole)

        tvGoToLogin.setOnClickListener {
            finish() // Volver al login
        }

        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            
            val checkedRadioButtonId = rgRole.checkedRadioButtonId
            val role = if (checkedRadioButtonId != -1) {
                findViewById<RadioButton>(checkedRadioButtonId).text.toString().lowercase()
            } else {
                "padre"
            }

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor llena todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Llamada al backend
            val request = RegistroRequest(name, email, password, role)
            ApiClient.apiService.registerUser(request).enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        Toast.makeText(this@RegistroActivity, "Registro exitoso", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@RegistroActivity, "Error al registrar", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Toast.makeText(this@RegistroActivity, "Fallo de conexión: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}
