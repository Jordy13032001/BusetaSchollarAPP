package com.example.busetaescolarapp.chofer

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.busetaescolarapp.R

import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.busetaescolarapp.network.ApiClient
import com.example.busetaescolarapp.network.AsistenciaRequest
import com.example.busetaescolarapp.network.AsistenciaResponse
import com.example.busetaescolarapp.network.EstudianteResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AsistenciaActivity : AppCompatActivity() {

    private lateinit var rvAsistenciaDetail: RecyclerView
    private lateinit var tvAttendanceCount: TextView
    private lateinit var btnFilterAll: Button
    private lateinit var btnFilterPresent: Button
    private lateinit var btnFilterAbsent: Button
    private var driverEmail: String = ""
    private var childrenList = emptyList<EstudianteResponse>()
    private val checkedChildren = mutableSetOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asistencia)

        tvAttendanceCount = findViewById(R.id.tvAttendanceCount)
        rvAsistenciaDetail = findViewById(R.id.rvAsistenciaDetail)
        rvAsistenciaDetail.layoutManager = LinearLayoutManager(this)

        val btnGuardar = findViewById<Button>(R.id.btnGuardar)

        btnFilterAll = findViewById(R.id.btnFilterAll)
        btnFilterPresent = findViewById(R.id.btnFilterPresent)
        btnFilterAbsent = findViewById(R.id.btnFilterAbsent)

        fun updateFilterButtons(activeButton: Button) {
            val inactiveColor = android.graphics.Color.parseColor("#E0E0E0")
            val inactiveText = android.graphics.Color.parseColor("#424242")
            val activeColor = android.graphics.Color.parseColor("#F57F17")
            val activeText = android.graphics.Color.WHITE

            btnFilterAll.backgroundTintList = android.content.res.ColorStateList.valueOf(inactiveColor)
            btnFilterAll.setTextColor(inactiveText)
            btnFilterPresent.backgroundTintList = android.content.res.ColorStateList.valueOf(inactiveColor)
            btnFilterPresent.setTextColor(inactiveText)
            btnFilterAbsent.backgroundTintList = android.content.res.ColorStateList.valueOf(inactiveColor)
            btnFilterAbsent.setTextColor(inactiveText)

            activeButton.backgroundTintList = android.content.res.ColorStateList.valueOf(activeColor)
            activeButton.setTextColor(activeText)
        }

        fun applyFilter(filter: String) {
            val filteredList = when (filter) {
                "PRESENT" -> childrenList.filter { checkedChildren.contains(it.id_estudiante) }
                "ABSENT" -> childrenList.filter { !checkedChildren.contains(it.id_estudiante) }
                else -> childrenList
            }
            val adapter = AsistenciaAdapter(filteredList, checkedChildren, isReadOnly = false, showCheckbox = false) { child, checked ->
                if (checked) checkedChildren.add(child.id_estudiante) else checkedChildren.remove(child.id_estudiante)
                tvAttendanceCount.text = "${checkedChildren.size} / ${childrenList.size}"
            }
            rvAsistenciaDetail.adapter = adapter
        }

        btnFilterAll.setOnClickListener {
            updateFilterButtons(btnFilterAll)
            applyFilter("ALL")
        }

        btnFilterPresent.setOnClickListener {
            updateFilterButtons(btnFilterPresent)
            applyFilter("PRESENT")
        }

        btnFilterAbsent.setOnClickListener {
            updateFilterButtons(btnFilterAbsent)
            applyFilter("ABSENT")
        }

        val sessionManager = com.example.busetaescolarapp.utils.SessionManager(this)
        driverEmail = sessionManager.getUserEmail() ?: ""

        // El viaje id viene del DriverTracker (la ruta acaba de terminar)
        val idViaje = DriverTracker.currentViajeId

        btnGuardar.setOnClickListener {
            guardarAsistenciaYContinuar(idViaje)
        }
    }

    override fun onResume() {
        super.onResume()
        if (driverEmail.isNotEmpty()) {
            loadRuta()
        }
    }

    private fun loadRuta() {
        ApiClient.apiService.getRuta(driverEmail).enqueue(object : Callback<List<EstudianteResponse>> {
            override fun onResponse(call: Call<List<EstudianteResponse>>, response: Response<List<EstudianteResponse>>) {
                if (response.isSuccessful) {
                    childrenList = response.body() ?: emptyList()
                    checkedChildren.clear()
                    for (child in childrenList) {
                        if (child.subio == true) {
                            checkedChildren.add(child.id_estudiante)
                        }
                    }
                    tvAttendanceCount.text = "${checkedChildren.size} / ${childrenList.size}"
                    btnFilterAll.performClick()
                }
            }
            override fun onFailure(call: Call<List<EstudianteResponse>>, t: Throwable) {}
        })
    }

    private fun guardarAsistenciaYContinuar(idViaje: Int?) {
        if (idViaje == null) {
            // Sin viaje id: igual navegar al resumen
            goToResumen()
            return
        }
        if (childrenList.isEmpty()) {
            goToResumen()
            return
        }

        var pendientes = childrenList.size
        for (child in childrenList) {
            val subio = checkedChildren.contains(child.id_estudiante)
            val request = AsistenciaRequest(child.id_estudiante, subio)
            ApiClient.apiService.marcarAsistencia(idViaje, request).enqueue(object : Callback<AsistenciaResponse> {
                override fun onResponse(call: Call<AsistenciaResponse>, response: Response<AsistenciaResponse>) {
                    pendientes--
                    if (pendientes == 0) {
                        runOnUiThread { goToResumen() }
                    }
                }
                override fun onFailure(call: Call<AsistenciaResponse>, t: Throwable) {
                    pendientes--
                    if (pendientes == 0) {
                        runOnUiThread { goToResumen() }
                    }
                }
            })
        }
    }

    private fun goToResumen() {
        startActivity(Intent(this, ResumenViajeActivity::class.java))
        finish()
    }
}
