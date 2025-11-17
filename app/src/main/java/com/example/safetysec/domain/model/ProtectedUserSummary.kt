package com.example.safetysec.domain.model

data class ProtectedUserSummary(
    val id: String,
    val name: String,
    val email: String,
    val isActive: Boolean
)