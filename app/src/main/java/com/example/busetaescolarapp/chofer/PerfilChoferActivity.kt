package com.example.busetaescolarapp.chofer

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.busetaescolarapp.LoginActivity
import com.example.busetaescolarapp.NavigationUtils
import com.example.busetaescolarapp.R
import com.example.busetaescolarapp.utils.SessionManager

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

        findViewById<Button>(R.id.btnCerrarSesionChofer).setOnClickListener {
            sessionManager.logout()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
