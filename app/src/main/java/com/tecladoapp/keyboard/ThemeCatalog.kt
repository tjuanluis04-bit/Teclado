package com.tecladoapp.keyboard

import android.graphics.Color

object ThemeCatalog {

    val themes = listOf(
        KeyboardTheme("clasico_azul", "Clásico Azul", Color.parseColor("#FFFFFF"), Color.parseColor("#1A1A2E"), Color.parseColor("#E8ECFB"), Color.parseColor("#4C6FFF")),
        KeyboardTheme("modo_oscuro", "Modo Oscuro", Color.parseColor("#2B2D42"), Color.parseColor("#FFFFFF"), Color.parseColor("#121212"), Color.parseColor("#4C6FFF")),
        KeyboardTheme("pastel", "Pastel", Color.parseColor("#FFFFFF"), Color.parseColor("#5B4B8A"), Color.parseColor("#F3E8FF"), Color.parseColor("#FFB6C1")),
        KeyboardTheme("neon", "Neón", Color.parseColor("#0F0F1A"), Color.parseColor("#2FE0C5"), Color.parseColor("#05050A"), Color.parseColor("#FF2FD0")),
        KeyboardTheme("madera", "Textura Madera", Color.parseColor("#8B5E34"), Color.parseColor("#FFF6E9"), Color.parseColor("#5C3A21"), Color.parseColor("#D9A15B")),
        KeyboardTheme("menta", "Menta", Color.parseColor("#FFFFFF"), Color.parseColor("#0B5B4C"), Color.parseColor("#DFF6F0"), Color.parseColor("#2FBF9F")),
        KeyboardTheme("atardecer", "Atardecer", Color.parseColor("#FFFFFF"), Color.parseColor("#4A1E3D"), Color.parseColor("#FFD9A0"), Color.parseColor("#FF6F61")),
        KeyboardTheme("monocromo", "Monocromo", Color.parseColor("#3A3A3A"), Color.parseColor("#FFFFFF"), Color.parseColor("#1A1A1A"), Color.parseColor("#9E9E9E")),
        KeyboardTheme("piedra", "Textura Piedra", Color.parseColor("#7A7A72"), Color.parseColor("#FFFFFF"), Color.parseColor("#4A4A44"), Color.parseColor("#C7C7B8")),
        KeyboardTheme("rosa_gold", "Rosa Gold", Color.parseColor("#FFFFFF"), Color.parseColor("#5C4033"), Color.parseColor("#FFE8E0"), Color.parseColor("#D4A373"))
    )

    val fonts = listOf(
        KeyboardFont("sistema", "Sistema (predeterminada)", null),
        KeyboardFont("sans_serif", "Sans Serif", "sans-serif"),
        KeyboardFont("sans_serif_medium", "Sans Serif Medium", "sans-serif-medium"),
        KeyboardFont("sans_serif_light", "Sans Serif Light", "sans-serif-light"),
        KeyboardFont("sans_serif_condensed", "Condensada", "sans-serif-condensed"),
        KeyboardFont("serif", "Serif", "serif"),
        KeyboardFont("monospace", "Monoespaciada", "monospace"),
        KeyboardFont("cursive", "Cursiva", "cursive")
    )

    fun theme(id: String) = themes.find { it.id == id } ?: themes[0]
    fun font(id: String) = fonts.find { it.id == id } ?: fonts[0]
}
