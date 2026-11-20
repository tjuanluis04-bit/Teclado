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
        KeyboardTheme("rosa_gold", "Rosa Gold", Color.parseColor("#FFFFFF"), Color.parseColor("#5C4033"), Color.parseColor("#FFE8E0"), Color.parseColor("#D4A373")),
        KeyboardTheme("medianoche", "Medianoche", Color.parseColor("#2A3350"), Color.parseColor("#E6ECFF"), Color.parseColor("#0E1426"), Color.parseColor("#5C7CFA")),
        KeyboardTheme("bosque", "Bosque", Color.parseColor("#2F4F3A"), Color.parseColor("#E8F5E9"), Color.parseColor("#16281D"), Color.parseColor("#66BB6A")),
        KeyboardTheme("oceano", "Océano", Color.parseColor("#1E5F74"), Color.parseColor("#E0F7FA"), Color.parseColor("#0B2B36"), Color.parseColor("#26C6DA")),
        KeyboardTheme("lavanda", "Lavanda", Color.parseColor("#7E6BB5"), Color.parseColor("#FFFFFF"), Color.parseColor("#3D3260"), Color.parseColor("#C3A6FF")),
        KeyboardTheme("cereza", "Cereza", Color.parseColor("#7A1F36"), Color.parseColor("#FFEDEF"), Color.parseColor("#3B0D1A"), Color.parseColor("#FF5C7A")),
        KeyboardTheme("dorado", "Negro y Dorado", Color.parseColor("#1F1B12"), Color.parseColor("#F5D78A"), Color.parseColor("#0C0A06"), Color.parseColor("#D4AF37")),
        KeyboardTheme("hielo", "Hielo", Color.parseColor("#FFFFFF"), Color.parseColor("#12384F"), Color.parseColor("#CFE8F5"), Color.parseColor("#4AA3D8")),
        KeyboardTheme("carbon", "Carbón Rojo", Color.parseColor("#2C2C2C"), Color.parseColor("#FFFFFF"), Color.parseColor("#141414"), Color.parseColor("#E53935")),
        KeyboardTheme("arena", "Arena", Color.parseColor("#F7EBD3"), Color.parseColor("#5A4524"), Color.parseColor("#D9C39A"), Color.parseColor("#C68B3E")),
        KeyboardTheme("cyberpunk", "Cyberpunk", Color.parseColor("#1B1035"), Color.parseColor("#F9F871"), Color.parseColor("#0A0618"), Color.parseColor("#00E5FF")),
        KeyboardTheme("sakura", "Sakura", Color.parseColor("#FFF5F8"), Color.parseColor("#7A2E4A"), Color.parseColor("#FFD6E4"), Color.parseColor("#F06292")),
        KeyboardTheme("volcan", "Volcán", Color.parseColor("#3A1F1A"), Color.parseColor("#FFE0B2"), Color.parseColor("#1C0D0A"), Color.parseColor("#FF7043")),
        KeyboardTheme("esmeralda", "Esmeralda", Color.parseColor("#0F5132"), Color.parseColor("#D1F7E3"), Color.parseColor("#062B1B"), Color.parseColor("#1DE9B6")),
        KeyboardTheme("limon", "Limón", Color.parseColor("#FFFDE7"), Color.parseColor("#4D4400"), Color.parseColor("#F4EE9A"), Color.parseColor("#C0CA33")),
        KeyboardTheme("cielo", "Cielo Claro", Color.parseColor("#FFFFFF"), Color.parseColor("#0D3B66"), Color.parseColor("#BFE3FF"), Color.parseColor("#2196F3")),
        KeyboardTheme("uva", "Uva", Color.parseColor("#4A2C6B"), Color.parseColor("#F3E5FF"), Color.parseColor("#22103A"), Color.parseColor("#AB47BC")),
        KeyboardTheme("cafe", "Café", Color.parseColor("#6F4E37"), Color.parseColor("#FFF3E0"), Color.parseColor("#3B2A1E"), Color.parseColor("#D7A86E")),
        KeyboardTheme("grafito", "Grafito Azul", Color.parseColor("#37474F"), Color.parseColor("#ECEFF1"), Color.parseColor("#1C262B"), Color.parseColor("#40C4FF")),
        KeyboardTheme("coral", "Coral", Color.parseColor("#FFFFFF"), Color.parseColor("#5A1F1A"), Color.parseColor("#FFCFC2"), Color.parseColor("#FF5252")),
        KeyboardTheme("blanco_puro", "Blanco Puro", Color.parseColor("#FFFFFF"), Color.parseColor("#212121"), Color.parseColor("#F2F2F2"), Color.parseColor("#616161")),
        KeyboardTheme("negro_puro", "Negro Puro (AMOLED)", Color.parseColor("#1A1A1A"), Color.parseColor("#FFFFFF"), Color.parseColor("#000000"), Color.parseColor("#BB86FC")),
        KeyboardTheme("aurora", "Aurora", Color.parseColor("#1B3A4B"), Color.parseColor("#C8FFF4"), Color.parseColor("#0B1D26"), Color.parseColor("#7CFFCB")),
        KeyboardTheme("durazno", "Durazno", Color.parseColor("#FFF1E6"), Color.parseColor("#6B3E26"), Color.parseColor("#FFCBA4"), Color.parseColor("#FF8A65"))
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
