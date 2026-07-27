package com.example.busetaescolarapp.chofer

import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.busetaescolarapp.R
import com.example.busetaescolarapp.data.repository.ChoferRepository
import com.example.busetaescolarapp.network.ApiClient
import com.example.busetaescolarapp.network.ColegioResponse
import com.example.busetaescolarapp.network.RutaInfoRequest
import com.example.busetaescolarapp.utils.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar

class DefinirRutaActivity : AppCompatActivity() {

    private lateinit var etNombre: TextInputEditText
    private lateinit var etSectores: TextInputEditText
    private lateinit var etHoraSalida: TextInputEditText
    private lateinit var rgTurno: RadioGroup
    private lateinit var actvColegio: AutoCompleteTextView

    private val repository = ChoferRepository()
    private var driverEmail: String = ""
    private var colegios: List<ColegioResponse> = emptyList()
    private var selectedColegioId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_definir_ruta)

        etNombre = findViewById(R.id.etNombreRuta)
        etSectores = findViewById(R.id.etSectores)
        etHoraSalida = findViewById(R.id.etHoraSalida)
        rgTurno = findViewById(R.id.rgTurno)
        actvColegio = findViewById(R.id.actvColegio)

        driverEmail = SessionManager(this).getUserEmail() ?: ""

        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbarDefinirRuta)
            ?.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        etHoraSalida.setOnClickListener { mostrarSelectorHora() }
        findViewById<MaterialButton>(R.id.btnGuardarRuta).setOnClickListener { guardar() }

        cargarColegios()
    }

    private fun cargarColegios() {
        ApiClient.apiService.getColegios().enqueue(object : Callback<List<ColegioResponse>> {
            override fun onResponse(call: Call<List<ColegioResponse>>, response: Response<List<ColegioResponse>>) {
                if (response.isSuccessful) {
                    colegios = response.body() ?: emptyList()
                    val nombres = colegios.map { it.nombre }
                    val adapter = ArrayAdapter(this@DefinirRutaActivity, android.R.layout.simple_dropdown_item_1line, nombres)
                    actvColegio.setAdapter(adapter)
                    actvColegio.setOnItemClickListener { _, _, position, _ ->
                        selectedColegioId = colegios[position].id_colegio
                    }
                }
                if (driverEmail.isNotEmpty()) cargarRuta()
            }

            override fun onFailure(call: Call<List<ColegioResponse>>, t: Throwable) {
                Toast.makeText(this@DefinirRutaActivity, "Error al cargar colegios", Toast.LENGTH_SHORT).show()
                if (driverEmail.isNotEmpty()) cargarRuta()
            }
        })
    }

    private fun cargarRuta() {
        repository.getRutaInfo(driverEmail) { info ->
            if (info == null) return@getRutaInfo
            etNombre.setText(info.nombre)
            etSectores.setText(info.sectores ?: "")
            etHoraSalida.setText(info.hora_salida ?: "")
            rgTurno.check(if (info.turno == "TARDE") R.id.rbTarde else R.id.rbManana)

            val colegioActual = colegios.find { it.id_colegio == info.id_colegio }
            if (colegioActual != null) {
                actvColegio.setText(colegioActual.nombre, false)
                selectedColegioId = colegioActual.id_colegio
            } else if (info.colegio != null) {
                actvColegio.setText(info.colegio, false)
            }
        }
    }

    private fun mostrarSelectorHora() {
        val ahora = Calendar.getInstance()
        val partes = etHoraSalida.text?.toString()?.split(":")
        val hora = partes?.getOrNull(0)?.toIntOrNull() ?: ahora.get(Calendar.HOUR_OF_DAY)
        val minuto = partes?.getOrNull(1)?.toIntOrNull() ?: 0

        TimePickerDialog(this, { _, h, m ->
            etHoraSalida.setText(String.format("%02d:%02d", h, m))
        }, hora, minuto, true).show()
    }

    private fun guardar() {
        val nombre = etNombre.text?.toString()?.trim().orEmpty()
        if (nombre.isEmpty()) {
            etNombre.error = "Ponle un nombre a tu ruta"
            return
        }
        if (selectedColegioId == null) {
            Toast.makeText(this, "Selecciona el colegio destino", Toast.LENGTH_SHORT).show()
            return
        }

        val request = RutaInfoRequest(
            nombre = nombre,
            turno = if (rgTurno.checkedRadioButtonId == R.id.rbTarde) "TARDE" else "MANANA",
            sectores = etSectores.text?.toString()?.trim().orEmpty().ifEmpty { null },
            hora_salida = etHoraSalida.text?.toString()?.trim().orEmpty().ifEmpty { null },
            id_colegio = selectedColegioId
        )

        repository.updateRutaInfo(driverEmail, request) { exito ->
            if (exito) {
                Toast.makeText(this, "Ruta guardada", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "No se pudo guardar la ruta", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
