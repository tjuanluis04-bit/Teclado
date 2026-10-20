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
    fun show(contentView: View, title: String, anchor: View, onDismiss: (() -> Unit)? = null) {
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 36, 40, 36)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#2A2A2A"))
                cornerRadius = 24f
            }
        }
        val titleView = TextView(context).apply {
            text = title
            textSize = 18f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 16)
        }
        val header = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        titleView.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        val closeX = TextView(context).apply {
            text = "✕"; textSize = 22f
            setTextColor(Color.parseColor("#CCCCCC"))
            setPadding(24, 0, 8, 16)
            setOnClickListener { dismiss() }
        }
        header.addView(titleView)
        header.addView(closeX)
        card.addView(header)
        card.addView(contentView)

        val pw = PopupWindow(
            card,
            context.resources.displayMetrics.widthPixels,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            false // no enfocable: robar el foco reinicia el IME y causa parpadeo
        )
        pw.isOutsideTouchable = false
        onDismiss?.let { cb -> pw.setOnDismissListener { cb() } }
        popupWindow = pw
        pw.showAtLocation(anchor, Gravity.BOTTOM, 0, 0)
    }

    fun dismiss() {
        popupWindow?.dismiss()
    }
}
