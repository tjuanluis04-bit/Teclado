package com.tecladoapp.keyboard

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Traductor rápido: escribes con el teclado real y se traduce en línea (requiere internet). */
class TranslatePanelView(
    context: Context,
    private val inlineProvider: InlineKeyboardProvider,
    initialText: String
) : LinearLayout(context) {

    interface Listener {
        fun onInsert(text: String)
        fun onClose()
    }
    var listener: Listener? = null

    private val langs = listOf(
        "es" to "Español", "en" to "Inglés", "fr" to "Francés", "pt" to "Portugués", "it" to "Italiano",
        "de" to "Alemán", "ja" to "Japonés", "ko" to "Coreano", "zh-CN" to "Chino", "ru" to "Ruso", "ar" to "Árabe"
    )
    private var from = 0
    private var to = 1

    private val density = context.resources.displayMetrics.density
    private fun px(dp: Float) = (dp * density).toInt()
    private val ui = Handler(Looper.getMainLooper())
    private var pending: Runnable? = null
    private var requestId = 0
    private var attached = false

    private val fromChip = TextView(context)
    private val toChip = TextView(context)
    private val result = TextView(context)
    private val field = InlineTextField(context, "Escribe para traducir…")
    private var lastTranslation = ""

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.parseColor("#161616"))

        val top = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(px(8f), 0, px(4f), 0) }
        styleChip(fromChip) { from = (from + 1) % langs.size; refreshChips(); translateNow() }
        styleChip(toChip) { to = (to + 1) % langs.size; refreshChips(); translateNow() }
        val swap = TextView(context).apply {
            text = "⇄"; textSize = 26f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
            layoutParams = LayoutParams(px(48f), px(44f))
            setOnClickListener { val t = from; from = to; to = t; refreshChips(); translateNow() }
        }
        val close = TextView(context).apply {
            text = "✕"; textSize = 26f; setTextColor(Color.parseColor("#CCCCCC")); gravity = Gravity.CENTER
            layoutParams = LayoutParams(px(52f), px(44f))
            setOnClickListener { listener?.onClose() }
        }
        top.addView(fromChip); top.addView(swap); top.addView(toChip)
        top.addView(android.view.View(context).apply { layoutParams = LayoutParams(0, 1, 1f) })
        top.addView(close)
        top.layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(52f))
        addView(top)

        result.apply {
            textSize = 20f
            setTextColor(Color.WHITE)
            setPadding(px(14f), px(8f), px(14f), px(8f))
            text = "La traducción aparecerá aquí"
            setTextColor(Color.parseColor("#888888"))
            background = GradientDrawable().apply { setColor(Color.parseColor("#232323")); cornerRadius = px(12f).toFloat() }
            layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(84f)).apply { setMargins(px(8f), px(2f), px(8f), px(4f)) }
            setOnClickListener { insert() }
        }
        addView(result)

        val buttons = LinearLayout(context).apply { orientation = HORIZONTAL; setPadding(px(4f), 0, px(4f), 0) }
        buttons.addView(button("Pegar", "#6A4C93") {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val t = cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
            if (t.isNotBlank()) { field.setTextValue(""); field.appendText(t.trim()); translateNow() }
        })
        buttons.addView(button("Traducir", "#4C6FFF") { translateNow() })
        buttons.addView(button("Insertar", "#2E7D32") { insert() })
        buttons.layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(48f))
        addView(buttons)

        field.layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(px(8f), px(2f), px(8f), px(2f)) }
        field.setTextValue(initialText)
        field.onTextChanged = { scheduleTranslate() }
        field.onEnter = { translateNow() }
        field.onTap = { attachKeyboard() }
        addView(field)
        addView(field.keyboardSlot)
        refreshChips()
    }

    private fun styleChip(tv: TextView, onClick: () -> Unit) {
        tv.apply {
            textSize = 17f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
            setPadding(px(14f), 0, px(14f), 0)
            background = GradientDrawable().apply { setColor(Color.parseColor("#2C3A66")); cornerRadius = px(20f).toFloat() }
            layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, px(40f))
            setOnClickListener { onClick() }
        }
    }

    private fun button(label: String, color: String, onClick: () -> Unit) = TextView(context).apply {
        text = label; textSize = 17f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
        background = GradientDrawable().apply { setColor(Color.parseColor(color)); cornerRadius = px(12f).toFloat() }
        layoutParams = LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply { setMargins(px(4f), px(2f), px(4f), px(2f)) }
        setOnClickListener { onClick() }
    }

    private fun refreshChips() {
        fromChip.text = langs[from].second
        toChip.text = langs[to].second
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!attached) { attached = true; attachKeyboard(); if (field.text().isNotBlank()) translateNow() }
    }

    private fun attachKeyboard() {
        field.setActive(true)
        inlineProvider.attachInlineKeyboard(field.keyboardSlot, field, extraHeightPx = px(250f))
    }

    private fun scheduleTranslate() {
        pending?.let { ui.removeCallbacks(it) }
        val r = Runnable { translateNow() }
        pending = r
        ui.postDelayed(r, 800)
    }

    private fun insert() {
        if (lastTranslation.isNotBlank()) listener?.onInsert(lastTranslation)
    }

    private fun translateNow() {
        val text = field.text().trim()
        if (text.isEmpty()) { showResult("", false); return }
        val id = ++requestId
        showStatus("Traduciendo…")
        val pair = "${langs[from].first}|${langs[to].first}"
        Thread {
            val out = try {
                val url = URL("https://api.mymemory.translated.net/get?q=" + URLEncoder.encode(text, "UTF-8") + "&langpair=" + URLEncoder.encode(pair, "UTF-8"))
                val c = url.openConnection() as HttpURLConnection
                c.connectTimeout = 8000; c.readTimeout = 8000
                val body = c.inputStream.bufferedReader().use { it.readText() }
                JSONObject(body).getJSONObject("responseData").getString("translatedText")
            } catch (e: Exception) { null }
            ui.post {
                if (id != requestId) return@post
                if (out == null) showStatus("Sin conexión o error al traducir") else showResult(out, true)
            }
        }.start()
    }

    private fun showStatus(msg: String) {
        result.text = msg
        result.setTextColor(Color.parseColor("#888888"))
    }

    private fun showResult(t: String, ok: Boolean) {
        lastTranslation = if (ok) android.text.Html.fromHtml(t, android.text.Html.FROM_HTML_MODE_LEGACY).toString() else ""
        if (lastTranslation.isEmpty()) showStatus("La traducción aparecerá aquí")
        else { result.text = lastTranslation; result.setTextColor(Color.WHITE) }
    }
}
