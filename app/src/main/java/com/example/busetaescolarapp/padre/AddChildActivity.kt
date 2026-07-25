package com.example.busetaescolarapp.padre

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.busetaescolarapp.R
import com.example.busetaescolarapp.network.ApiClient
import com.example.busetaescolarapp.network.ApiResponse
import com.example.busetaescolarapp.network.EstudianteRequest
import com.example.busetaescolarapp.network.ChoferResponse
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import android.app.Activity
import android.content.Intent
import android.widget.ImageButton
import androidx.activity.result.contract.ActivityResultContracts

class AddChildActivity : AppCompatActivity() {

    private lateinit var rvChoferes: RecyclerView
    private lateinit var etNombreNino: TextInputEditText
    private lateinit var etDireccion: TextInputEditText
    private lateinit var btnMapLocation: ImageButton
    private var parentEmail: String = ""
    private var selectedLat: Double? = null
    private var selectedLng: Double? = null

    private val selectLocationLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val address = result.data?.getStringExtra("ADDRESS")
            if (!address.isNullOrEmpty()) {
                etDireccion.setText(address)
                if (result.data?.hasExtra("LAT") == true && result.data?.hasExtra("LNG") == true) {
                    selectedLat = result.data?.getDoubleExtra("LAT", 0.0)
                    selectedLng = result.data?.getDoubleExtra("LNG", 0.0)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agregar_hijo)

        rvChoferes = findViewById(R.id.rvChoferes)
        etNombreNino = findViewById(R.id.etNombreNino)
        etDireccion = findViewById(R.id.etDireccion)
        btnMapLocation = findViewById(R.id.btnMapLocation)

        btnMapLocation.setOnClickListener {
            selectLocationLauncher.launch(Intent(this, SelectLocationActivity::class.java))
        }

        rvChoferes.layoutManager = LinearLayoutManager(this)

        val sessionManager = com.example.busetaescolarapp.utils.SessionManager(this)
        parentEmail = sessionManager.getUserEmail() ?: ""

        if (parentEmail.isEmpty()) {
            Toast.makeText(this, "Error de sesión", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadChoferes()
    }

    private fun loadChoferes() {
        ApiClient.apiService.getChoferes().enqueue(object : Callback<List<ChoferResponse>> {
            override fun onResponse(call: Call<List<ChoferResponse>>, response: Response<List<ChoferResponse>>) {
                if (response.isSuccessful) {
                    val choferes = response.body() ?: emptyList()
                    val adapter = ChoferAdapter(choferes) { chofer ->
                        mostrarDialogoContratacion(chofer)
                    }
                    rvChoferes.adapter = adapter
                } else {
                    Toast.makeText(this@AddChildActivity, "Error al cargar choferes", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<ChoferResponse>>, t: Throwable) {
                Toast.makeText(this@AddChildActivity, "Fallo de red: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun mostrarDialogoContratacion(chofer: ChoferResponse) {
        val childName = etNombreNino.text.toString().trim()
        val childAddress = etDireccion.text.toString().trim()

        if (childName.isEmpty() || childAddress.isEmpty()) {
            Toast.makeText(this, "Llena los datos del niño primero", Toast.LENGTH_SHORT).show()
            return
        }

        val ruta = chofer.nombre_ruta?.let { "\n\nRuta: $it" } ?: ""
        val sectores = chofer.sectores?.takeIf { it.isNotBlank() }?.let { "\nSectores: $it" } ?: ""

        AlertDialog.Builder(this)
            .setTitle("Solicitar cupo")
            .setMessage(
                "¿Enviar solicitud a ${chofer.nombre_completo} por $${chofer.tarifa_mensual} al mes " +
                    "para llevar a $childName?$ruta$sectores\n\nEl chofer debe aceptar la solicitud."
            )
            .setPositiveButton("Enviar solicitud") { _, _ ->
                val request = EstudianteRequest(childName, childAddress, selectedLat, selectedLng, parentEmail, chofer.correo)

                ApiClient.apiService.addEstudiante(request).enqueue(object : Callback<ApiResponse> {
                    override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                        if (response.isSuccessful) {
                            Toast.makeText(
                                this@AddChildActivity,
                                "Solicitud enviada. Espera que el chofer la acepte.",
                                Toast.LENGTH_LONG
                            ).show()
                            finish()
                        } else {
                            Toast.makeText(this@AddChildActivity, "Error al guardar", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                        Toast.makeText(this@AddChildActivity, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}

class ChoferAdapter(
    private val choferes: List<ChoferResponse>,
    private val onClick: (ChoferResponse) -> Unit
) : RecyclerView.Adapter<ChoferAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvChoferName)
        val tvEmail: TextView = view.findViewById(R.id.tvChoferEmail)
        val tvPrice: TextView = view.findViewById(R.id.tvChoferPrice)
        val tvBus: TextView = view.findViewById(R.id.tvChoferBus)
        val tvRuta: TextView = view.findViewById(R.id.tvChoferRuta)
        val tvSectores: TextView = view.findViewById(R.id.tvChoferSectores)
        val tvHorario: TextView = view.findViewById(R.id.tvChoferHorario)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chofer, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val chofer = choferes[position]
        holder.tvName.text = chofer.nombre_completo
        holder.tvEmail.text = chofer.correo
        holder.tvPrice.text = "$${chofer.tarifa_mensual}"
        holder.tvBus.text = "Placa: ${chofer.placa ?: "N/A"} | ${chofer.modelo ?: ""}"

        holder.tvRuta.text = chofer.nombre_ruta ?: "Ruta sin definir"
        holder.tvSectores.text = chofer.sectores?.takeIf { it.isNotBlank() }
            ?: "El chofer aún no indicó los sectores que cubre"

        val turno = when (chofer.turno) {
            "MANANA" -> "Mañana"
            "TARDE" -> "Tarde"
            else -> null
        }
        holder.tvHorario.text = listOfNotNull(
            turno,
            chofer.hora_salida?.let { "salida $it" },
            chofer.colegio
        ).joinToString(" · ").ifEmpty { "Horario sin definir" }

        holder.itemView.setOnClickListener {
            onClick(chofer)
        }
    }

    override fun getItemCount() = choferes.size
}
