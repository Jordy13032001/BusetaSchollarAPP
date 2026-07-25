package com.example.busetaescolarapp.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

object VoiceRecognitionManager {
    private var speechRecognizer: SpeechRecognizer? = null
    private var appContext: Context? = null

    /** true desde que pedimos escuchar hasta que llega un resultado o un error. */
    private var isActive = false
    private val handler = Handler(Looper.getMainLooper())

    fun init(context: Context) {
        appContext = context.applicationContext
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e("VoiceRecognitionManager", "El reconocimiento de voz no está disponible en este dispositivo")
            return
        }
        // Idempotente: varias Activities llaman init(), no queremos tirar una sesión en curso.
        if (speechRecognizer == null) crearRecognizer()
    }

    private fun crearRecognizer() {
        val ctx = appContext ?: return
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(ctx)
        Log.d("VoiceRecognitionManager", "Speech Recognizer creado")
    }

    fun startListening(onResult: (String) -> Unit, onError: (String) -> Unit) {
        // El SpeechRecognizer SOLO funciona desde el hilo principal.
        if (Looper.myLooper() != Looper.getMainLooper()) {
            handler.post { startListening(onResult, onError) }
            return
        }

        if (appContext == null) {
            onError("SpeechRecognizer no inicializado")
            return
        }
        if (isActive) {
            Log.d("VoiceRecognitionManager", "Ya se está escuchando, se ignora la petición duplicada")
            return
        }
        if (speechRecognizer == null) crearRecognizer()

        // Cancelamos cualquier sesión previa colgada: sin esto el reconocedor
        // responde ERROR_RECOGNIZER_BUSY y parece que "no escucha".
        speechRecognizer?.cancel()

        val ctx = appContext!!
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            // Varios dispositivos (Xiaomi entre ellos) exigen este extra para dar resultados.
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, ctx.packageName)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            // Más margen antes de cortar por silencio: respuestas cortas como "sí" se perdían.
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2500)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 2000)
        }

        var resuelto = false
        fun resolverResultado(texto: String) {
            if (resuelto) return
            resuelto = true
            isActive = false
            onResult(texto)
        }
        fun resolverError(msg: String) {
            if (resuelto) return
            resuelto = true
            isActive = false
            onError(msg)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("VoiceRecognitionManager", "Listo para escuchar")
            }

            override fun onBeginningOfSpeech() {
                Log.d("VoiceRecognitionManager", "Comenzó a hablar")
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d("VoiceRecognitionManager", "Terminó de hablar")
            }

            override fun onError(error: Int) {
                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Error de audio"
                    SpeechRecognizer.ERROR_CLIENT -> "Error del cliente"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permisos insuficientes"
                    SpeechRecognizer.ERROR_NETWORK -> "Error de red"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Tiempo de red agotado"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No se entendió"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Reconocedor ocupado"
                    SpeechRecognizer.ERROR_SERVER -> "Error de servidor"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No se escuchó nada"
                    else -> "Error desconocido: $error"
                }
                Log.e("VoiceRecognitionManager", errorMsg)

                // Si el reconocedor quedó en mal estado, lo recreamos para el siguiente intento.
                if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY || error == SpeechRecognizer.ERROR_CLIENT) {
                    crearRecognizer()
                }
                resolverError(errorMsg)
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    Log.d("VoiceRecognitionManager", "Resultados: $matches")
                    resolverResultado(matches.joinToString(" "))
                } else {
                    resolverError("No se reconoció ninguna palabra")
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                // Un "sí"/"no" suelto a veces solo llega como resultado parcial y luego
                // el reconocedor corta con NO_MATCH; lo aprovechamos si ya es concluyente.
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val texto = matches.joinToString(" ")
                    if (esRespuestaConcluyente(texto)) {
                        Log.d("VoiceRecognitionManager", "Resultado parcial concluyente: $texto")
                        speechRecognizer?.stopListening()
                        resolverResultado(texto)
                    }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        isActive = true
        speechRecognizer?.startListening(intent)
        Log.d("VoiceRecognitionManager", "Empezando a escuchar...")
    }

    /** Separa el texto en palabras para no confundir "no" dentro de "nombre" o "si" dentro de "siguiente". */
    private fun palabras(texto: String): List<String> =
        texto.lowercase()
            .replace(Regex("[^a-záéíóúñü ]"), " ")
            .split(" ")
            .filter { it.isNotBlank() }

    private val PALABRAS_SI = setOf("si", "sí", "claro", "afirmativo", "correcto", "subio", "subió", "presente")
    private val PALABRAS_NO = setOf("no", "nunca", "tampoco", "negativo", "falta", "ausente")

    /**
     * Interpreta la respuesta: true = subió, false = no subió, null = no concluyente.
     * La negación tiene prioridad porque "no subió" contiene ambas familias de palabras
     * y es, sin ambigüedad, una negación.
     */
    fun interpretarRespuesta(texto: String): Boolean? {
        val p = palabras(texto)
        if (p.any { it in PALABRAS_NO }) return false
        if (p.any { it in PALABRAS_SI }) return true
        return null
    }

    fun esRespuestaConcluyente(texto: String): Boolean = interpretarRespuesta(texto) != null

    fun stopListening() {
        handler.post {
            speechRecognizer?.cancel()
            isActive = false
        }
    }

    fun shutdown() {
        handler.post {
            speechRecognizer?.destroy()
            speechRecognizer = null
            isActive = false
        }
    }
}
