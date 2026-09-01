package com.tecladoapp.keyboard

data class ClipItem(
    val id: String,
    val text: String,
    val timestamp: Long,
    var categoryId: String? = null
)

data class ClipCategory(
    val id: String,
    var name: String
)

data class KeyboardTheme(
    val id: String,
    val nameRes: String,
    val keyColor: Int,
    val keyTextColor: Int,
    val backgroundColor: Int,
    val accentColor: Int,
    val textureRes: Int? = null
)

data class KeyboardFont(
    val id: String,
    val displayName: String,
    val fontFamily: String? // null = tipografía del sistema
)
