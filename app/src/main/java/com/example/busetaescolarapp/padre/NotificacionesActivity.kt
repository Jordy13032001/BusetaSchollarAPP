package com.example.busetaescolarapp.padre

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.busetaescolarapp.MainActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.busetaescolarapp.R
import com.example.busetaescolarapp.NavigationUtils

import android.widget.Toast
import com.example.busetaescolarapp.network.ApiClient
import com.example.busetaescolarapp.network.NotificationResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NotificacionesActivity : AppCompatActivity() {
    private lateinit var recycler: RecyclerView
    private lateinit var parentEmail: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notificaciones)

        recycler = findViewById(R.id.recyclerNotificaciones)
        recycler.layoutManager = LinearLayoutManager(this)
        
        val sessionManager = com.example.busetaescolarapp.utils.SessionManager(this)
        parentEmail = sessionManager.getUserEmail() ?: ""

        if (parentEmail.isNotEmpty()) {
            fetchNotifications()
        } else {
            Toast.makeText(this, "Error de sesión", Toast.LENGTH_SHORT).show()
        }

        NavigationUtils.setupPadreBottomNavigation(this)
        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbarNotificaciones)?.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun fetchNotifications() {
        ApiClient.apiService.getNotifications(parentEmail).enqueue(object : Callback<List<NotificationResponse>> {
            override fun onResponse(call: Call<List<NotificationResponse>>, response: Response<List<NotificationResponse>>) {
                if (response.isSuccessful) {
                    val notifications = response.body() ?: emptyList()
                    val mappedList = notifications.map {
                        val typeEnum = try {
                            TipoNotificacion.valueOf(it.type)
                        } catch (e: Exception) {
                            TipoNotificacion.CERCA
                        }
                        Notificacion(it.title, it.message, it.timestamp, typeEnum)
                    }
                    recycler.adapter = NotificacionAdapter(mappedList)
                }
            }
            override fun onFailure(call: Call<List<NotificationResponse>>, t: Throwable) {
                Toast.makeText(this@NotificacionesActivity, "Error de red", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
