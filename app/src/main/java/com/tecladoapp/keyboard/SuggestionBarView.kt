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
        gravity = Gravity.CENTER_VERTICAL
        setPadding(8, 0, 8, 0)
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
                maxLines = 1
                setPadding(16, 0, 16, 0)
                layoutParams = LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
                setOnClickListener { onSuggestionTap?.invoke(word) }
            }
            addView(tv)
        }
    }
}
