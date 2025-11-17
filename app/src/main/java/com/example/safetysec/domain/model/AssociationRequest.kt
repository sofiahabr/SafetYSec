package com.example.safetysec.domain.model

data class AssociationRequest(
    val monitorEmail: String,
    val protectedEmail: String,
    val otp: String
)

data class OTPInfo(
    val otp: String,
    val expiresInMinutes: Int = 10
)