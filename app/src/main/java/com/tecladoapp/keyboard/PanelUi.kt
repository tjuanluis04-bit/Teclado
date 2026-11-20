package com.tecladoapp.keyboard

import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

/** Piezas de interfaz compartidas por los paneles nuevos (barra superior, botones, chips, portapapeles). */
object PanelUi {

    fun dp(context: Context, v: Float) = (v * context.resources.displayMetrics.density).toInt()

    fun topBar(context: Context, title: String, onClose: () -> Unit): LinearLayout {
        val top = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(context, 16f), dp(context, 4f), dp(context, 8f), dp(context, 4f))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 56f))
        }
        top.addView(TextView(context).apply {
            text = title; textSize = 19f
            setTextColor(Color.parseColor("#EAEAEA"))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        top.addView(TextView(context).apply {
            text = "✕"; textSize = 26f
            setTextColor(Color.parseColor("#CCCCCC"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(context, 52f), dp(context, 48f))
            setOnClickListener { onClose() }
        })
        return top
    }

    fun button(context: Context, label: String, color: String, onClick: () -> Unit) = TextView(context).apply {
        text = label; textSize = 16f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
        background = GradientDrawable().apply { setColor(Color.parseColor(color)); cornerRadius = dp(context, 12f).toFloat() }
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
            setMargins(dp(context, 3f), dp(context, 2f), dp(context, 3f), dp(context, 2f))
        }
        setOnClickListener { onClick() }
    }

    /** Chip seleccionable; [selected] cambia el color. */
    fun chip(context: Context, label: String, selected: Boolean, onClick: () -> Unit) = TextView(context).apply {
        text = label; textSize = 15f; gravity = Gravity.CENTER
        setTextColor(Color.WHITE)
        setPadding(dp(context, 14f), 0, dp(context, 14f), 0)
        background = GradientDrawable().apply {
            setColor(Color.parseColor(if (selected) "#4C6FFF" else "#2C2C2C")); cornerRadius = dp(context, 20f).toFloat()
        }
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(context, 38f)).apply {
            setMargins(dp(context, 4f), dp(context, 4f), dp(context, 4f), dp(context, 4f))
        }
        setOnClickListener { onClick() }
    }

    fun clipboardText(context: Context): String {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        return cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
    }

    fun copyToClipboard(context: Context, text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(android.content.ClipData.newPlainText("texto", text))
    }
}
