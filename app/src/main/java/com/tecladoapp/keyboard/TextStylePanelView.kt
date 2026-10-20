package com.tecladoapp.keyboard

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Elegir un estilo: a partir de ahí, lo que escribas se convierte en ese estilo. */
class TextStylePanelView(context: Context, private val activeId: String?) : LinearLayout(context) {

    interface Listener {
        fun onStyleChosen(style: TextStyle?)
        fun onClose()
    }
    var listener: Listener? = null

    private val density = context.resources.displayMetrics.density
    private fun px(dp: Float) = (dp * density).toInt()

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.parseColor("#161616"))

        val top = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(px(16f), px(6f), px(8f), px(6f))
        }
        top.addView(TextView(context).apply {
            text = "Estilo de texto"; textSize = 19f
            setTextColor(Color.parseColor("#EAEAEA"))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        top.addView(TextView(context).apply {
            text = "✕"; textSize = 26f
            setTextColor(Color.parseColor("#CCCCCC"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(px(52f), px(48f))
            setOnClickListener { listener?.onClose() }
        })
        top.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(56f))
        addView(top)

        val list = LinearLayout(context).apply { orientation = VERTICAL; setPadding(px(10f), 0, px(10f), px(10f)) }
        list.addView(row("Normal (sin estilo)", "Hola mundo 123", activeId == null) { listener?.onStyleChosen(null) })
        TextStyles.styles.forEach { st ->
            list.addView(row(st.name, TextStyles.convert(st, "Hola mundo 123"), st.id == activeId) { listener?.onStyleChosen(st) })
        }
        val scroll = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            addView(list)
        }
        addView(scroll)
    }

    private fun row(name: String, preview: String, selected: Boolean, onClick: () -> Unit): LinearLayout {
        return LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(px(16f), px(10f), px(16f), px(10f))
            background = GradientDrawable().apply {
                setColor(Color.parseColor(if (selected) "#33405F" else "#222222"))
                cornerRadius = px(12f).toFloat()
                if (selected) setStroke(px(2f), Color.parseColor("#6C8CFF"))
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { setMargins(0, px(4f), 0, px(4f)) }
            addView(TextView(context).apply { text = name; textSize = 14f; setTextColor(Color.parseColor("#999999")) })
            addView(TextView(context).apply { text = preview; textSize = 24f; setTextColor(Color.WHITE) })
            setOnClickListener { onClick() }
        }
    }
}
