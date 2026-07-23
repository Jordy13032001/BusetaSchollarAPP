package com.example.busetaescolarapp.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

object VoiceRecognitionManager {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    fun init(context: Context) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            Log.d("VoiceRecognitionManager", "Speech Recognizer inicializado correctamente")
        } else {
            Log.e("VoiceRecognitionManager", "El reconocimiento de voz no está disponible en este dispositivo")
        }
    }

    fun startListening(onResult: (String) -> Unit, onError: (String) -> Unit) {
        if (speechRecognizer == null) {
            onError("SpeechRecognizer no inicializado")
            return
        }
        if (isListening) return

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("VoiceRecognitionManager", "Listo para escuchar")
            }

            override fun onBeginningOfSpeech() {
                isListening = true
                Log.d("VoiceRecognitionManager", "Comenzó a hablar")
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isListening = false
                Log.d("VoiceRecognitionManager", "Terminó de hablar")
            }

            override fun onError(error: Int) {
                isListening = false
                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Error de audio"
                    SpeechRecognizer.ERROR_CLIENT -> "Error del cliente"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permisos insuficientes"
                    SpeechRecognizer.ERROR_NETWORK -> "Error de red"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Tiempo de red agotado"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No se entendió el comando"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Reconocedor ocupado"
                    SpeechRecognizer.ERROR_SERVER -> "Error de servidor"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Tiempo de habla agotado"
                    else -> "Error desconocido: $error"
                }
                Log.e("VoiceRecognitionManager", errorMsg)
                onError(errorMsg)
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    Log.d("VoiceRecognitionManager", "Resultado: $text")
                    onResult(text)
                } else {
                    onError("No se reconoció ninguna palabra")
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer?.startListening(intent)
        Log.d("VoiceRecognitionManager", "Empezando a escuchar...")
    }

    fun stopListening() {
        if (isListening) {
            speechRecognizer?.stopListening()
            isListening = false
        }
    }

    fun shutdown() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        isListening = false
    }
}
