package com.example.busetaescolarapp.padre

import android.app.Activity
import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.busetaescolarapp.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import java.util.Locale

class SelectLocationActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var btnConfirmLocation: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_location)

        btnConfirmLocation = findViewById(R.id.btnConfirmLocation)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapSelection) as SupportMapFragment
        mapFragment.getMapAsync(this)

        btnConfirmLocation.setOnClickListener {
            if (::mMap.isInitialized) {
                // Obtener el centro del mapa actualmente visible
                val centerLatLng = mMap.cameraPosition.target
                getAddressFromLatLng(centerLatLng)
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Centrar por defecto en Cuenca
        val cuenca = LatLng(-2.9001, -79.0001)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(cuenca, 15f))
    }

    private fun getAddressFromLatLng(latLng: LatLng) {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            
            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                val addressText = address.thoroughfare ?: address.featureName ?: "Dirección seleccionada"
                returnResult(addressText, latLng.latitude, latLng.longitude)
            } else {
                Toast.makeText(this, "No se pudo obtener la dirección", Toast.LENGTH_SHORT).show()
                returnResult("${latLng.latitude}, ${latLng.longitude}", latLng.latitude, latLng.longitude)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to coordinates
            returnResult("${latLng.latitude}, ${latLng.longitude}", latLng.latitude, latLng.longitude)
        }
    }

    private fun returnResult(address: String, lat: Double, lng: Double) {
        val resultIntent = Intent()
        resultIntent.putExtra("ADDRESS", address)
        resultIntent.putExtra("LAT", lat)
        resultIntent.putExtra("LNG", lng)
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
}
