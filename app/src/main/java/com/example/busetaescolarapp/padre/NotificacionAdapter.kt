package com.example.busetaescolarapp.padre

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.busetaescolarapp.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import android.graphics.Color

class NotificacionAdapter(
    private val lista: List<Notificacion>,
    // El pago y el reenvío los resuelve la Activity: el adaptador solo avisa
    private val onPagar: ((Notificacion) -> Unit)? = null,
    private val onReenviar: ((Notificacion) -> Unit)? = null
) : RecyclerView.Adapter<NotificacionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titulo: TextView = view.findViewById(R.id.txtTituloNotificacion)
        val mensaje: TextView = view.findViewById(R.id.txtMensajeNotificacion)
        val hora: TextView = view.findViewById(R.id.txtHoraNotificacion)
        val icono: ShapeableImageView = view.findViewById(R.id.imgIconoNotificacion)
        val contenedorAccion: LinearLayout = view.findViewById(R.id.contenedorAccionNotificacion)
        val descripcionAccion: TextView = view.findViewById(R.id.txtAccionDescripcion)
        val botonAccion: MaterialButton = view.findViewById(R.id.btnAccionNotificacion)
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
            TipoNotificacion.SOLICITUD_ACEPTADA -> {
                holder.icono.setBackgroundColor(Color.parseColor("#E8F5E9")) // Verde claro
                holder.icono.setColorFilter(Color.parseColor("#2E7D32")) // Verde oscuro
                holder.icono.setImageResource(android.R.drawable.checkbox_on_background)
            }
            TipoNotificacion.SOLICITUD_RECHAZADA -> {
                holder.icono.setBackgroundColor(Color.parseColor("#FFEBEE")) // Rojo claro
                holder.icono.setColorFilter(Color.parseColor("#C62828")) // Rojo oscuro
                holder.icono.setImageResource(android.R.drawable.ic_delete)
            }
        }

        mostrarAccion(holder, notificacion)
    }

    private fun mostrarAccion(holder: ViewHolder, notificacion: Notificacion) {
        // El ViewHolder se recicla: la acción se apaga siempre y solo se enciende
        // en los dos casos que la necesitan.
        holder.contenedorAccion.visibility = View.GONE
        holder.botonAccion.setOnClickListener(null)

        // Sin id de estudiante no se sabe sobre quién actuar
        if (notificacion.idEstudiante == null) return

        // Las notificaciones no se borran, así que una vieja puede haber quedado
        // obsoleta: si el hijo ya cambió de estado, el botón no debe seguir activo.
        // (ej. le rechazaron, reenvió a otro chofer y ese ya lo aceptó)
        val estado = notificacion.estadoEstudiante

        when (notificacion.tipo) {
            TipoNotificacion.SOLICITUD_ACEPTADA -> {
                if (estado != null && estado != "ACEPTADO") return
                holder.contenedorAccion.visibility = View.VISIBLE
                holder.descripcionAccion.text =
                    "El cupo está reservado. Completa el pago para confirmarlo."
                holder.botonAccion.text = "Proceder al pago"
                holder.botonAccion.setBackgroundColor(Color.parseColor("#2E7D32"))
                holder.botonAccion.setOnClickListener { onPagar?.invoke(notificacion) }
            }
            TipoNotificacion.SOLICITUD_RECHAZADA -> {
                if (estado != null && estado != "RECHAZADO") return
                holder.contenedorAccion.visibility = View.VISIBLE
                holder.descripcionAccion.text =
                    "Puedes elegir otro chofer y enviarle la solicitud."
                holder.botonAccion.text = "Buscar otro chofer"
                holder.botonAccion.setBackgroundColor(Color.parseColor("#C62828"))
                holder.botonAccion.setOnClickListener { onReenviar?.invoke(notificacion) }
            }
            else -> Unit
        }
    }
}
