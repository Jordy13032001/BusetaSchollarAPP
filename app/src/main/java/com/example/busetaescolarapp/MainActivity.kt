package com.example.busetaescolarapp

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.busetaescolarapp.padre.DetalleHijoActivity
import com.example.busetaescolarapp.padre.NinoPadreAdapter

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: com.example.busetaescolarapp.ui.viewmodel.PadreViewModel
    private var rvNinosPadre: RecyclerView? = null
    private var email: String = ""

    private val detalleHijoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) viewModel.fetchChildren(email)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_padre_home)

        val sessionManager = com.example.busetaescolarapp.utils.SessionManager(this)
        val name = sessionManager.getUserName() ?: "Padre"
        email = sessionManager.getUserEmail() ?: ""

        findViewById<TextView>(R.id.tvGreeting)?.text = "¡Hola, $name!"

        rvNinosPadre = findViewById(R.id.rvNinosPadre)
        rvNinosPadre?.layoutManager = LinearLayoutManager(this)

        viewModel = androidx.lifecycle.ViewModelProvider(this)
            .get(com.example.busetaescolarapp.ui.viewmodel.PadreViewModel::class.java)

        viewModel.children.observe(this) { children ->
            rvNinosPadre?.adapter = NinoPadreAdapter(children) { child ->
                val intent = Intent(this, DetalleHijoActivity::class.java).apply {
                    putExtra(DetalleHijoActivity.EXTRA_ID, child.id_estudiante)
                    putExtra(DetalleHijoActivity.EXTRA_NOMBRE, child.nombre_completo)
                    putExtra(DetalleHijoActivity.EXTRA_DIRECCION, child.direccion)
                    putExtra(DetalleHijoActivity.EXTRA_CHOFER, child.nombre_chofer ?: child.correo_chofer)
                    putExtra(DetalleHijoActivity.EXTRA_HORA, child.hora_estimada ?: "--:--")
                    putExtra(DetalleHijoActivity.EXTRA_ESTADO, child.estado ?: "")
                }
                detalleHijoLauncher.launch(intent)
            }
        }

        if (email.isNotEmpty()) viewModel.fetchChildren(email)

        NavigationUtils.setupPadreBottomNavigation(this)
    }

    override fun onResume() {
        super.onResume()
        if (email.isNotEmpty()) viewModel.fetchChildren(email)
    }
}
