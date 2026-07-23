package com.example.busetaescolarapp.chofer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.content.Intent
import android.net.Uri
import com.example.busetaescolarapp.NavigationUtils
import com.example.busetaescolarapp.R

class IncidenteChoferActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incidente_chofer)
        NavigationUtils.setupChoferBottomNavigation(this)

        val btnWhatsapp = findViewById<Button>(R.id.btnWhatsapp)
        btnWhatsapp?.setOnClickListener {
            val numero = "593962946714" // Nuevo número destino solicitado por el usuario
            val mensaje = "Alerta: Se ha reportado un incidente (retraso por accidente) en la ruta de la buseta escolar."
            val url = "https://api.whatsapp.com/send?phone=$numero&text=${Uri.encode(mensaje)}"
            
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        }
    }
}
