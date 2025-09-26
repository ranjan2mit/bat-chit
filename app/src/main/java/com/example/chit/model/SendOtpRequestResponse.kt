package com.example.chit.model

data class SendOtpRequest(
    val phone: String
)

data class SendOtpResponse(
    val message: String,
    val status: String
)