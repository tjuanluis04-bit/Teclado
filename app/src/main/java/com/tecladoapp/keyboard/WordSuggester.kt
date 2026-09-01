package com.tecladoapp.keyboard

/**
 * Sugeridor de palabras y corrector ortográfico simple basado en un diccionario
 * embebido de palabras frecuentes en español + distancia de edición (Levenshtein)
 * para proponer correcciones cuando la palabra escrita no existe en el diccionario.
 */
object WordSuggester {

    // Diccionario compacto de palabras muy frecuentes en español.
    private val dictionary: Set<String> = setOf(
        "el","la","de","que","y","a","en","un","ser","se","no","haber","por","con","su",
        "para","como","estar","tener","le","lo","lo","todo","pero","más","hacer","o","poder",
        "decir","este","ir","otro","ese","si","me","ya","ver","porque","dar","cuando","muy",
        "sin","vez","mucho","saber","qué","sobre","mi","alguno","mismo","yo","también","hasta",
        "año","dos","querer","entre","así","primero","desde","grande","eso","ni","nos","llegar",
        "pasar","tiempo","ella","siempre","día","tanto","ella","hola","gracias","por favor",
        "buenos","días","tardes","noches","adiós","nombre","casa","trabajo","teléfono","correo",
        "mensaje","teclado","aplicación","favorito","reciente","categoría","buscar","copiar",
        "pegar","emoji","tema","color","tamaño","ajustar","fuente","letra","número","hoy",
        "mañana","ayer","semana","mes","gente","persona","amigo","familia","cosa","lugar",
        "vida","mundo","mano","parte","ojo","palabra","caso","punto","forma","problema"
    )

    fun suggestionsFor(prefix: String, limit: Int = 3): List<String> {
        if (prefix.isBlank()) return emptyList()
        val lower = prefix.lowercase()
        return dictionary.filter { it.startsWith(lower) && it != lower }
            .sortedBy { it.length }
            .take(limit)
    }

    /** Devuelve true si la palabra se considera correctamente escrita. */
    fun isSpelledCorrectly(word: String): Boolean {
        if (word.isBlank()) return true
        val lower = word.lowercase()
        if (lower.length <= 2) return true
        return dictionary.contains(lower)
    }

    fun spellingCorrections(word: String, limit: Int = 3): List<String> {
        val lower = word.lowercase()
        return dictionary
            .map { it to levenshtein(lower, it) }
            .filter { it.second <= 2 }
            .sortedBy { it.second }
            .take(limit)
            .map { it.first }
    }

    private fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
            }
        }
        return dp[a.length][b.length]
    }
}
