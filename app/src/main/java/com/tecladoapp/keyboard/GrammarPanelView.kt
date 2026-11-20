package com.tecladoapp.keyboard

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Pegas un texto y obtienes la lista de sustantivos, adjetivos, verbos y adverbios (aproximada, en español). */
class GrammarPanelView(context: Context, initialText: String) : LinearLayout(context) {

    interface Listener {
        fun onInsert(text: String)
        fun onClose()
    }
    var listener: Listener? = null

    private var source = initialText
    private var result: Grammar.Result? = null
    private var tab = 0
    private val chips = LinearLayout(context)
    private val listView = TextView(context)
    private val info = TextView(context)

    private val names = listOf("Sustantivos", "Adjetivos", "Verbos", "Adverbios")

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.parseColor("#161616"))
        addView(PanelUi.topBar(context, "Categorías gramaticales") { listener?.onClose() })

        val buttons = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, PanelUi.dp(context, 46f))
            setPadding(PanelUi.dp(context, 4f), 0, PanelUi.dp(context, 4f), 0)
        }
        buttons.addView(PanelUi.button(context, "Pegar texto", "#6A4C93") {
            val t = PanelUi.clipboardText(context)
            if (t.isNotBlank()) { source = t; analyze() }
        })
        buttons.addView(PanelUi.button(context, "Copiar lista", "#4C6FFF") {
            val t = currentList().joinToString("\n")
            if (t.isNotEmpty()) { PanelUi.copyToClipboard(context, t); info.text = "✔ Lista de ${names[tab].lowercase()} copiada" }
        })
        buttons.addView(PanelUi.button(context, "Insertar", "#2E7D32") {
            val t = currentList().joinToString(", ")
            if (t.isNotEmpty()) listener?.onInsert(t)
        })
        addView(buttons)

        val scroll = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, PanelUi.dp(context, 50f))
        }
        chips.orientation = HORIZONTAL
        chips.setPadding(PanelUi.dp(context, 6f), PanelUi.dp(context, 4f), PanelUi.dp(context, 6f), 0)
        scroll.addView(chips)
        addView(scroll)

        info.apply {
            textSize = 12f; setTextColor(Color.parseColor("#888888"))
            setPadding(PanelUi.dp(context, 14f), 0, PanelUi.dp(context, 14f), PanelUi.dp(context, 2f))
        }
        addView(info)

        listView.apply {
            textSize = 17f; setTextColor(Color.WHITE)
            setLineSpacing(0f, 1.15f)
            setPadding(PanelUi.dp(context, 12f), PanelUi.dp(context, 10f), PanelUi.dp(context, 12f), PanelUi.dp(context, 10f))
        }
        val sv = ScrollView(context).apply {
            background = GradientDrawable().apply { setColor(Color.parseColor("#232323")); cornerRadius = PanelUi.dp(context, 12f).toFloat() }
            layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f).apply {
                setMargins(PanelUi.dp(context, 8f), PanelUi.dp(context, 2f), PanelUi.dp(context, 8f), PanelUi.dp(context, 8f))
            }
            addView(listView)
        }
        addView(sv)
        if (source.isNotBlank()) analyze() else render()
    }

    private fun currentList(): List<String> {
        val r = result ?: return emptyList()
        return when (tab) { 0 -> r.nouns; 1 -> r.adjectives; 2 -> r.verbs; else -> r.adverbs }
    }

    private fun analyze() {
        result = Grammar.analyze(source)
        render()
    }

    private fun render() {
        chips.removeAllViews()
        val r = result
        val counts = if (r == null) listOf(0, 0, 0, 0) else listOf(r.nouns.size, r.adjectives.size, r.verbs.size, r.adverbs.size)
        names.forEachIndexed { i, n -> chips.addView(PanelUi.chip(context, "$n (${counts[i]})", i == tab) { tab = i; render() }) }
        if (r == null) {
            listView.text = "Pulsa «Pegar texto» para analizar el texto que tengas copiado (en español)."
            listView.setTextColor(Color.parseColor("#888888"))
            info.text = ""
        } else {
            val l = currentList()
            listView.text = if (l.isEmpty()) "No se encontraron ${names[tab].lowercase()}." else l.joinToString("\n")
            listView.setTextColor(if (l.isEmpty()) Color.parseColor("#888888") else Color.WHITE)
            if (!info.text.startsWith("✔")) info.text = "Clasificación aproximada: algunas palabras ambiguas pueden quedar en otra lista."
        }
    }
}
