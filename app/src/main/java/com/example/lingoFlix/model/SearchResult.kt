package com.example.lingoFlix.model

data class SearchResult(
    val id: Int,
    val title: String,
    val imageUrl: String?,
    val description: String,
    val mediaType: String
)
