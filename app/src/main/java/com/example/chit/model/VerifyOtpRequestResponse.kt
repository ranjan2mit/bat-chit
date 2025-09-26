package com.example.chit.model

data class VerifyOtpRequest(
    val phone: String,
    val otp: String
)

data class VerifyOtpResponse(
    val status: String,
    val token: String?
)
