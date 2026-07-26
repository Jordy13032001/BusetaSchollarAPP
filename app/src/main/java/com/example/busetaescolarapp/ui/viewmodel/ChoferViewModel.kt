package com.example.busetaescolarapp.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.busetaescolarapp.data.repository.ChoferRepository
import com.example.busetaescolarapp.network.EstudianteResponse
import com.example.busetaescolarapp.network.ViajeResponse

class ChoferViewModel : ViewModel() {
    private val repository = ChoferRepository()

    private val _ruta = MutableLiveData<List<EstudianteResponse>>()
    val ruta: LiveData<List<EstudianteResponse>> = _ruta

    private val _viajeActivo = MutableLiveData<ViajeResponse?>()
    val viajeActivo: LiveData<ViajeResponse?> = _viajeActivo

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadRuta(email: String) {
        _isLoading.value = true
        repository.getRuta(email) { result ->
            _isLoading.value = false
            if (result != null) {
                _ruta.value = result
            } else {
                _ruta.value = emptyList()
            }
        }
    }

    fun iniciarViaje(email: String) {
        repository.iniciarViaje(email) { result ->
            if (result != null) {
                _viajeActivo.value = result
            }
        }
    }

    fun finalizarViaje(email: String, fecha: String) {
        repository.finalizarViaje(email, fecha) {
            _viajeActivo.value = null
        }
    }

    fun resetViajeActivo() {
        _viajeActivo.value = null
    }

    fun marcarAsistencia(idViaje: Int, idEstudiante: Int, subio: Boolean, motivo: String? = null) {
        val request = com.example.busetaescolarapp.network.AsistenciaRequest(idEstudiante, subio, motivo)
        repository.marcarAsistencia(idViaje, request) { success ->
            // Optionally handle success or failure
        }
    }
}
