package com.example.busetaescolarapp.network

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// Modelos de la respuesta de Google Directions API (solo los campos que usamos)
data class DirectionsResponse(
    val routes: List<DirectionsRoute>,
    val status: String
)

data class DirectionsRoute(
    val legs: List<DirectionsLeg>
)

data class DirectionsLeg(
    val start_address: String,
    val end_address: String,
    val distance: DirectionsValue,
    val duration: DirectionsValue,
    val steps: List<DirectionsStep>
)

data class DirectionsValue(
    val text: String,
    val value: Int
)

data class DirectionsStep(
    val polyline: DirectionsPolyline
)

data class DirectionsPolyline(
    val points: String
)

interface DirectionsService {
    @GET("directions/json")
    fun getDirections(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("waypoints") waypoints: String?,
        @Query("mode") mode: String = "driving",
        @Query("key") apiKey: String
    ): Call<DirectionsResponse>
}

object DirectionsApiClient {
    private const val BASE_URL = "https://maps.googleapis.com/maps/api/"

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val service: DirectionsService by lazy {
        retrofit.create(DirectionsService::class.java)
    }
}
