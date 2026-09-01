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

    fun show(contentView: View, title: String) {
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = 24f
            }
        }
        val titleView = TextView(context).apply {
            text = title
            textSize = 15f
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

        // Se ancla dentro del propio contentView del panel; usamos showAtLocation vía la vista pasada.
        contentView.post {
            val anchor = contentView.rootView
            pw.showAtLocation(anchor, Gravity.CENTER, 0, 0)
        }
    }

    fun dismiss() {
        popupWindow?.dismiss()
    }
}
