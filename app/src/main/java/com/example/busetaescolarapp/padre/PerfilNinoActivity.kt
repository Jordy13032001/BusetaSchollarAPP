package com.example.busetaescolarapp.padre

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.busetaescolarapp.LoginActivity
import com.example.busetaescolarapp.NavigationUtils
import com.example.busetaescolarapp.R
import com.example.busetaescolarapp.chofer.ChoferHomeActivity
import com.google.android.material.button.MaterialButton

class PerfilNinoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil_nino)

        NavigationUtils.setupPadreBottomNavigation(this)

        val sessionManager = com.example.busetaescolarapp.utils.SessionManager(this)
        val name = sessionManager.getUserName() ?: "Usuario"
        val email = sessionManager.getUserEmail() ?: ""
        val phone = sessionManager.getUserPhone() ?: ""

        findViewById<TextView>(R.id.tvParentName).text = name
        findViewById<TextView>(R.id.tvParentEmail).text = email
        findViewById<TextView>(R.id.tvParentPhone).text = if (phone.isNotEmpty()) phone else ""
        findViewById<TextView>(R.id.tvParentEmailCard).text = email.ifEmpty { "—" }
        findViewById<TextView>(R.id.tvParentPhoneCard).text = phone.ifEmpty { "—" }

        findViewById<Button>(R.id.btnCerrarSesion).setOnClickListener {
            sessionManager.logout()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        findViewById<Button>(R.id.btnAddChild)?.setOnClickListener {
            startActivity(Intent(this, AddChildActivity::class.java))
        }

        val btnUnirseConductor = findViewById<MaterialButton>(R.id.btnUnirseConductor)
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
}
