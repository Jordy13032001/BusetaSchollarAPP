package com.example.busetaescolarapp.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

object TextToSpeechManager : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    fun init(context: Context) {
        if (tts == null) {
            tts = TextToSpeech(context.applicationContext, this)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("es", "ES"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "El idioma Español no está soportado")
            } else {
                isInitialized = true
                Log.i("TTS", "TextToSpeech inicializado correctamente")
            }
        } else {
            Log.e("TTS", "Fallo la inicialización de TextToSpeech")
        }
    }

    fun speak(text: String) {
        if (isInitialized) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            Log.e("TTS", "TTS no ha sido inicializado aún. Intentando hablar: $text")
        }
    }

    /**
     * Habla y avisa exactamente cuando termina, para no empezar a escuchar mientras
     * el propio audio del bot todavía suena (eso es lo que hacía fallar el reconocimiento de voz).
     */
    fun speak(text: String, onDone: () -> Unit) {
        if (!isInitialized) {
            Log.e("TTS", "TTS no ha sido inicializado aún. Intentando hablar: $text")
            onDone()
            return
        }
        val utteranceId = System.currentTimeMillis().toString()
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                onDone()
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                onDone()
            }
        })
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        if (tts != null) {
            tts?.stop()
        }
    }

    fun shutdown() {
        if (tts != null) {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
        }
    }
}
