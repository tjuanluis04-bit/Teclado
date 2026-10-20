package com.tecladoapp.keyboard

import android.view.ViewGroup

/** Recibe el texto tecleado cuando el teclado real está reempotrado dentro de un panel. */
interface InlineInputHost {
    fun onInlineChar(c: Char)
    fun onInlineBackspace()
    fun onInlineEnter()
}

/**
 * Un IME no puede invocarse a sí mismo para escribir en un EditText propio, así que
 * los paneles piden prestado el teclado real y lo montan dentro de su propio espacio.
 */
interface InlineKeyboardProvider {
    /** Monta el teclado real en [slot]. [extraHeightPx]>0 agranda el área del IME esa cantidad
     *  (para paneles incrustados); 0 para ventanas emergentes, que ya se ajustan solas. */
    fun attachInlineKeyboard(slot: ViewGroup, host: InlineInputHost, extraHeightPx: Int = 0)
    fun detachInlineKeyboard()
}
