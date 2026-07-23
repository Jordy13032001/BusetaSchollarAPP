package com.example.busetaescolarapp.data.repository

import com.example.busetaescolarapp.network.ApiClient
import com.example.busetaescolarapp.network.ApiResponse
import com.example.busetaescolarapp.network.LoginRequest
import com.example.busetaescolarapp.network.RegistroRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AuthRepository {
    fun login(request: LoginRequest, onResult: (ApiResponse?) -> Unit) {
        ApiClient.apiService.login(request).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful) {
                    onResult(response.body())
                } else {
                    onResult(null)
                }
            }
            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                onResult(null)
            }
        })
    }

    fun register(request: RegistroRequest, onResult: (ApiResponse?) -> Unit) {
        ApiClient.apiService.registerUser(request).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful) {
                    onResult(response.body())
                } else {
                    onResult(null)
                }
            }
            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                onResult(null)
            }
        })
    }
}
