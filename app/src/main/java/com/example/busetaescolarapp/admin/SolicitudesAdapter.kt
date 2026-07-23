package com.example.busetaescolarapp.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.busetaescolarapp.R
import com.example.busetaescolarapp.network.SolicitudResponse

class SolicitudesAdapter(
    private val solicitudes: List<SolicitudResponse>,
    private val onAprobarClick: (SolicitudResponse) -> Unit,
    private val onRechazarClick: (SolicitudResponse) -> Unit
) : RecyclerView.Adapter<SolicitudesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombreChofer)
        val tvEmail: TextView = view.findViewById(R.id.tvEmailChofer)
        val tvInfo: TextView = view.findViewById(R.id.tvBusInfo)
        val btnAprobar: Button = view.findViewById(R.id.btnAprobar)
        val btnRechazar: Button = view.findViewById(R.id.btnRechazar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_solicitud, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val solicitud = solicitudes[position]
        holder.tvNombre.text = solicitud.nombre_completo
        holder.tvEmail.text = solicitud.correo
        holder.tvInfo.text = "Placa: ${solicitud.placa}\nModelo: ${solicitud.modelo}\nCapacidad: ${solicitud.capacidad}\nTarifa Mensual: $${solicitud.tarifa_mensual}"

        holder.btnAprobar.setOnClickListener { onAprobarClick(solicitud) }
        holder.btnRechazar.setOnClickListener { onRechazarClick(solicitud) }
    }

    override fun getItemCount(): Int = solicitudes.size
}
