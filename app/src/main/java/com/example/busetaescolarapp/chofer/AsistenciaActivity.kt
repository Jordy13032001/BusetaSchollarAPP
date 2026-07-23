package com.example.busetaescolarapp.chofer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.busetaescolarapp.NavigationUtils
import com.example.busetaescolarapp.R

import android.content.Context
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.busetaescolarapp.network.ApiClient
import com.example.busetaescolarapp.network.AsistenciaRequest
import com.example.busetaescolarapp.network.AsistenciaResponse
import com.example.busetaescolarapp.network.EstudianteResponse
import com.example.busetaescolarapp.network.ViajeResponse
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
    private var currentViajeId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asistencia)

        tvAttendanceCount = findViewById(R.id.tvAttendanceCount)
        rvAsistenciaDetail = findViewById(R.id.rvAsistenciaDetail)
        rvAsistenciaDetail.layoutManager = LinearLayoutManager(this)

        val btnGuardar = findViewById<Button>(R.id.btnGuardar)
        val btnReiniciar = findViewById<Button>(R.id.btnReiniciar)
        
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

        NavigationUtils.setupChoferBottomNavigation(this)

        btnGuardar.setOnClickListener {
            guardarAsistencia()
        }

        btnReiniciar.setOnClickListener {
            checkedChildren.clear()
            tvAttendanceCount.text = "0 / ${childrenList.size}"
            btnFilterAll.performClick() // Resetea la lista y el filtro a "Todos"
            guardarAsistencia() // Sincronizar inmediatamente con el servidor
            Toast.makeText(this, "Asistencia reiniciada", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (driverEmail.isNotEmpty()) {
            loadRuta()
            loadViajeActual()
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

    private fun loadViajeActual() {
        ApiClient.apiService.iniciarViaje(driverEmail).enqueue(object : Callback<ViajeResponse> {
            override fun onResponse(call: Call<ViajeResponse>, response: Response<ViajeResponse>) {
                if (response.isSuccessful) {
                    currentViajeId = response.body()?.id_viaje
                }
            }
            override fun onFailure(call: Call<ViajeResponse>, t: Throwable) {}
        })
    }

    private fun guardarAsistencia() {
        val idViaje = currentViajeId
        if (idViaje == null) {
            Toast.makeText(this, "No se pudo determinar el viaje de hoy", Toast.LENGTH_SHORT).show()
            return
        }
        if (childrenList.isEmpty()) return

        var pendientes = childrenList.size
        for (child in childrenList) {
            val subio = checkedChildren.contains(child.id_estudiante)
            val request = AsistenciaRequest(child.id_estudiante, subio)
            ApiClient.apiService.marcarAsistencia(idViaje, request).enqueue(object : Callback<AsistenciaResponse> {
                override fun onResponse(call: Call<AsistenciaResponse>, response: Response<AsistenciaResponse>) {
                    pendientes--
                    if (pendientes == 0) {
                        Toast.makeText(this@AsistenciaActivity, "Asistencia guardada: ${checkedChildren.size} niños", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<AsistenciaResponse>, t: Throwable) {
                    pendientes--
                    Toast.makeText(this@AsistenciaActivity, "Error guardando asistencia de ${child.nombre_completo}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}
