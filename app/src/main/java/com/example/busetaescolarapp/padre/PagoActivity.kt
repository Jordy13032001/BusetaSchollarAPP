package com.example.busetaescolarapp.padre

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.busetaescolarapp.R
import com.example.busetaescolarapp.data.local.PagoEntity
import com.example.busetaescolarapp.data.repository.PadreRepository
import com.example.busetaescolarapp.network.ApiClient
import com.example.busetaescolarapp.network.ChoferResponse
import com.example.busetaescolarapp.network.EstudianteResponse
import com.example.busetaescolarapp.utils.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Pago simulado del cupo mensual. No hay pasarela real ni modelo de pagos en el
 * backend: el comprobante se guarda solo en Room, en el teléfono del padre.
 */
class PagoActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID_ESTUDIANTE = "extra_id_estudiante"
        const val EXTRA_NOMBRE_ESTUDIANTE = "extra_nombre_estudiante"
    }

    private lateinit var repository: PadreRepository
    private lateinit var tvEstudiante: TextView
    private lateinit var tvChofer: TextView
    private lateinit var tvMonto: TextView
    private lateinit var etTitular: TextInputEditText
    private lateinit var etNumeroTarjeta: TextInputEditText
    private lateinit var etVencimiento: TextInputEditText
    private lateinit var etCvv: TextInputEditText
    private lateinit var btnConfirmar: MaterialButton

    private var idEstudiante: Int = -1
    private var nombreEstudiante: String = "Estudiante"
    private var nombreChofer: String = "Chofer"
    private var monto: Double = 0.0
    private var parentEmail: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pago)

        repository = PadreRepository(this)

        tvEstudiante = findViewById(R.id.tvPagoEstudiante)
        tvChofer = findViewById(R.id.tvPagoChofer)
        tvMonto = findViewById(R.id.tvPagoMonto)
        etTitular = findViewById(R.id.etTitular)
        etNumeroTarjeta = findViewById(R.id.etNumeroTarjeta)
        etVencimiento = findViewById(R.id.etVencimiento)
        etCvv = findViewById(R.id.etCvv)
        btnConfirmar = findViewById(R.id.btnConfirmarPago)

        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbarPago)
            .setNavigationOnClickListener { finish() }

        idEstudiante = intent.getIntExtra(EXTRA_ID_ESTUDIANTE, -1)
        nombreEstudiante = intent.getStringExtra(EXTRA_NOMBRE_ESTUDIANTE) ?: "Estudiante"
        parentEmail = SessionManager(this).getUserEmail() ?: ""

        if (idEstudiante == -1 || parentEmail.isEmpty()) {
            Toast.makeText(this, "No se pudo identificar la solicitud", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        tvEstudiante.text = nombreEstudiante
        tvChofer.text = "Cargando datos del chofer…"
        tvMonto.text = "--"
        // Hasta saber la tarifa no tiene sentido dejar pagar
        btnConfirmar.isEnabled = false

        verificarSiYaPago()
        cargarDatosDelCupo()

        btnConfirmar.setOnClickListener { confirmarPago() }
    }

    private fun verificarSiYaPago() {
        lifecycleScope.launch {
            if (repository.estaPagado(idEstudiante)) {
                AlertDialog.Builder(this@PagoActivity)
                    .setTitle("Cupo ya pagado")
                    .setMessage("Ya registraste un pago para $nombreEstudiante. ¿Quieres pagar otro mes?")
                    .setPositiveButton("Pagar de nuevo", null)
                    .setNegativeButton("Salir") { _, _ -> finish() }
                    .setCancelable(false)
                    .show()
            }
        }
    }

    /** La tarifa vive en el chofer, así que hay que ir del hijo → su chofer → su tarifa. */
    private fun cargarDatosDelCupo() {
        ApiClient.apiService.getParentChildren(parentEmail)
            .enqueue(object : Callback<List<EstudianteResponse>> {
                override fun onResponse(
                    call: Call<List<EstudianteResponse>>,
                    response: Response<List<EstudianteResponse>>
                ) {
                    val hijo = response.body()
                        ?.takeIf { response.isSuccessful }
                        ?.firstOrNull { it.id_estudiante == idEstudiante }

                    if (hijo == null) {
                        mostrarErrorDeCarga()
                        return
                    }

                    nombreEstudiante = hijo.nombre_completo
                    nombreChofer = hijo.nombre_chofer ?: hijo.correo_chofer
                    tvEstudiante.text = nombreEstudiante
                    tvChofer.text = "Chofer: $nombreChofer"
                    cargarTarifa(hijo.correo_chofer)
                }

                override fun onFailure(call: Call<List<EstudianteResponse>>, t: Throwable) {
                    mostrarErrorDeCarga()
                }
            })
    }

    private fun cargarTarifa(correoChofer: String) {
        ApiClient.apiService.getChoferes().enqueue(object : Callback<List<ChoferResponse>> {
            override fun onResponse(
                call: Call<List<ChoferResponse>>,
                response: Response<List<ChoferResponse>>
            ) {
                val chofer = response.body()
                    ?.takeIf { response.isSuccessful }
                    ?.firstOrNull { it.correo == correoChofer }

                val tarifa = chofer?.tarifa_mensual
                if (tarifa == null) {
                    mostrarErrorDeCarga()
                    return
                }
                monto = tarifa
                tvMonto.text = String.format("$%.2f", monto)
                btnConfirmar.isEnabled = true
            }

            override fun onFailure(call: Call<List<ChoferResponse>>, t: Throwable) {
                mostrarErrorDeCarga()
            }
        })
    }

    private fun mostrarErrorDeCarga() {
        tvChofer.text = "No se pudo cargar la tarifa"
        tvMonto.text = "--"
        btnConfirmar.isEnabled = false
        Toast.makeText(this, "Revisa tu conexión e intenta de nuevo", Toast.LENGTH_LONG).show()
    }

    private fun confirmarPago() {
        val titular = etTitular.text.toString().trim()
        val numero = etNumeroTarjeta.text.toString().trim()
        val vencimiento = etVencimiento.text.toString().trim()
        val cvv = etCvv.text.toString().trim()

        if (titular.isEmpty()) {
            etTitular.error = "Ingresa el nombre del titular"
            etTitular.requestFocus()
            return
        }
        if (numero.length != 16) {
            etNumeroTarjeta.error = "La tarjeta debe tener 16 dígitos"
            etNumeroTarjeta.requestFocus()
            return
        }
        if (!Regex("^(0[1-9]|1[0-2])/\\d{2}$").matches(vencimiento)) {
            etVencimiento.error = "Usa el formato MM/AA"
            etVencimiento.requestFocus()
            return
        }
        if (cvv.length !in 3..4) {
            etCvv.error = "El CVV tiene 3 o 4 dígitos"
            etCvv.requestFocus()
            return
        }
        if (monto <= 0.0) {
            Toast.makeText(this, "Aún no se cargó la tarifa", Toast.LENGTH_SHORT).show()
            return
        }

        btnConfirmar.isEnabled = false
        btnConfirmar.text = "Procesando…"

        val pago = PagoEntity(
            correoPadre = parentEmail,
            idEstudiante = idEstudiante,
            nombreEstudiante = nombreEstudiante,
            nombreChofer = nombreChofer,
            monto = monto,
            metodo = "Tarjeta",
            // Solo los últimos 4 dígitos: el número completo no se guarda nunca
            referencia = "**** ${numero.takeLast(4)}"
        )

        lifecycleScope.launch {
            repository.registrarPago(pago)
            btnConfirmar.text = "Confirmar pago"
            AlertDialog.Builder(this@PagoActivity)
                .setTitle("Pago registrado")
                .setMessage(
                    "Se registró el pago de ${String.format("$%.2f", monto)} " +
                        "por el cupo de $nombreEstudiante con $nombreChofer.\n\n" +
                        "Recuerda que es una simulación: no se realizó ningún cobro."
                )
                .setPositiveButton("Listo") { _, _ ->
                    setResult(Activity.RESULT_OK)
                    finish()
                }
                .setCancelable(false)
                .show()
        }
    }
}
