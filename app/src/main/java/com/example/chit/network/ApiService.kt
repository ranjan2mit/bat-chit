package com.example.chit.network

import com.example.chit.model.SendOtpRequest
import com.example.chit.model.SendOtpResponse
import com.example.chit.model.VerifyOtpRequest
import com.example.chit.model.VerifyOtpResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("/auth/send-otp")
    fun sendOtp(@Body request: SendOtpRequest): Call<SendOtpResponse>

    @POST("/auth/verify-otp")
    fun verifyOtp(@Body request: VerifyOtpRequest): Call<VerifyOtpResponse>
}