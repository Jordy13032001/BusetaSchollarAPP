package com.example.busetaescolarapp.padre

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.busetaescolarapp.R
import com.google.android.material.imageview.ShapeableImageView
import android.graphics.Color

class NotificacionAdapter(private val lista: List<Notificacion>) :
    RecyclerView.Adapter<NotificacionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titulo: TextView = view.findViewById(R.id.txtTituloNotificacion)
        val mensaje: TextView = view.findViewById(R.id.txtMensajeNotificacion)
        val hora: TextView = view.findViewById(R.id.txtHoraNotificacion)
        val icono: ShapeableImageView = view.findViewById(R.id.imgIconoNotificacion)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notificacion, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return lista.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notificacion = lista[position]

        holder.titulo.text = notificacion.titulo
        holder.mensaje.text = notificacion.mensaje
        holder.hora.text = notificacion.hora

        // Cambiar colores e iconos segun el tipo para que se vea mas profesional
        when (notificacion.tipo) {
            TipoNotificacion.CERCA -> {
                holder.icono.setBackgroundColor(Color.parseColor("#FFFDE7")) // Verde claro
                holder.icono.setColorFilter(Color.parseColor("#F57F17")) // Verde oscuro
                holder.icono.setImageResource(android.R.drawable.ic_dialog_map)
            }
            TipoNotificacion.SUBIO -> {
                holder.icono.setBackgroundColor(Color.parseColor("#FFF9C4")) // Amarillo claro
                holder.icono.setColorFilter(Color.parseColor("#F9A825")) // Amarillo oscuro
                holder.icono.setImageResource(android.R.drawable.ic_menu_myplaces)
            }
            TipoNotificacion.FINALIZADA -> {
                holder.icono.setBackgroundColor(Color.parseColor("#F5F5F5")) // Gris claro
                holder.icono.setColorFilter(Color.parseColor("#9E9E9E")) // Gris oscuro
                holder.icono.setImageResource(android.R.drawable.ic_lock_power_off)
            }
            TipoNotificacion.ALERTA -> {
                holder.icono.setBackgroundColor(Color.parseColor("#FFEBEE")) // Rojo claro
                holder.icono.setColorFilter(Color.parseColor("#D32F2F")) // Rojo oscuro
                holder.icono.setImageResource(android.R.drawable.ic_dialog_alert)
            }
        }
    }
}
