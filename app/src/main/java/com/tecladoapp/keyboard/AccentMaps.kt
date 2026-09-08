package com.tecladoapp.keyboard

/**
 * Caracteres alternativos que aparecen en un popup al mantener presionada una tecla,
 * deslizando el dedo para elegir uno.
 */
object AccentMaps {

    val numbers: Map<Char, List<Char>> = mapOf(
        '1' to listOf('¹', '½', '⅓', '¼', '⅛'),
        '2' to listOf('²', '⅔'),
        '3' to listOf('³', '¾', '⅜'),
        '4' to listOf('⁴'),
        '5' to listOf('⅝'),
        '7' to listOf('⅞'),
        '0' to listOf('ⁿ', '∅')
    )

    val letters: Map<Char, List<Char>> = mapOf(
        'q' to listOf('%'),
        'w' to listOf('\\'),
        'e' to listOf('|', 'é', 'ë', 'è', 'ė', 'ê', 'ę', 'ē'),
        'r' to listOf('='),
        't' to listOf('['),
        'y' to listOf(']'),
        'u' to listOf('<', 'ú', 'ü', 'û', 'ù', 'ū'),
        'i' to listOf('>', 'í', 'ï', 'ì', 'ī', 'î', 'į'),
        'o' to listOf('{', 'ó', 'ò', 'ö', 'ô', 'õ', 'ø', 'œ', 'ō', 'º'),
        'p' to listOf('}'),
        'a' to listOf('@', 'à', 'á', 'ä', 'â', 'ã', 'å', 'ą', 'æ', 'ā', 'ª'),
        's' to listOf('#'),
        'd' to listOf('$'),
        'f' to listOf('_'),
        'g' to listOf('&'),
        'h' to listOf('-'),
        'j' to listOf('+'),
        'k' to listOf('('),
        'l' to listOf(')'),
        'z' to listOf('*'),
        'x' to listOf('"'),
        'c' to listOf('ć', '\'', 'ç', 'č'),
        'v' to listOf(':'),
        'b' to listOf(';'),
        'n' to listOf('ń', '!', 'ñ'),
        'm' to listOf('?')
    )

    val symbols: Map<Char, List<Char>> = mapOf(
        '%' to listOf('‰'),
        '$' to listOf('£', '¢', '¥', '€', '₱'),
        '_' to listOf('‰'),
        '-' to listOf('·', '–', '—'),
        '+' to listOf('±'),
        '*' to listOf('★', '†', '‡'),
        '"' to listOf('„', '“', '”', '«', '»'),
        '\'' to listOf('‚', '‘', '’', '‹', '›'),
        '!' to listOf('¡'),
        '?' to listOf('¿'),
        '.' to listOf('…')
    )

    fun forKey(code: Int, isFunctionKey: Boolean): List<Char> {
        if (isFunctionKey) return emptyList()
        val c = code.toChar()
        return when {
            c.isDigit() -> numbers[c] ?: emptyList()
            c.isLetter() -> letters[c.lowercaseChar()] ?: emptyList()
            else -> symbols[c] ?: emptyList()
        }
    }
}
