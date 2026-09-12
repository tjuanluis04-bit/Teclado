package com.tecladoapp.keyboard

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView

/** Ventana emergente simple para diálogos rápidos dentro del IME (categorizar, crear, editar). */
class PopupContainer(private val context: Context) {

    private var popupWindow: PopupWindow? = null

    /**
     * @param anchor una vista que YA esté adjunta a la ventana (por ejemplo, el panel
     * que la invoca). Es indispensable pasarla: no se puede usar contentView como ancla
     * porque todavía no está adjunto a nada en este punto.
     */
    fun show(contentView: View, title: String, anchor: View) {
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 28, 32, 28)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#2A2A2A"))
                cornerRadius = 24f
            }
        }
        val titleView = TextView(context).apply {
            text = title
            textSize = 15f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 16)
        }
        card.addView(titleView)
        card.addView(contentView)

        val pw = PopupWindow(
            card,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        pw.isOutsideTouchable = true
        popupWindow = pw
        pw.showAtLocation(anchor, Gravity.CENTER, 0, 0)
    }

    fun dismiss() {
        popupWindow?.dismiss()
    }
}
