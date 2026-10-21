package com.tecladoapp.keyboard

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

class SuggestionBarView(context: Context) : LinearLayout(context) {

    var onSuggestionTap: ((String) -> Unit)? = null
    var fontFamily: String? = null

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(8, 0, 8, 0)
    }

    fun setSuggestions(words: List<String>, theme: KeyboardTheme, clip: String? = null, onClip: (() -> Unit)? = null) {
        removeAllViews()
        setBackgroundColor(theme.backgroundColor)
        val tf = fontFamily?.let { android.graphics.Typeface.create(it, android.graphics.Typeface.NORMAL) }
            ?: android.graphics.Typeface.DEFAULT
        val density = context.resources.displayMetrics.density

        if (clip != null) {
            // Cuadro distintivo con lo último copiado: un toque lo pega
            val preview = clip.replace('\n', ' ').trim().let { if (it.length > 28) it.take(27) + "…" else it }
            val chip = TextView(context).apply {
                text = "📋 $preview"
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                textSize = 16f
                maxLines = 1
                typeface = tf
                setPadding((10 * density).toInt(), 0, (10 * density).toInt(), 0)
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(theme.accentColor)
                    cornerRadius = 10 * density
                }
                layoutParams = LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.6f).apply {
                    setMargins((4 * density).toInt(), (6 * density).toInt(), (6 * density).toInt(), (6 * density).toInt())
                }
                setOnClickListener { onClip?.invoke() }
            }
            addView(chip)
        }
        words.take(if (clip != null) 2 else 3).forEach { word ->
            val tv = TextView(context).apply {
                text = word
                gravity = Gravity.CENTER
                setTextColor(theme.keyTextColor)
                textSize = 18f
                maxLines = 1
                typeface = tf
                setPadding(16, 0, 16, 0)
                layoutParams = LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
                setOnClickListener { onSuggestionTap?.invoke(word) }
            }
            addView(tv)
        }
    }
}
