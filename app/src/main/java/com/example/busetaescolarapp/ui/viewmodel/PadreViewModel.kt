package com.example.busetaescolarapp.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.busetaescolarapp.data.repository.PadreRepository
import com.example.busetaescolarapp.network.EstudianteResponse

class PadreViewModel : ViewModel() {
    private val repository = PadreRepository()

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
}
