package com.example.busetaescolarapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.busetaescolarapp.padre.NinoPadreAdapter

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: com.example.busetaescolarapp.ui.viewmodel.PadreViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_padre_home)

        val sessionManager = com.example.busetaescolarapp.utils.SessionManager(this)
        val name = sessionManager.getUserName() ?: "Padre"
        val email = sessionManager.getUserEmail() ?: ""

        findViewById<TextView>(R.id.tvGreeting)?.text = "¡Hola, $name!"

        val rvNinosPadre = findViewById<RecyclerView>(R.id.rvNinosPadre)
        rvNinosPadre?.layoutManager = LinearLayoutManager(this)

        viewModel = androidx.lifecycle.ViewModelProvider(this)
            .get(com.example.busetaescolarapp.ui.viewmodel.PadreViewModel::class.java)

        viewModel.children.observe(this) { children ->
            rvNinosPadre?.adapter = NinoPadreAdapter(children)
        }

        if (email.isNotEmpty()) {
            viewModel.fetchChildren(email)
        }

        NavigationUtils.setupPadreBottomNavigation(this)
    }

    override fun onResume() {
        super.onResume()
        val email = com.example.busetaescolarapp.utils.SessionManager(this).getUserEmail() ?: ""
        if (email.isNotEmpty()) {
            viewModel.fetchChildren(email)
        }
    }
}
