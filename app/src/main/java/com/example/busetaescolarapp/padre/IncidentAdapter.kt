package com.example.busetaescolarapp.padre

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.busetaescolarapp.R
import com.example.busetaescolarapp.data.local.IncidenteEntity

class IncidentAdapter(private val incidents: List<IncidenteEntity>) :
    RecyclerView.Adapter<IncidentAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDescription: TextView = view.findViewById(R.id.tvIncidentDescription)
        val tvStatus: TextView = view.findViewById(R.id.tvIncidentStatus)
        val tvDate: TextView = view.findViewById(R.id.tvIncidentDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_incident, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val incident = incidents[position]
        holder.tvDescription.text = incident.mensaje
        holder.tvStatus.text = "Estado: ${incident.estado}"
        holder.tvDate.text = incident.fechaHora
    }

    override fun getItemCount() = incidents.size
}
