package com.tecladoapp.keyboard

const val CODE_SHIFT = -1
const val CODE_BACKSPACE = -2
const val CODE_ENTER = -3
const val CODE_SPACE = -4
const val CODE_SYMBOLS = -5
const val CODE_LETTERS = -6
const val CODE_COMMA = -7
const val CODE_PERIOD = -8
const val CODE_EMOJI = -9
const val CODE_CLIPBOARD = -10

data class Key(
    val code: Int,
    val label: String,
    val altLabel: String? = null, // etiqueta cuando shift está activo
    val weight: Float = 1f,
    val isFunctionKey: Boolean = false
)

object KeyDefs {

    val numberRow = listOf(
        Key(48 + 1, "1"), Key(48 + 2, "2"), Key(48 + 3, "3"), Key(48 + 4, "4"), Key(48 + 5, "5"),
        Key(48 + 6, "6"), Key(48 + 7, "7"), Key(48 + 8, "8"), Key(48 + 9, "9"), Key(48, "0")
    )

    private val row1 = "qwertyuiop"
    private val row2 = "asdfghjkl"
    private val row3 = "zxcvbnm"

    fun lettersRow1() = row1.map { Key(it.code, it.toString(), it.uppercaseChar().toString()) }
    fun lettersRow2() = row2.map { Key(it.code, it.toString(), it.uppercaseChar().toString()) }
    fun lettersRow3() = row3.map { Key(it.code, it.toString(), it.uppercaseChar().toString()) }

    val symbolsRow1 = "@#\$_&-+()".map { Key(it.code, it.toString()) }
    val symbolsRow2 = "*\"':;!?".map { Key(it.code, it.toString()) }
    val symbolsRow3 = "/,.".map { Key(it.code, it.toString()) }
}
