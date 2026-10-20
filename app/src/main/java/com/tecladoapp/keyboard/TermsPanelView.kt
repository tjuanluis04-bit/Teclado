package com.tecladoapp.keyboard

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.util.UUID

/** Biblioteca personal de términos, definiciones y analogías por categoría. */
class TermsPanelView(
    context: Context,
    private val prefs: Prefs,
    private val inlineProvider: InlineKeyboardProvider
) : LinearLayout(context) {

    interface Listener {
        fun onInsert(text: String)
        fun onClose()
    }
    var listener: Listener? = null

    private val density = context.resources.displayMetrics.density
    private fun px(dp: Float) = (dp * density).toInt()

    private var filter: String? = null // null = todas
    private val chipsRow = LinearLayout(context)
    private val listBox = LinearLayout(context)

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.parseColor("#161616"))

        val top = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(px(16f), px(4f), px(8f), px(4f))
        }
        top.addView(TextView(context).apply {
            text = "Términos y definiciones"; textSize = 19f
            setTextColor(Color.parseColor("#EAEAEA"))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        top.addView(TextView(context).apply {
            text = "＋"; textSize = 28f
            setTextColor(Color.parseColor("#6C8CFF"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(px(52f), px(48f))
            setOnClickListener { showAddDialog() }
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

        chipsRow.orientation = HORIZONTAL
        chipsRow.setPadding(px(8f), 0, px(8f), 0)
        val chipScroll = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(50f))
            addView(chipsRow)
        }
        addView(chipScroll)

        listBox.orientation = VERTICAL
        listBox.setPadding(px(10f), px(4f), px(10f), px(10f))
        addView(ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            addView(listBox)
        })
        render()
    }

    private fun chip(text: String, selected: Boolean, onClick: () -> Unit) = TextView(context).apply {
        this.text = text; textSize = 16f
        setTextColor(if (selected) Color.WHITE else Color.parseColor("#BBBBBB"))
        gravity = Gravity.CENTER
        setPadding(px(18f), 0, px(18f), 0)
        background = GradientDrawable().apply {
            setColor(Color.parseColor(if (selected) "#4C6FFF" else "#2C2C2C"))
            cornerRadius = px(20f).toFloat()
        }
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, px(38f))
            .apply { setMargins(px(4f), px(6f), px(4f), px(6f)) }
        setOnClickListener { onClick() }
    }

    private fun render() {
        val all = prefs.getTerms()
        val cats = all.map { it.category }.filter { it.isNotBlank() }.distinct()
        if (filter != null && filter !in cats) filter = null
        chipsRow.removeAllViews()
        chipsRow.addView(chip("Todas", filter == null) { filter = null; render() })
        cats.forEach { c -> chipsRow.addView(chip(c, filter == c) { filter = c; render() }) }

        listBox.removeAllViews()
        val shown = all.filter { filter == null || it.category == filter }
        if (shown.isEmpty()) {
            listBox.addView(TextView(context).apply {
                text = "Aún no hay términos.\nToca ＋ para añadir uno con su definición y analogía."
                textSize = 17f
                setTextColor(Color.parseColor("#888888"))
                gravity = Gravity.CENTER
                setPadding(px(16f), px(40f), px(16f), px(16f))
            })
            return
        }
        shown.forEach { listBox.addView(card(it)) }
    }

    private fun insertButton(label: String, text: String): TextView? {
        if (text.isBlank()) return null
        return TextView(context).apply {
            this.text = label; textSize = 15f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(px(14f), px(8f), px(14f), px(8f))
            background = GradientDrawable().apply { setColor(Color.parseColor("#3A4A80")); cornerRadius = px(10f).toFloat() }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { setMargins(0, px(8f), px(8f), 0) }
            setOnClickListener { listener?.onInsert(text) }
        }
    }

    private fun card(e: TermEntry): View {
        val card = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(px(14f), px(12f), px(14f), px(12f))
            background = GradientDrawable().apply { setColor(Color.parseColor("#232323")); cornerRadius = px(12f).toFloat() }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { setMargins(0, px(5f), 0, px(5f)) }
        }
        card.addView(TextView(context).apply { text = e.term; textSize = 19f; setTextColor(Color.WHITE); setTypeface(typeface, android.graphics.Typeface.BOLD) })
        if (e.definition.isNotBlank()) card.addView(TextView(context).apply { text = e.definition; textSize = 16f; setTextColor(Color.parseColor("#D0D0D0")); setPadding(0, px(4f), 0, 0) })
        if (e.analogy.isNotBlank()) card.addView(TextView(context).apply { text = "≈ " + e.analogy; textSize = 15f; setTextColor(Color.parseColor("#9FB4FF")); setPadding(0, px(4f), 0, 0) })

        val buttons = LinearLayout(context).apply { orientation = HORIZONTAL }
        insertButton("Término", e.term)?.let { buttons.addView(it) }
        insertButton("Definición", e.definition)?.let { buttons.addView(it) }
        insertButton("Analogía", e.analogy)?.let { buttons.addView(it) }
        val full = listOf(e.term, e.definition, e.analogy).filter { it.isNotBlank() }.let {
            if (it.size > 1) "${e.term}: ${e.definition}" + (if (e.analogy.isNotBlank()) " Es como ${e.analogy}" else "") else it.joinToString()
        }
        buttons.addView(insertButton("Todo", full) ?: View(context))
        val del = TextView(context).apply {
            text = "Borrar"; textSize = 15f
            setTextColor(Color.parseColor("#FF6B6B"))
            gravity = Gravity.CENTER
            setPadding(px(10f), px(8f), px(10f), px(8f))
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = px(8f) }
            setOnClickListener { prefs.deleteTerm(e.id); render() }
        }
        buttons.addView(del)
        card.addView(buttons)
        card.setOnLongClickListener { del.visibility = if (del.visibility == View.VISIBLE) View.GONE else View.VISIBLE; true }
        return card
    }

    // ---------- diálogo para añadir (4 campos, un solo teclado compartido) ----------
    private fun showAddDialog() {
        val fields = listOf(
            InlineTextField(context, "Categoría (ej. Biología)"),
            InlineTextField(context, "Término"),
            InlineTextField(context, "Definición"),
            InlineTextField(context, "Analogía (opcional)")
        )
        val slot = FrameLayout(context).apply { layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0) }
        val popup = PopupContainer(context)
        val container = LinearLayout(context).apply { orientation = VERTICAL; setPadding(24, 8, 24, 16) }

        fun focus(f: InlineTextField) {
            fields.forEach { it.setActive(it === f) }
            inlineProvider.attachInlineKeyboard(slot, f, extraHeightPx = 0)
        }
        val save = Button(context).apply {
            text = "Guardar"; textSize = 18f
            setOnClickListener {
                val term = fields[1].text().trim()
                if (term.isEmpty()) return@setOnClickListener
                prefs.addTerm(TermEntry(UUID.randomUUID().toString(), fields[0].text().trim(), term, fields[2].text().trim(), fields[3].text().trim()))
                popup.dismiss()
                render()
            }
        }
        container.addView(save)
        fields.forEach { f ->
            f.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = px(6f) }
            f.onTap = { focus(f) }
            f.onEnter = { val i = fields.indexOf(f); if (i < fields.lastIndex) focus(fields[i + 1]) else save.performClick() }
            container.addView(f)
        }
        container.addView(slot)
        popup.show(container, "Nuevo término", this, onDismiss = { inlineProvider.detachInlineKeyboard() })
        focus(fields[1])
    }
}
