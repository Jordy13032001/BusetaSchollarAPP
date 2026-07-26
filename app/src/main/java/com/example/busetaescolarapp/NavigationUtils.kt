package com.example.busetaescolarapp

import android.app.Activity
import android.content.Intent
import android.widget.LinearLayout
import com.example.busetaescolarapp.padre.*

object NavigationUtils {

    fun setupPadreBottomNavigation(activity: Activity) {
        val bottomNav = activity.findViewById<LinearLayout>(R.id.bottomNavigation) ?: return

        // Aseguramos que existan 5 hijos
        if (bottomNav.childCount == 5) {
            val navInicio = bottomNav.getChildAt(0)
            val navRuta = bottomNav.getChildAt(1)
            val navNotificaciones = bottomNav.getChildAt(2)
            val navIncidente = bottomNav.getChildAt(3)
            val navPerfil = bottomNav.getChildAt(4)

            navInicio.setOnClickListener { navigateTo(activity, MainActivity::class.java) }
            navRuta.setOnClickListener { navigateTo(activity, RutaCompletaActivity::class.java) }
            navNotificaciones.setOnClickListener { navigateTo(activity, NotificacionesActivity::class.java) }
            navIncidente.setOnClickListener { navigateTo(activity, IncidenteActivity::class.java) }
            navPerfil.setOnClickListener { navigateTo(activity, PerfilNinoActivity::class.java) }
        }
    }

    fun setupChoferBottomNavigation(activity: Activity) {
        val bottomNav = activity.findViewById<LinearLayout>(R.id.bottomNavigation) ?: return

        if (bottomNav.childCount == 5) {
            val navInicio = bottomNav.getChildAt(0)
            val navAsistencia = bottomNav.getChildAt(1)
            val navMapa = bottomNav.getChildAt(2)
            val navIncidente = bottomNav.getChildAt(3)
            val navResumen = bottomNav.getChildAt(4)

            navInicio.setOnClickListener { navigateTo(activity, com.example.busetaescolarapp.chofer.ChoferHomeActivity::class.java) }
            navAsistencia.setOnClickListener { navigateTo(activity, com.example.busetaescolarapp.chofer.AsistenciaActivity::class.java) }
            navMapa.setOnClickListener {
                if (com.example.busetaescolarapp.chofer.DriverTracker.isTracking()) {
                    navigateTo(activity, com.example.busetaescolarapp.chofer.MapaChoferActivity::class.java)
                } else {
                    android.widget.Toast.makeText(activity, "Inicia la ruta primero", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            navIncidente.setOnClickListener { navigateTo(activity, com.example.busetaescolarapp.chofer.IncidenteChoferActivity::class.java) }
            navResumen.setOnClickListener { navigateTo(activity, com.example.busetaescolarapp.chofer.ResumenViajeActivity::class.java) }
        }
    }

    private fun navigateTo(currentActivity: Activity, targetActivity: Class<*>) {
        if (currentActivity::class.java == targetActivity) return // No abrir si ya estamos ahi

        val intent = Intent(currentActivity, targetActivity)
        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        currentActivity.startActivity(intent)
    }
}
