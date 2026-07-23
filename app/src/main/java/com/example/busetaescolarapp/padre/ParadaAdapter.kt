package com.example.busetaescolarapp.padre

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.busetaescolarapp.R

class ParadaAdapter(private val paradas: List<Parada>) : RecyclerView.Adapter<ParadaAdapter.ParadaViewHolder>() {

    class ParadaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombreParada)
        val tvHora: TextView = view.findViewById(R.id.tvHoraParada)
        val container: View = view.findViewById(R.id.containerParada)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParadaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_parada, parent, false)
        return ParadaViewHolder(view)
    }

    override fun onBindViewHolder(holder: ParadaViewHolder, position: Int) {
        val parada = paradas[position]
        holder.tvNombre.text = "${parada.numero}. ${parada.nombre}"
        holder.tvHora.text = parada.hora

        if (parada.esDestinoPropio) {
            holder.container.setBackgroundColor(Color.parseColor("#FFFDE7"))
            holder.tvNombre.setTextColor(Color.parseColor("#F57F17"))
            holder.tvHora.setTextColor(Color.parseColor("#F57F17"))
            holder.tvNombre.setTypeface(null, Typeface.BOLD)
            holder.tvHora.setTypeface(null, Typeface.BOLD)
        } else {
            holder.container.setBackgroundColor(Color.TRANSPARENT)
            holder.tvNombre.setTextColor(Color.parseColor("#000000"))
            holder.tvHora.setTextColor(Color.parseColor("#757575"))
            holder.tvNombre.setTypeface(null, Typeface.NORMAL)
            holder.tvHora.setTypeface(null, Typeface.NORMAL)
        }
    }

    override fun getItemCount() = paradas.size
}
