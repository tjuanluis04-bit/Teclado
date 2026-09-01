package com.tecladoapp.keyboard

data class EmojiEntry(val emoji: String, val keywords: List<String>)

object EmojiData {
    val all: List<EmojiEntry> = listOf(
        EmojiEntry("😀", listOf("feliz", "sonrisa", "contento")),
        EmojiEntry("😂", listOf("risa", "llorar de risa", "jaja")),
        EmojiEntry("🥰", listOf("amor", "enamorado", "cariño")),
        EmojiEntry("😍", listOf("amor", "ojos de corazon")),
        EmojiEntry("😘", listOf("beso")),
        EmojiEntry("😉", listOf("guiño")),
        EmojiEntry("😎", listOf("genial", "lentes de sol")),
        EmojiEntry("🤔", listOf("pensando", "duda")),
        EmojiEntry("😢", listOf("triste", "llorar")),
        EmojiEntry("😭", listOf("llorar mucho", "triste")),
        EmojiEntry("😡", listOf("enojado", "furioso")),
        EmojiEntry("😱", listOf("miedo", "sorpresa")),
        EmojiEntry("🥳", listOf("fiesta", "celebrar")),
        EmojiEntry("😴", listOf("dormir", "sueño")),
        EmojiEntry("🤗", listOf("abrazo")),
        EmojiEntry("👍", listOf("bien", "like", "pulgar arriba")),
        EmojiEntry("👎", listOf("mal", "pulgar abajo")),
        EmojiEntry("👏", listOf("aplausos", "bravo")),
        EmojiEntry("🙏", listOf("gracias", "por favor", "rezar")),
        EmojiEntry("💪", listOf("fuerza", "musculo")),
        EmojiEntry("👋", listOf("hola", "adios", "saludo")),
        EmojiEntry("✌️", listOf("paz", "victoria")),
        EmojiEntry("🤝", listOf("trato", "acuerdo")),
        EmojiEntry("❤️", listOf("amor", "corazon rojo")),
        EmojiEntry("💔", listOf("corazon roto", "triste")),
        EmojiEntry("🔥", listOf("fuego", "genial")),
        EmojiEntry("⭐", listOf("estrella")),
        EmojiEntry("✨", listOf("brillo", "magia")),
        EmojiEntry("🎉", listOf("fiesta", "celebracion")),
        EmojiEntry("🎂", listOf("cumpleaños", "pastel")),
        EmojiEntry("🎁", listOf("regalo")),
        EmojiEntry("☀️", listOf("sol", "clima")),
        EmojiEntry("🌧️", listOf("lluvia")),
        EmojiEntry("🌙", listOf("luna", "noche")),
        EmojiEntry("☕", listOf("café")),
        EmojiEntry("🍕", listOf("pizza", "comida")),
        EmojiEntry("🍔", listOf("hamburguesa")),
        EmojiEntry("🍎", listOf("manzana", "fruta")),
        EmojiEntry("🐶", listOf("perro")),
        EmojiEntry("🐱", listOf("gato")),
        EmojiEntry("🚗", listOf("carro", "auto")),
        EmojiEntry("✈️", listOf("avion", "viaje")),
        EmojiEntry("🏠", listOf("casa")),
        EmojiEntry("📱", listOf("celular", "telefono")),
        EmojiEntry("💻", listOf("computadora", "laptop")),
        EmojiEntry("📷", listOf("cámara", "foto")),
        EmojiEntry("🎵", listOf("música", "nota")),
        EmojiEntry("⚽", listOf("futbol", "balon")),
        EmojiEntry("🏀", listOf("basquetbol")),
        EmojiEntry("💰", listOf("dinero")),
        EmojiEntry("⏰", listOf("reloj", "alarma")),
        EmojiEntry("✅", listOf("check", "listo", "hecho")),
        EmojiEntry("❌", listOf("no", "equis", "error")),
        EmojiEntry("❓", listOf("pregunta", "duda")),
        EmojiEntry("❗", listOf("exclamación", "importante"))
    )

    fun search(query: String): List<EmojiEntry> {
        if (query.isBlank()) return all
        val q = query.lowercase()
        return all.filter { entry -> entry.keywords.any { it.contains(q) } || entry.emoji == q }
    }
}
