package com.example.busetaescolarapp.chofer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.busetaescolarapp.R
import com.example.busetaescolarapp.network.EstudianteResponse
import com.google.android.material.button.MaterialButton

class SolicitudEstudianteAdapter(
    private val solicitudes: MutableList<EstudianteResponse>,
    private val onAceptar: (EstudianteResponse) -> Unit,
    private val onRechazar: (EstudianteResponse) -> Unit
) : RecyclerView.Adapter<SolicitudEstudianteAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombreEstudiante)
        val tvDireccion: TextView = view.findViewById(R.id.tvDireccionEstudiante)
        val tvCorreoPadre: TextView = view.findViewById(R.id.tvCorreoPadre)
        val btnAceptar: MaterialButton = view.findViewById(R.id.btnAceptarEstudiante)
        val btnRechazar: MaterialButton = view.findViewById(R.id.btnRechazarEstudiante)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_solicitud_estudiante, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val solicitud = solicitudes[position]
        holder.tvNombre.text = solicitud.nombre_completo
        holder.tvDireccion.text = solicitud.direccion
        holder.tvCorreoPadre.text = solicitud.correo_padre

        holder.btnAceptar.setOnClickListener { onAceptar(solicitud) }
        holder.btnRechazar.setOnClickListener { onRechazar(solicitud) }
    }

    override fun getItemCount() = solicitudes.size

    fun quitar(solicitud: EstudianteResponse) {
        val index = solicitudes.indexOfFirst { it.id_estudiante == solicitud.id_estudiante }
        if (index >= 0) {
            solicitudes.removeAt(index)
            notifyItemRemoved(index)
        }
    }
}
