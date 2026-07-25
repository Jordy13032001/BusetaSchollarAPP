package com.example.busetaescolarapp.chofer

import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.busetaescolarapp.R
import com.example.busetaescolarapp.data.repository.ChoferRepository
import com.example.busetaescolarapp.network.RutaInfoRequest
import com.example.busetaescolarapp.utils.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.util.Calendar

/**
 * El chofer define su ruta (nombre, sectores, turno y horario). Es lo que el padre
 * consulta antes de contratarlo; las paradas reales salen de los estudiantes aceptados.
 */
class DefinirRutaActivity : AppCompatActivity() {

    private lateinit var etNombre: TextInputEditText
    private lateinit var etSectores: TextInputEditText
    private lateinit var etHoraSalida: TextInputEditText
    private lateinit var rgTurno: RadioGroup
    private lateinit var tvColegio: TextView

    private val repository = ChoferRepository()
    private var driverEmail: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_definir_ruta)

        etNombre = findViewById(R.id.etNombreRuta)
        etSectores = findViewById(R.id.etSectores)
        etHoraSalida = findViewById(R.id.etHoraSalida)
        rgTurno = findViewById(R.id.rgTurno)
        tvColegio = findViewById(R.id.tvColegioRuta)

        driverEmail = SessionManager(this).getUserEmail() ?: ""

        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbarDefinirRuta)
            ?.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        etHoraSalida.setOnClickListener { mostrarSelectorHora() }
        findViewById<MaterialButton>(R.id.btnGuardarRuta).setOnClickListener { guardar() }

        if (driverEmail.isNotEmpty()) cargarRuta()
    }

    private fun cargarRuta() {
        repository.getRutaInfo(driverEmail) { info ->
            if (info == null) {
                Toast.makeText(this, "No se pudo cargar tu ruta", Toast.LENGTH_SHORT).show()
                return@getRutaInfo
            }
            etNombre.setText(info.nombre)
            etSectores.setText(info.sectores ?: "")
            etHoraSalida.setText(info.hora_salida ?: "")
            rgTurno.check(if (info.turno == "TARDE") R.id.rbTarde else R.id.rbManana)
            tvColegio.text = "Colegio destino: ${info.colegio ?: "—"}"
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

        val request = RutaInfoRequest(
            nombre = nombre,
            turno = if (rgTurno.checkedRadioButtonId == R.id.rbTarde) "TARDE" else "MANANA",
            sectores = etSectores.text?.toString()?.trim().orEmpty().ifEmpty { null },
            hora_salida = etHoraSalida.text?.toString()?.trim().orEmpty().ifEmpty { null }
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
