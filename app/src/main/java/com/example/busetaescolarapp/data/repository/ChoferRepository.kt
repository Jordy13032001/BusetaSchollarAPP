package com.example.busetaescolarapp.data.repository

import com.example.busetaescolarapp.network.ApiClient
import com.example.busetaescolarapp.network.ApiResponse
import com.example.busetaescolarapp.network.EstudianteResponse
import com.example.busetaescolarapp.network.RutaInfoRequest
import com.example.busetaescolarapp.network.RutaInfoResponse
import com.example.busetaescolarapp.network.ViajeResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChoferRepository {
    fun getRuta(email: String, onResult: (List<EstudianteResponse>?) -> Unit) {
        ApiClient.apiService.getRuta(email).enqueue(object : Callback<List<EstudianteResponse>> {
            override fun onResponse(call: Call<List<EstudianteResponse>>, response: Response<List<EstudianteResponse>>) {
                if (response.isSuccessful) {
                    onResult(response.body())
                } else {
                    onResult(null)
                }
            }
            override fun onFailure(call: Call<List<EstudianteResponse>>, t: Throwable) {
                onResult(null)
            }
        })
    }

    fun iniciarViaje(email: String, onResult: (ViajeResponse?) -> Unit) {
        ApiClient.apiService.iniciarViaje(email).enqueue(object : Callback<ViajeResponse> {
            override fun onResponse(call: Call<ViajeResponse>, response: Response<ViajeResponse>) {
                if (response.isSuccessful) {
                    onResult(response.body())
                } else {
                    onResult(null)
                }
            }
            override fun onFailure(call: Call<ViajeResponse>, t: Throwable) {
                onResult(null)
            }
        })
    }

    fun finalizarViaje(email: String, fecha: String, onResult: (ViajeResponse?) -> Unit) {
        val request = com.example.busetaescolarapp.network.FinalizarRequest(fecha)
        ApiClient.apiService.finalizarViaje(email, request).enqueue(object : Callback<ViajeResponse> {
            override fun onResponse(call: Call<ViajeResponse>, response: Response<ViajeResponse>) {
                if (response.isSuccessful) {
                    onResult(response.body())
                } else {
                    onResult(null)
                }
            }
            override fun onFailure(call: Call<ViajeResponse>, t: Throwable) {
                onResult(null)
            }
        })
    }

    // --- Solicitudes de estudiantes: el chofer decide quién entra a su ruta ---

    fun getSolicitudesEstudiantes(email: String, onResult: (List<EstudianteResponse>?) -> Unit) {
        ApiClient.apiService.getSolicitudesEstudiantes(email).enqueue(object : Callback<List<EstudianteResponse>> {
            override fun onResponse(call: Call<List<EstudianteResponse>>, response: Response<List<EstudianteResponse>>) {
                onResult(if (response.isSuccessful) response.body() else null)
            }
            override fun onFailure(call: Call<List<EstudianteResponse>>, t: Throwable) {
                onResult(null)
            }
        })
    }

    fun aceptarEstudiante(idEstudiante: Int, onResult: (Boolean) -> Unit) {
        ApiClient.apiService.aceptarEstudiante(idEstudiante).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                onResult(response.isSuccessful)
            }
            override fun onFailure(call: Call<ApiResponse>, t: Throwable) = onResult(false)
        })
    }

    fun rechazarEstudiante(idEstudiante: Int, onResult: (Boolean) -> Unit) {
        ApiClient.apiService.rechazarEstudiante(idEstudiante).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                onResult(response.isSuccessful)
            }
            override fun onFailure(call: Call<ApiResponse>, t: Throwable) = onResult(false)
        })
    }

    // --- Ruta descriptiva que el padre ve antes de contratar ---

    fun getRutaInfo(email: String, onResult: (RutaInfoResponse?) -> Unit) {
        ApiClient.apiService.getRutaInfo(email).enqueue(object : Callback<RutaInfoResponse> {
            override fun onResponse(call: Call<RutaInfoResponse>, response: Response<RutaInfoResponse>) {
                onResult(if (response.isSuccessful) response.body() else null)
            }
            override fun onFailure(call: Call<RutaInfoResponse>, t: Throwable) = onResult(null)
        })
    }

    fun updateRutaInfo(email: String, request: RutaInfoRequest, onResult: (Boolean) -> Unit) {
        ApiClient.apiService.updateRutaInfo(email, request).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                onResult(response.isSuccessful)
            }
            override fun onFailure(call: Call<ApiResponse>, t: Throwable) = onResult(false)
        })
    }

    fun marcarAsistencia(idViaje: Int, request: com.example.busetaescolarapp.network.AsistenciaRequest, onResult: (Boolean) -> Unit) {
        ApiClient.apiService.marcarAsistencia(idViaje, request).enqueue(object : Callback<com.example.busetaescolarapp.network.AsistenciaResponse> {
            override fun onResponse(call: Call<com.example.busetaescolarapp.network.AsistenciaResponse>, response: Response<com.example.busetaescolarapp.network.AsistenciaResponse>) {
                onResult(response.isSuccessful)
            }
            override fun onFailure(call: Call<com.example.busetaescolarapp.network.AsistenciaResponse>, t: Throwable) {
                onResult(false)
            }
        })
    }
}
