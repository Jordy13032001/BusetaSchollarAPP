package com.example.busetaescolarapp.padre

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.busetaescolarapp.MainActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.busetaescolarapp.R
import com.example.busetaescolarapp.NavigationUtils

import android.widget.Toast
import com.example.busetaescolarapp.data.local.NotificacionEntity
import com.example.busetaescolarapp.ui.viewmodel.PadreViewModel

class NotificacionesActivity : AppCompatActivity() {
    private lateinit var recycler: RecyclerView
    private lateinit var parentEmail: String
    private lateinit var viewModel: PadreViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notificaciones)

        recycler = findViewById(R.id.recyclerNotificaciones)
        recycler.layoutManager = LinearLayoutManager(this)

        val sessionManager = com.example.busetaescolarapp.utils.SessionManager(this)
        parentEmail = sessionManager.getUserEmail() ?: ""

        viewModel = ViewModelProvider(this)[PadreViewModel::class.java]

        if (parentEmail.isNotEmpty()) {
            // Room es la fuente de la UI: se actualiza sola en cuanto llegan datos nuevos
            viewModel.notificaciones(parentEmail).observe(this) { entities ->
                recycler.adapter = NotificacionAdapter(entities.map { it.toDomain() })
            }
            // Dispara la sincronización con el backend (si falla, queda lo último guardado en Room)
            viewModel.sincronizarNotificaciones(parentEmail)
        } else {
            Toast.makeText(this, "Error de sesión", Toast.LENGTH_SHORT).show()
        }

        NavigationUtils.setupPadreBottomNavigation(this)
        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbarNotificaciones)?.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun NotificacionEntity.toDomain(): Notificacion {
        val typeEnum = try {
            TipoNotificacion.valueOf(tipo)
        } catch (e: Exception) {
            TipoNotificacion.CERCA
        }
        return Notificacion(titulo, mensaje, hora, typeEnum)
    }
}
