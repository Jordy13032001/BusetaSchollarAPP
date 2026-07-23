package com.example.busetaescolarapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import android.widget.LinearLayout
import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.busetaescolarapp.padre.*
import com.google.android.gms.maps.model.LatLngBounds

// 1. Cambiamos ComponentActivity por AppCompatActivity
// 2. Implementamos OnMapReadyCallback para manejar el mapa
class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var viewModel: com.example.busetaescolarapp.ui.viewmodel.PadreViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 3. QUITAMOS el setContent de Compose y usamos el XML
        setContentView(R.layout.activity_padre_home)

        // 3.5 Cargar datos reales
        val sessionManager = com.example.busetaescolarapp.utils.SessionManager(this)
        val name = sessionManager.getUserName() ?: "Padre"
        val email = sessionManager.getUserEmail() ?: ""

        val tvGreeting = findViewById<android.widget.TextView>(R.id.tvGreeting)
        tvGreeting?.text = "¡Hola, $name!"

        val rvNinosPadre = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvNinosPadre)
        rvNinosPadre.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)

        viewModel = androidx.lifecycle.ViewModelProvider(this).get(com.example.busetaescolarapp.ui.viewmodel.PadreViewModel::class.java)

        viewModel.children.observe(this) { children ->
            rvNinosPadre.adapter = NinoPadreAdapter(children)
        }

        if (email.isNotEmpty()) {
            viewModel.fetchChildren(email)
        }

        // 4. Inicializamos el mapa
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        
        // 5. Configurar Botones de Navegacion Inferior
        setupBottomNavigation()
    }
    
    private fun setupBottomNavigation() {
        NavigationUtils.setupPadreBottomNavigation(this)
    }

    // 5. Esta función se ejecuta cuando el mapa está listo
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Pedir permisos de ubicación
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 102)
        } else {
            mMap.isMyLocationEnabled = true
        }

        // Ejemplo: Centrar el mapa en Cuenca, Ecuador (Simulando la escuela)
        val cuenca = LatLng(-2.9001, -79.0001)
        
        // Bloquear el mapa para que no puedan salir de Cuenca
        val limiteCuenca = LatLngBounds(
            LatLng(-2.9300, -79.0500), // Sur-Oeste
            LatLng(-2.8700, -78.9500)  // Nor-Este
        )
        mMap.setLatLngBoundsForCameraTarget(limiteCuenca)
        mMap.setMinZoomPreference(12.0f) // No permitir alejar demasiado
        
        mMap.addMarker(MarkerOptions().position(cuenca).title("Colegio San José"))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(cuenca, 15f))

        // Opcional: Quitar botones de zoom feos para que se vea más limpio
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isCompassEnabled = true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 102 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mMap.isMyLocationEnabled = true
            }
        }
    }
}

class NinoPadreAdapter(
    private val children: List<com.example.busetaescolarapp.network.EstudianteResponse>
) : androidx.recyclerview.widget.RecyclerView.Adapter<NinoPadreAdapter.ViewHolder>() {

    class ViewHolder(view: android.view.View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
        val tvNombreNino: android.widget.TextView = view.findViewById(R.id.tvNombreNino)
        val tvChofer: android.widget.TextView = view.findViewById(R.id.tvChofer)
        val tvDireccion: android.widget.TextView = view.findViewById(R.id.tvDireccion)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context).inflate(R.layout.item_nino_padre, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val child = children[position]
        holder.tvNombreNino.text = child.nombre_completo
        holder.tvChofer.text = "Chofer: ${if (child.correo_chofer.isNotEmpty()) child.correo_chofer else "No asignado"}"
        holder.tvDireccion.text = "Parada: ${child.direccion}"
    }

    override fun getItemCount() = children.size
}