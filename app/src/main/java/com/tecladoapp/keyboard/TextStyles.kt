package com.tecladoapp.keyboard

import java.text.Normalizer

/** Estilos de texto con caracteres Unicode (se ven igual en cualquier celular y red social). */
class TextStyle(val id: String, val name: String, val map: (Char) -> String?)

object TextStyles {

    private fun alpha(upper: Int, lower: Int, digit: Int? = null): (Char) -> String? = { c ->
        when (c) {
            in 'A'..'Z' -> String(Character.toChars(upper + (c - 'A')))
            in 'a'..'z' -> String(Character.toChars(lower + (c - 'a')))
            in '0'..'9' -> if (digit != null) String(Character.toChars(digit + (c - '0'))) else null
            else -> null
        }
    }

    private val superLower = "ᵃᵇᶜᵈᵉᶠᵍʰⁱʲᵏˡᵐⁿᵒᵖqʳˢᵗᵘᵛʷˣʸᶻ"
    private val superDigits = "⁰¹²³⁴⁵⁶⁷⁸⁹"

    /** Letras (mayús. y minús. iguales) a partir de un punto de código base. */
    private fun sameCase(base: Int): (Char) -> String? = { c ->
        when (c) {
            in 'A'..'Z' -> String(Character.toChars(base + (c - 'A')))
            in 'a'..'z' -> String(Character.toChars(base + (c - 'a')))
            else -> null
        }
    }

    private fun combining(cp: Int): (Char) -> String? = { c -> if (c.isLetterOrDigit()) c.toString() + String(Character.toChars(cp)) else null }

    private val smallCaps = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀꜱᴛᴜᴠᴡxʏᴢ"

    val styles: List<TextStyle> = listOf(
        TextStyle("bold", "Negrita", alpha(0x1D5D4, 0x1D5EE, 0x1D7EC)),
        TextStyle("italic", "Cursiva", alpha(0x1D608, 0x1D622)),
        TextStyle("bolditalic", "Negrita cursiva", alpha(0x1D63C, 0x1D656, 0x1D7EC)),
        TextStyle("serifbold", "Serif negrita", alpha(0x1D400, 0x1D41A, 0x1D7CE)),
        TextStyle("script", "Caligrafía", alpha(0x1D4D0, 0x1D4EA)),
        TextStyle("fraktur", "Gótica", alpha(0x1D56C, 0x1D586)),
        TextStyle("double", "Doble trazo", alpha(0x1D538, 0x1D552, 0x1D7D8)),
        TextStyle("mono", "Máquina de escribir", alpha(0x1D670, 0x1D68A, 0x1D7F6)),
        TextStyle("circled", "Circulado", { c ->
            when (c) {
                in 'A'..'Z' -> String(Character.toChars(0x24B6 + (c - 'A')))
                in 'a'..'z' -> String(Character.toChars(0x24D0 + (c - 'a')))
                '0' -> "⓪"
                in '1'..'9' -> String(Character.toChars(0x2460 + (c - '1')))
                else -> null
            }
        }),
        TextStyle("full", "Ancho completo", alpha(0xFF21, 0xFF41, 0xFF10)),
        TextStyle("smallcaps", "Versalitas", { c ->
            when (c) {
                in 'a'..'z' -> smallCaps[c - 'a'].toString()
                in 'A'..'Z' -> smallCaps[c - 'A'].toString()
                else -> null
            }
        }),
        TextStyle("strike", "Tachado", { c -> if (c.isLetterOrDigit()) c.toString() + String(Character.toChars(0x336)) else null }),
        TextStyle("underline", "Subrayado", { c -> if (c.isLetterOrDigit()) c.toString() + String(Character.toChars(0x332)) else null }),
        TextStyle("sans", "Sans simple", alpha(0x1D5A0, 0x1D5BA, 0x1D7E2)),
        TextStyle("serifitalic", "Serif cursiva", { c ->
            if (c == 'h') "ℎ" else alpha(0x1D434, 0x1D44E).invoke(c)
        }),
        TextStyle("serifbolditalic", "Serif negrita cursiva", alpha(0x1D468, 0x1D482, 0x1D7CE)),
        TextStyle("squares", "Cuadrado", sameCase(0x1F130)),
        TextStyle("negsquares", "Cuadrado negro", sameCase(0x1F170)),
        TextStyle("negcircles", "Burbuja negra", sameCase(0x1F150)),
        TextStyle("paren", "Entre paréntesis", { c ->
            when (c) {
                in 'A'..'Z' -> String(Character.toChars(0x249C + (c - 'A')))
                in 'a'..'z' -> String(Character.toChars(0x249C + (c - 'a')))
                else -> null
            }
        }),
        TextStyle("superscript", "Superíndice", { c ->
            when (c) {
                in 'a'..'z' -> superLower[c - 'a'].toString()
                in 'A'..'Z' -> superLower[c - 'A'].toString()
                in '0'..'9' -> superDigits[c - '0'].toString()
                else -> null
            }
        }),
        TextStyle("spaced", "Espaciado", { c -> if (c.isLetterOrDigit()) c.toString() + "\u2009" else null }),
        TextStyle("dunderline", "Doble subrayado", combining(0x333)),
        TextStyle("overline", "Línea arriba", combining(0x305)),
        TextStyle("wavy", "Ondulado", combining(0x330)),
        TextStyle("wavystrike", "Tachado ondulado", combining(0x334)),
        TextStyle("dotted", "Con puntos", combining(0x323))
    )

    fun byId(id: String?) = styles.find { it.id == id }

    /** Convierte un carácter (respetando acentos: la base se estila y la marca se conserva). */
    fun applyChar(style: TextStyle, c: Char): String {
        if (!c.isLetterOrDigit()) return c.toString()
        val decomposed = Normalizer.normalize(c.toString(), Normalizer.Form.NFD)
        val base = decomposed[0]
        val marks = decomposed.substring(1)
        val styled = style.map(base) ?: return c.toString()
        return styled + marks
    }

    fun convert(style: TextStyle, text: String): String =
        buildString { text.forEach { append(applyChar(style, it)) } }
}
