package com.example.safetysec.domain.model

import java.util.Date

data class Association(
    val id: String = "",
    val monitorId: String = "",
    val monitorName: String = "",
    val monitorEmail: String = "",
    val protectedId: String = "",
    val protectedName: String = "",
    val protectedEmail: String = "",
    val status: AssociationStatus = AssociationStatus.PENDING,
    val otp: String? = null,
    val otpExpiresAt: Date? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

enum class AssociationStatus {
    PENDING,    // OTP generated, waiting for acceptance
    ACTIVE,     // Association confirmed and active
    CANCELLED   // Association terminated
}