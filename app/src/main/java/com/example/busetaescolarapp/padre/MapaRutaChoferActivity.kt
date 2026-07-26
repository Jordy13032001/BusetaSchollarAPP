package com.example.busetaescolarapp.padre

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.busetaescolarapp.R
import com.example.busetaescolarapp.network.ApiClient
import com.example.busetaescolarapp.network.EstudianteResponse
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MapaRutaChoferActivity : AppCompatActivity(), OnMapReadyCallback {

    private var mMap: GoogleMap? = null
    private var correoChofer: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mapa_ruta_chofer)

        correoChofer = intent.getStringExtra("CORREO_CHOFER") ?: ""
        val nombreChofer = intent.getStringExtra("NOMBRE_CHOFER") ?: "Chofer"

        findViewById<TextView>(R.id.tvToolbarTitle)?.text = "Ruta de $nombreChofer"
        findViewById<android.widget.ImageButton>(R.id.btnBack)?.setOnClickListener { finish() }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapRutaChofer) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(-2.9000, -79.0000), 13f))

        if (correoChofer.isNotEmpty()) {
            cargarRuta()
        }
    }

    private fun cargarRuta() {
        ApiClient.apiService.getRuta(correoChofer).enqueue(object : Callback<List<EstudianteResponse>> {
            override fun onResponse(call: Call<List<EstudianteResponse>>, response: Response<List<EstudianteResponse>>) {
                if (!response.isSuccessful) {
                    Toast.makeText(this@MapaRutaChoferActivity, "No se pudo cargar la ruta", Toast.LENGTH_SHORT).show()
                    return
                }
                val estudiantes = response.body() ?: emptyList()
                dibujarParadas(estudiantes)
            }

            override fun onFailure(call: Call<List<EstudianteResponse>>, t: Throwable) {
                Toast.makeText(this@MapaRutaChoferActivity, "Error de red", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun dibujarParadas(estudiantes: List<EstudianteResponse>) {
        if (mMap == null) return

        val puntos = mutableListOf<LatLng>()
        val boundsBuilder = LatLngBounds.Builder()

        for ((index, est) in estudiantes.withIndex()) {
            val lat = est.lat?.takeIf { it != 0.0 } ?: continue
            val lng = est.lng?.takeIf { it != 0.0 } ?: continue
            val pos = LatLng(lat, lng)
            puntos.add(pos)
            boundsBuilder.include(pos)

            mMap?.addMarker(
                MarkerOptions()
                    .position(pos)
                    .title("${index + 1}. ${est.nombre_completo}")
                    .snippet(est.direccion)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            )
        }

        val colegioPos = LatLng(-2.9065, -79.0040)
        puntos.add(colegioPos)
        boundsBuilder.include(colegioPos)

        mMap?.addMarker(
            MarkerOptions()
                .position(colegioPos)
                .title("Colegio")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        )

        if (puntos.size >= 2) {
            mMap?.addPolyline(
                PolylineOptions()
                    .addAll(puntos)
                    .width(8f)
                    .color(android.graphics.Color.parseColor("#F57C00"))
                    .geodesic(true)
            )
        }

        if (puntos.isNotEmpty()) {
            try {
                val bounds = boundsBuilder.build()
                mMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 80))
            } catch (e: Exception) {
                puntos.firstOrNull()?.let { mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(it, 13f)) }
            }
        }
    }
}
