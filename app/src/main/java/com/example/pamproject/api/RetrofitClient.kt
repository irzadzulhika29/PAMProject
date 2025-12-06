package com.example.pamproject.api

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    const val BASE_URL = "https://ctzndtrcxiqjalyqwkqj.supabase.co/rest/v1/"
    const val STORAGE_BASE_URL = "https://ctzndtrcxiqjalyqwkqj.supabase.co/storage/v1"
    const val PUBLIC_STORAGE_URL = "$STORAGE_BASE_URL/object/public"
    const val WORKOUT_IMAGE_BUCKET = "workout-images"
    const val API_KEY = "sb_publishable_BKrLoFXim8ILj1sJqjOcIA_oK9itaQK"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer $API_KEY")
                .addHeader("Content-Type", "application/json")
                .build()
            chain.proceed(request)
        }
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)
}
