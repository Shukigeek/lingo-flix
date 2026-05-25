package com.example.lingoFlix.model

data class UserProfile(
    val id: String,
    val name: String,
    val avatarRes: Int,
    val totalXP: Int = 0,
    val currentStreak: Int = 0
)
