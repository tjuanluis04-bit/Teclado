package com.tecladoapp.keyboard

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

class SuggestionBarView(context: Context) : LinearLayout(context) {

    var onSuggestionTap: ((String) -> Unit)? = null

    init {
        orientation = HORIZONTAL
        setPadding(8, 8, 8, 8)
    }

    fun setSuggestions(words: List<String>, theme: KeyboardTheme) {
        removeAllViews()
        setBackgroundColor(theme.backgroundColor)
        words.take(3).forEach { word ->
            val tv = TextView(context).apply {
                text = word
                gravity = Gravity.CENTER
                setTextColor(theme.keyTextColor)
                textSize = 14f
                setPadding(16, 12, 16, 12)
                layoutParams = LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener { onSuggestionTap?.invoke(word) }
            }
            addView(tv)
        }
    }
}
