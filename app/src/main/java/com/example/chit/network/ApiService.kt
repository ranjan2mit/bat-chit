package com.example.chit.network

import com.example.chit.model.SendOtpRequest
import com.example.chit.model.SendOtpResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("/auth/send-otp")  // Replace with your actual endpoint
    fun sendOtp(@Body request: SendOtpRequest): Call<SendOtpResponse>
}