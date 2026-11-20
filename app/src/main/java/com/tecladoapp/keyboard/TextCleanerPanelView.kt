package com.tecladoapp.keyboard

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Editor de textos: pegas un texto y lo dejas limpio (sin *, #, saltos de párrafo…) listo para copiar o insertar. */
class TextCleanerPanelView(context: Context, initialText: String) : LinearLayout(context) {

    interface Listener {
        fun onInsert(text: String)
        fun onClose()
    }
    var listener: Listener? = null

    private var source = initialText
    private var noAsterisks = true
    private var noHashes = true
    private var noExtras = false      // _ ` > ~
    private var noBullets = false     // "- " / "• " al inicio de línea
    private var noDoubleSpaces = true
    private var trim = true
    /** 0 = sin cambios, 1 = quitar saltos entre párrafos (todo seguido), 2 = una línea en blanco entre párrafos, 3 = un solo salto entre párrafos */
    private var paragraphMode = 3

    private val chips = LinearLayout(context)
    private val resultView = TextView(context)
    private val info = TextView(context)

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.parseColor("#161616"))
        addView(PanelUi.topBar(context, "Editor de textos") { listener?.onClose() })

        val scroll = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, PanelUi.dp(context, 50f))
        }
        chips.orientation = HORIZONTAL
        chips.setPadding(PanelUi.dp(context, 6f), PanelUi.dp(context, 4f), PanelUi.dp(context, 6f), 0)
        scroll.addView(chips)
        addView(scroll)

        val buttons = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, PanelUi.dp(context, 46f))
            setPadding(PanelUi.dp(context, 4f), 0, PanelUi.dp(context, 4f), 0)
        }
        buttons.addView(PanelUi.button(context, "Pegar", "#6A4C93") {
            val t = PanelUi.clipboardText(context)
            if (t.isNotEmpty()) { source = t; refresh() }
        })
        buttons.addView(PanelUi.button(context, "Copiar", "#4C6FFF") {
            val out = clean()
            if (out.isNotEmpty()) { PanelUi.copyToClipboard(context, out); info.text = "✔ Copiado al portapapeles" }
        })
        buttons.addView(PanelUi.button(context, "Insertar", "#2E7D32") {
            val out = clean()
            if (out.isNotEmpty()) listener?.onInsert(out)
        })
        buttons.addView(PanelUi.button(context, "Borrar", "#7A2E2E") { source = ""; refresh() })
        addView(buttons)

        info.apply {
            textSize = 13f; setTextColor(Color.parseColor("#999999"))
            setPadding(PanelUi.dp(context, 14f), PanelUi.dp(context, 4f), PanelUi.dp(context, 14f), 0)
        }
        addView(info)

        resultView.apply {
            textSize = 16f
            setTextColor(Color.WHITE)
            setTextIsSelectable(false)
            setPadding(PanelUi.dp(context, 12f), PanelUi.dp(context, 10f), PanelUi.dp(context, 12f), PanelUi.dp(context, 10f))
        }
        val sv = ScrollView(context).apply {
            background = GradientDrawable().apply { setColor(Color.parseColor("#232323")); cornerRadius = PanelUi.dp(context, 12f).toFloat() }
            layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f).apply {
                setMargins(PanelUi.dp(context, 8f), PanelUi.dp(context, 4f), PanelUi.dp(context, 8f), PanelUi.dp(context, 8f))
            }
            addView(resultView)
        }
        addView(sv)
        refresh()
    }

    private fun paragraphLabel() = when (paragraphMode) {
        1 -> "Saltos: quitar todos"
        2 -> "Saltos: línea en blanco"
        3 -> "Saltos: uno solo"
        else -> "Saltos: sin cambios"
    }

    private fun refresh() {
        chips.removeAllViews()
        chips.addView(PanelUi.chip(context, "Quitar *", noAsterisks) { noAsterisks = !noAsterisks; refresh() })
        chips.addView(PanelUi.chip(context, "Quitar #", noHashes) { noHashes = !noHashes; refresh() })
        chips.addView(PanelUi.chip(context, paragraphLabel(), paragraphMode != 0) { paragraphMode = (paragraphMode + 1) % 4; refresh() })
        chips.addView(PanelUi.chip(context, "Quitar _ ` > ~", noExtras) { noExtras = !noExtras; refresh() })
        chips.addView(PanelUi.chip(context, "Quitar viñetas", noBullets) { noBullets = !noBullets; refresh() })
        chips.addView(PanelUi.chip(context, "Espacios dobles", noDoubleSpaces) { noDoubleSpaces = !noDoubleSpaces; refresh() })
        chips.addView(PanelUi.chip(context, "Recortar", trim) { trim = !trim; refresh() })
        val out = clean()
        if (source.isEmpty()) {
            resultView.text = "Pulsa «Pegar» para traer el texto copiado. Aquí verás el resultado limpio."
            resultView.setTextColor(Color.parseColor("#888888"))
            info.text = ""
        } else {
            resultView.text = out
            resultView.setTextColor(Color.WHITE)
            info.text = "${source.length} → ${out.length} caracteres. Toca los botones de arriba para cambiar la limpieza."
        }
    }

    fun clean(): String {
        var t = source.replace("\r\n", "\n").replace('\r', '\n')
        if (noAsterisks) t = t.replace("*", "")
        if (noHashes) t = t.replace("#", "")
        if (noExtras) t = t.replace(Regex("[_`~]"), "").replace(Regex("(?m)^\\s*>+ ?"), "")
        if (noBullets) t = t.replace(Regex("(?m)^[ \\t]*[-•–·] +"), "")
        if (noDoubleSpaces) t = t.replace(Regex("[ \\t]{2,}"), " ").replace(Regex("(?m)[ \\t]+$"), "").replace(Regex("(?m)^[ \\t]+"), "")
        when (paragraphMode) {
            1 -> t = t.replace(Regex("\\n+"), " ").replace(Regex(" {2,}"), " ")
            2 -> t = t.replace(Regex("\\n{2,}"), "\n").replace("\n", "\n\n")
            3 -> t = t.replace(Regex("\\n{2,}"), "\n")
        }
        if (trim) t = t.trim()
        return t
    }
}
