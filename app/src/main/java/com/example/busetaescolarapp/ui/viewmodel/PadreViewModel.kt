package com.example.busetaescolarapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.busetaescolarapp.data.local.IncidenteEntity
import com.example.busetaescolarapp.data.local.NotificacionEntity
import com.example.busetaescolarapp.data.repository.PadreRepository
import com.example.busetaescolarapp.network.EstudianteResponse
import kotlinx.coroutines.launch

class PadreViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PadreRepository(application)

    private val _children = MutableLiveData<List<EstudianteResponse>>()
    val children: LiveData<List<EstudianteResponse>> = _children

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun fetchChildren(email: String) {
        _isLoading.value = true
        repository.getParentChildren(email) { result ->
            _isLoading.value = false
            if (result != null) {
                _children.value = result
            } else {
                _children.value = emptyList()
            }
        }
    }

    // --- Notificaciones (Room, offline-first) ---

    fun notificaciones(email: String): LiveData<List<NotificacionEntity>> =
        repository.getNotificacionesLocal(email)

    fun sincronizarNotificaciones(email: String) {
        viewModelScope.launch { repository.sincronizarNotificaciones(email) }
    }

    // --- Incidentes (Room, offline-first) ---

    fun incidentes(email: String): LiveData<List<IncidenteEntity>> =
        repository.getIncidentesLocal(email)

    fun sincronizarIncidentes(email: String) {
        viewModelScope.launch { repository.sincronizarIncidentes(email) }
    }
}
