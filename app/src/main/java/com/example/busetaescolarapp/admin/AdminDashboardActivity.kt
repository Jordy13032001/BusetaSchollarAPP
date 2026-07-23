package com.example.busetaescolarapp.admin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.busetaescolarapp.LoginActivity
import com.example.busetaescolarapp.R
import com.example.busetaescolarapp.utils.SessionManager

class AdminDashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        val sessionManager = SessionManager(this)

        val tvName = findViewById<TextView>(R.id.tvAdminName)
        val tvEmail = findViewById<TextView>(R.id.tvAdminEmail)
        val tvPhone = findViewById<TextView>(R.id.tvAdminPhone)
        val tvRole = findViewById<TextView>(R.id.tvAdminRole)

        tvName.text = sessionManager.getUserName() ?: "Administrador"
        tvEmail.text = sessionManager.getUserEmail() ?: "Sin correo"
        val phone = sessionManager.getUserPhone()
        tvPhone.text = if (phone.isNullOrEmpty()) "Sin teléfono registrado" else phone
        
        tvRole.text = "Rol: ${sessionManager.getUserRole()?.capitalize() ?: "Admin"}"

        findViewById<Button>(R.id.btnVerSolicitudes).setOnClickListener {
            startActivity(Intent(this, AdminSolicitudesActivity::class.java))
        }

        findViewById<Button>(R.id.btnCerrarSesionAdmin).setOnClickListener {
            sessionManager.logout()
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }
}
