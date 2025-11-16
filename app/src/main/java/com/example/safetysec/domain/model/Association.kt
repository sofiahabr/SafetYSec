package com.example.safetysec.domain.model

import java.util.Date

data class Association(
    val id: String = "",
    val monitorId: String,
    val protectedId: String,
    val status: AssociationStatus,
    val otp: String? = null, // Only stored temporarily
    val otpExpiresAt: Date? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

enum class AssociationStatus {
    PENDING,    // OTP generated, waiting for acceptance
    ACTIVE,     // Association confirmed and active
    CANCELLED   // Association terminated
}