package com.example.busetaescolarapp.padre

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.busetaescolarapp.NotificationHelper
import com.example.busetaescolarapp.network.ApiClient
import com.example.busetaescolarapp.network.NotificationResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Consulta el backend cada pocos segundos y convierte en notificación del sistema
 * todo lo que llegó nuevo. El backend no hace push (no hay FCM), así que la app
 * pregunta ella misma mientras está abierta.
 *
 * El id de la última notificación mostrada se guarda en SharedPreferences para
 * que al reabrir la app no vuelvan a sonar avisos ya vistos.
 */
class NotificacionPoller(
    private val context: Context,
    private val parentEmail: String,
    private val intervaloMs: Long = 15_000L,
    // Avisa que llegó algo nuevo, para que la pantalla que esté abierta se refresque
    // sin que el usuario tenga que salir y volver a entrar.
    private val onNuevas: (() -> Unit)? = null
) {

    private val handler = Handler(Looper.getMainLooper())
    private val prefs = context.getSharedPreferences("padre_prefs", Context.MODE_PRIVATE)
    private val clave = "last_notification_id_$parentEmail"

    private var ultimoId: Int = prefs.getInt(clave, 0)

    private val tarea = object : Runnable {
        override fun run() {
            consultar()
            handler.postDelayed(this, intervaloMs)
        }
    }

    fun iniciar() {
        if (parentEmail.isEmpty()) return
        // Se quita primero para no encolar dos veces si se llama en onResume repetido
        handler.removeCallbacks(tarea)
        handler.post(tarea)
    }

    fun detener() {
        handler.removeCallbacks(tarea)
    }

    private fun consultar() {
        ApiClient.apiService.getNotifications(parentEmail)
            .enqueue(object : Callback<List<NotificationResponse>> {
                override fun onResponse(
                    call: Call<List<NotificationResponse>>,
                    response: Response<List<NotificationResponse>>
                ) {
                    val notifs = response.body()?.takeIf { response.isSuccessful } ?: return
                    if (notifs.isEmpty()) return

                    // Primera vez en este teléfono: el historial completo ya está en la
                    // pantalla de notificaciones, así que solo se toma como punto de
                    // partida. Sin esto sonarían de golpe todas las notificaciones viejas.
                    if (ultimoId == 0) {
                        guardarUltimoId(notifs.maxOf { it.id })
                        onNuevas?.invoke()
                        return
                    }

                    val nuevas = notifs.filter { it.id > ultimoId }
                    if (nuevas.isEmpty()) return

                    // De la más vieja a la más nueva, para que queden en orden en la bandeja
                    nuevas.sortedBy { it.id }.forEach { notif ->
                        NotificationHelper.sendNotification(context, notif.title, notif.message)
                    }

                    guardarUltimoId(nuevas.maxOf { it.id })
                    onNuevas?.invoke()
                }

                override fun onFailure(call: Call<List<NotificationResponse>>, t: Throwable) {
                    // Sin conexión: se reintenta en el siguiente ciclo
                }
            })
    }

    private fun guardarUltimoId(id: Int) {
        ultimoId = id
        prefs.edit().putInt(clave, id).apply()
    }
}
