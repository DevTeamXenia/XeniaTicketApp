package com.xenia.ticket.data.network.service


import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object ApiClient {
//    private const val BASE_URL = "https://eticketapi.xeniapos.com/api/"
    private const val BASE_URL = "https://192.168.1.33:8085/api/"

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(UnsafeOkHttpClient.getUnsafeOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}