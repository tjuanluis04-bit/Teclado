package com.tecladoapp.keyboard

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/** Campo de texto "falso" (sin EditText) para usar dentro del propio IME. */
class InlineTextField(context: Context, private val hintText: String) : LinearLayout(context), InlineInputHost {

    var onTextChanged: ((String) -> Unit)? = null
    var onEnter: (() -> Unit)? = null
    var onTap: (() -> Unit)? = null

    private val density = context.resources.displayMetrics.density
    private fun px(dp: Float) = (dp * density).toInt()
    private val sb = StringBuilder()
    private var active = false
    private val display = TextView(context)
    /** Espacio donde se monta el teclado; el que lo usa debe añadirlo a su layout (LinearLayout). */
    val keyboardSlot = FrameLayout(context)

    init {
        orientation = VERTICAL
        display.apply {
            textSize = 18f
            minHeight = px(48f)
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(px(16f), px(10f), px(16f), px(10f))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#262626"))
                cornerRadius = px(10f).toFloat()
                setStroke(px(2f), Color.parseColor("#4A4A4A"))
            }
            setOnClickListener { onTap?.invoke() }
        }
        addView(display, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        keyboardSlot.layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0)
        refresh()
    }

    fun text(): String = sb.toString()
    fun setTextValue(t: String) { sb.setLength(0); sb.append(t); refresh() }
    fun appendText(t: String) { sb.append(t); refresh(); onTextChanged?.invoke(sb.toString()) }
    fun clear() { sb.setLength(0); refresh(); onTextChanged?.invoke("") }
    fun setActive(a: Boolean) { active = a; refresh() }

    private fun refresh() {
        if (sb.isEmpty()) {
            display.text = if (active) "|" else hintText
            display.setTextColor(if (active) Color.parseColor("#EAEAEA") else Color.parseColor("#888888"))
        } else {
            display.text = sb.toString() + if (active) "|" else ""
            display.setTextColor(Color.parseColor("#EAEAEA"))
        }
    }

    override fun onInlineChar(c: Char) { sb.append(c); refresh(); onTextChanged?.invoke(sb.toString()) }
    override fun onInlineBackspace() {
        if (sb.isNotEmpty()) sb.deleteCharAt(sb.length - 1)
        refresh(); onTextChanged?.invoke(sb.toString())
    }
    override fun onInlineEnter() { onEnter?.invoke() }
}
