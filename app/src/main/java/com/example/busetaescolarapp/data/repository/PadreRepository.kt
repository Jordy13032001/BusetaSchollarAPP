package com.example.busetaescolarapp.data.repository

import com.example.busetaescolarapp.network.ApiClient
import com.example.busetaescolarapp.network.EstudianteResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PadreRepository {
    fun getParentChildren(email: String, onResult: (List<EstudianteResponse>?) -> Unit) {
        ApiClient.apiService.getParentChildren(email).enqueue(object : Callback<List<EstudianteResponse>> {
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
}
