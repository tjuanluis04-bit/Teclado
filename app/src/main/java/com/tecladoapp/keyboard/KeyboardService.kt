package com.tecladoapp.keyboard

import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import java.text.BreakIterator
import java.util.Calendar

class KeyboardService : InputMethodService(), KeyboardListener, InlineKeyboardProvider {

    private lateinit var prefs: Prefs
    private lateinit var rootLayout: LinearLayout
    private lateinit var toolbar: View
    private lateinit var suggestionBar: SuggestionBarView
    private lateinit var contentContainer: FrameLayout
    private lateinit var keyboardView: KeyboardView
    private var styleButton: ImageView? = null

    private val uiHandler = Handler(Looper.getMainLooper())
    private var inlineHost: InlineInputHost? = null
    private var inlineSlot: ViewGroup? = null
    private var currentEmojiPanel: EmojiPanelView? = null
    private var activeStyle: TextStyle? = null
    private var userFreq: MutableMap<String, Int> = mutableMapOf()
    private var learnCount = 0
    private var hasSelection = false
    private var builtThemeId = ""

    override fun onUpdateSelection(oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int, candidatesStart: Int, candidatesEnd: Int) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        hasSelection = newSelStart != newSelEnd
    }

    // ---- historial para deshacer / rehacer ----
    private class Edit(var inserted: String, var deletedBefore: String, var deletedAfter: String, var time: Long)
    private val undoStack = ArrayDeque<Edit>()
    private val redoStack = ArrayDeque<Edit>()

    private var clipboardManager: ClipboardManager? = null
    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        val clip = clipboardManager?.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).coerceToText(this)?.toString()
            if (!text.isNullOrBlank()) { prefs.addNewClip(text); prefs.pendingClip = text; uiHandler.post { updateSuggestions() } }
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        userFreq = prefs.getWordFreq()
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager?.addPrimaryClipChangedListener(clipListener)
    }

    override fun onDestroy() {
        clipboardManager?.removePrimaryClipChangedListener(clipListener)
        prefs.saveWordFreq(userFreq)
        super.onDestroy()
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    private fun px(dp: Float) = (dp * resources.displayMetrics.density).toInt()
    private fun toolbarHeightPx() = px(56f)
    private fun suggestionBarHeightPx() = px(50f)
    private fun keyboardHeightPx() = px(KeyboardView.BASE_ROW_DP * 5 * prefs.keyboardHeightScale)

    override fun onCreateInputView(): View {
        return try {
            buildInputView()
        } catch (e: Throwable) {
            errorView("onCreateInputView", e)
        }
    }

    private fun errorView(where: String, e: Throwable): View {
        return TextView(this).apply {
            text = "ERROR EN $where\n\n${e.javaClass.simpleName}: ${e.message}\n\n" +
                e.stackTrace.take(8).joinToString("\n") { "  en $it" }
            setBackgroundColor(Color.RED)
            setTextColor(Color.WHITE)
            textSize = 11f
            setPadding(24, 24, 24, 24)
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun buildInputView(): View {
        val theme = ThemeCatalog.theme(prefs.themeId)
        val font = ThemeCatalog.font(prefs.fontId)
        builtThemeId = theme.id

        rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(theme.backgroundColor)
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        toolbar = buildToolbar(theme)
        toolbar.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, toolbarHeightPx())
        rootLayout.addView(toolbar)

        suggestionBar = SuggestionBarView(this).apply {
            onSuggestionTap = { word -> replaceCurrentWord(word) }
        }
        suggestionBar.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, suggestionBarHeightPx())
        rootLayout.addView(suggestionBar)
        suggestionBar.fontFamily = font.fontFamily

        contentContainer = FrameLayout(this)
        contentContainer.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, keyboardHeightPx())
        rootLayout.addView(contentContainer)

        try {
            keyboardView = KeyboardView(this).apply {
                listener = this@KeyboardService
                this.theme = theme
                this.fontFamily = font.fontFamily
                this.heightScale = prefs.keyboardHeightScale
                layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, keyboardHeightPx())
            }
            showKeyboardView()
        } catch (e: Throwable) {
            contentContainer.addView(errorView("KeyboardView", e))
        }

        return rootLayout
    }

    // ---------- barra superior ----------
    private fun buildToolbar(theme: KeyboardTheme): View {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(px(8f), px(4f), px(8f), px(4f))
        }
        val size = px(46f)
        fun lp() = LinearLayout.LayoutParams(size, size).apply { marginStart = px(6f) }

        fun iconButton(resId: Int, onClick: (View) -> Unit) = ImageView(this).apply {
            setImageResource(resId)
            setColorFilter(theme.accentColor)
            layoutParams = lp()
            setPadding(px(9f), px(9f), px(9f), px(9f))
            setOnClickListener { onClick(it) }
        }
        val fontButton = TextView(this).apply {
            text = "Aa"
            textSize = 20f
            setTextColor(theme.accentColor)
            gravity = Gravity.CENTER
            layoutParams = lp()
        }
        fontButton.setOnClickListener { showFontPicker(fontButton) }

        bar.addView(fontButton)
        bar.addView(iconButton(R.drawable.ic_clipboard) { showClipboardPanel() })
        bar.addView(iconButton(R.drawable.ic_emoji) { showEmojiPanel() })
        bar.addView(iconButton(R.drawable.ic_clock) { showDateTimePicker(it) })
        bar.addView(iconButton(R.drawable.ic_undo) { undo() })
        bar.addView(iconButton(R.drawable.ic_redo) { redo() })
        bar.addView(iconButton(R.drawable.ic_book) { showTermsPanel() })
        bar.addView(iconButton(R.drawable.ic_translate) { showTranslatePanel() })
        bar.addView(iconButton(R.drawable.ic_calculator) { showCalculatorPanel() })
        val sb = iconButton(R.drawable.ic_textstyle) { showTextStylePanel() }
        styleButton = sb
        bar.addView(sb)
        bar.addView(iconButton(R.drawable.ic_settings) { openSettings() })

        return HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            setBackgroundColor(darken(theme.backgroundColor))
            addView(bar)
        }
    }

    /** Lista emergente NO enfocable: una ventana enfocable le quita el foco al teclado y lo hace parpadear. */
    private fun showListPopup(anchor: View, rows: List<Triple<String, android.graphics.Typeface?, () -> Unit>>, selectedIndex: Int = -1) {
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#2A2A2A"))
                cornerRadius = px(12f).toFloat()
            }
        }
        val popup = PopupWindow(list, px(260f), ViewGroup.LayoutParams.WRAP_CONTENT, false)
        popup.isOutsideTouchable = true
        rows.forEachIndexed { i, (label, tf, action) ->
            list.addView(TextView(this).apply {
                text = label
                textSize = 19f
                setTextColor(Color.WHITE)
                setPadding(px(22f), px(15f), px(22f), px(15f))
                tf?.let { typeface = it }
                if (i == selectedIndex) setBackgroundColor(Color.parseColor("#3A3A5A"))
                setOnClickListener { popup.dismiss(); action() }
            })
        }
        popup.showAsDropDown(anchor, 0, 0)
    }

    private fun showFontPicker(anchor: View) {
        val fonts = ThemeCatalog.fonts
        val rows = fonts.map { font ->
            val tf = font.fontFamily?.let { android.graphics.Typeface.create(it, android.graphics.Typeface.NORMAL) } ?: android.graphics.Typeface.DEFAULT
            Triple(font.displayName, tf as android.graphics.Typeface?, {
                prefs.fontId = font.id
                if (::keyboardView.isInitialized) keyboardView.fontFamily = font.fontFamily
                suggestionBar.fontFamily = font.fontFamily
            })
        }
        showListPopup(anchor, rows, fonts.indexOfFirst { it.id == prefs.fontId })
    }

    private fun showDateTimePicker(anchor: View) {
        val now = Calendar.getInstance()
        val h24 = now.get(Calendar.HOUR_OF_DAY)
        val min = now.get(Calendar.MINUTE)
        val suffix = if (h24 < 12) "a.m" else "p.m"
        val h12 = if (h24 % 12 == 0) 12 else h24 % 12
        val t24 = String.format("%d:%02d %s", h24, min, suffix)
        val t12 = String.format("%d:%02d %s", h12, min, suffix)
        val months = listOf("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")
        val date = "${now.get(Calendar.DAY_OF_MONTH)}/${months[now.get(Calendar.MONTH)]}/${now.get(Calendar.YEAR)}"
        showListPopup(anchor, listOf(
            Triple("Hora 24 h: $t24", null as android.graphics.Typeface?, { commitRecorded(t24) }),
            Triple("Hora 12 h: $t12", null as android.graphics.Typeface?, { commitRecorded(t12) }),
            Triple("Fecha: $date", null as android.graphics.Typeface?, { commitRecorded(date) })
        ))
    }

    private fun darken(color: Int): Int {
        val r = ((color shr 16) and 0xFF) * 0.85
        val g = ((color shr 8) and 0xFF) * 0.85
        val b = (color and 0xFF) * 0.85
        return Color.rgb(r.toInt(), g.toInt(), b.toInt())
    }

    private fun openSettings() {
        val intent = android.content.Intent(this, SettingsActivity::class.java)
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    // ---------- paneles ----------
    private fun resetContentHeight() {
        contentContainer.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, keyboardHeightPx())
    }

    private fun showPanel(panel: View, hideSuggestions: Boolean = true) {
        detachInlineKeyboard()
        currentEmojiPanel = null
        contentContainer.removeAllViews()
        panel.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        contentContainer.addView(panel)
        if (hideSuggestions) suggestionBar.visibility = View.GONE
    }

    private fun showKeyboardView() {
        detachInlineKeyboard()
        currentEmojiPanel = null
        contentContainer.removeAllViews()
        (keyboardView.parent as? ViewGroup)?.removeView(keyboardView)
        contentContainer.addView(keyboardView)
        keyboardView.resetToDefault()
        suggestionBar.visibility = if (prefs.wordSuggestionsEnabled) View.VISIBLE else View.GONE
        evaluateAutoCap()
        updateSuggestions()
    }

    private fun showEmojiPanel() {
        detachInlineKeyboard()
        val panel = EmojiPanelView(this, prefs, this)
        panel.listener = object : EmojiPanelView.Listener {
            override fun onEmojiSelected(emoji: String) { commitRecorded(emoji) }
            override fun onClose() { showKeyboardView() }
            override fun onDeleteLastEmoji() { deleteLastEmojiOrChar() }
        }
        showPanel(panel)
        currentEmojiPanel = panel
    }

    private fun showClipboardPanel() {
        val panel = ClipboardPanelView(this, prefs, this)
        panel.listener = object : ClipboardPanelView.Listener {
            override fun onPaste(text: String) { if (text == prefs.pendingClip) prefs.pendingClip = null; commitRecorded(text) }
            override fun onClose() { showKeyboardView() }
        }
        showPanel(panel)
    }

    private fun showTermsPanel() {
        val panel = TermsPanelView(this, prefs, this)
        panel.listener = object : TermsPanelView.Listener {
            override fun onInsert(text: String) { commitRecorded(text) }
            override fun onClose() { showKeyboardView() }
        }
        showPanel(panel)
    }

    private fun showCalculatorPanel() {
        val panel = CalculatorPanelView(this)
        panel.listener = object : CalculatorPanelView.Listener {
            override fun onInsert(text: String) { commitRecorded(text) }
            override fun onClose() { showKeyboardView() }
        }
        showPanel(panel)
    }

    private fun showTranslatePanel() {
        val selected = if (hasSelection) currentInputConnection?.getSelectedText(0)?.toString().orEmpty() else ""
        val panel = TranslatePanelView(this, this, selected)
        panel.listener = object : TranslatePanelView.Listener {
            override fun onInsert(text: String) { commitRecorded(text) }
            override fun onClose() { showKeyboardView() }
        }
        showPanel(panel)
    }

    private fun showTextStylePanel() {
        val panel = TextStylePanelView(this, activeStyle?.id)
        panel.listener = object : TextStylePanelView.Listener {
            override fun onStyleChosen(style: TextStyle?) {
                activeStyle = style
                styleButton?.setColorFilter(if (style != null) Color.parseColor("#FFC107") else ThemeCatalog.theme(prefs.themeId).accentColor)
                showKeyboardView()
            }
            override fun onClose() { showKeyboardView() }
        }
        showPanel(panel)
    }

    // ---------- teclado compartido dentro de paneles (InlineKeyboardProvider) ----------
    override fun attachInlineKeyboard(slot: ViewGroup, host: InlineInputHost, extraHeightPx: Int) {
        if (!::keyboardView.isInitialized) return
        (keyboardView.parent as? ViewGroup)?.removeView(keyboardView)
        // si había otro hueco ocupado, lo colapsamos
        inlineSlot?.let { old -> if (old !== slot) collapse(old) }
        slot.removeAllViews()
        val kh = keyboardHeightPx()
        keyboardView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, kh)
        slot.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, kh)
        slot.visibility = View.VISIBLE
        slot.addView(keyboardView)
        inlineSlot = slot
        inlineHost = host
        contentContainer.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, if (extraHeightPx > 0) kh + extraHeightPx else kh
        )
    }

    private fun collapse(slot: ViewGroup) {
        slot.removeAllViews()
        (slot.layoutParams as? LinearLayout.LayoutParams)?.let { it.height = 0; slot.layoutParams = it }
    }

    override fun detachInlineKeyboard() {
        if (!::contentContainer.isInitialized) return
        if (inlineHost == null && inlineSlot == null) return
        (keyboardView.parent as? ViewGroup)?.removeView(keyboardView)
        inlineSlot?.let { collapse(it) }
        inlineSlot = null
        inlineHost = null
        resetContentHeight()
    }

    // ---------- ciclo de vida ----------
    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        try {
            if (!::keyboardView.isInitialized) return
            if (builtThemeId != prefs.themeId) {
                // el tema cambió: se reconstruye todo (barra, fondo, iconos) para no arrastrar colores viejos
                detachInlineKeyboard()
                (keyboardView.parent as? ViewGroup)?.removeView(keyboardView)
                setInputView(buildInputView())
                return
            }
            val theme = ThemeCatalog.theme(prefs.themeId)
            val font = ThemeCatalog.font(prefs.fontId)
            keyboardView.theme = theme
            keyboardView.fontFamily = font.fontFamily
            suggestionBar.fontFamily = font.fontFamily
            keyboardView.heightScale = prefs.keyboardHeightScale
            val kh = keyboardHeightPx()
            keyboardView.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, kh)
            contentContainer.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, kh)
            if (!restarting) { undoStack.clear(); redoStack.clear() }
            // Siempre se vuelve al panel principal de letras, con Bloq Mayús liberado
            showKeyboardView()
        } catch (e: Throwable) {
            if (::contentContainer.isInitialized) {
                contentContainer.removeAllViews()
                contentContainer.addView(errorView("onStartInputView", e))
            }
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        saveFreq()
    }

    // ---------- edición con historial ----------
    private fun record(inserted: String, before: String, after: String) {
        val now = System.currentTimeMillis()
        val last = undoStack.lastOrNull()
        redoStack.clear()
        if (last != null && now - last.time < 1200) {
            if (before.isEmpty() && after.isEmpty() && last.deletedBefore.isEmpty() && last.deletedAfter.isEmpty() && inserted.length < 2 && last.inserted.length < 60 && inserted != " ") {
                last.inserted += inserted; last.time = now; return
            }
            if (inserted.isEmpty() && last.inserted.isEmpty() && after.isEmpty() && last.deletedAfter.isEmpty() && before.isNotEmpty() && last.deletedBefore.length < 80) {
                last.deletedBefore = before + last.deletedBefore; last.time = now; return
            }
        }
        undoStack.addLast(Edit(inserted, before, after, now))
        if (undoStack.size > 200) undoStack.removeFirst()
    }

    private fun commitRecorded(text: String) {
        val ic = currentInputConnection ?: return
        val sel = if (hasSelection) ic.getSelectedText(0)?.toString().orEmpty() else ""
        ic.commitText(text, 1)
        record(text, sel, "")
        scheduleRefresh()
    }

    private fun undo() {
        val ic = currentInputConnection ?: return
        val e = undoStack.removeLastOrNull() ?: return
        if (e.inserted.isNotEmpty()) ic.deleteSurroundingText(e.inserted.length, 0)
        if (e.deletedBefore.isNotEmpty()) ic.commitText(e.deletedBefore, 1)
        if (e.deletedAfter.isNotEmpty()) {
            ic.commitText(e.deletedAfter, 1)
            moveByRelative(ic, -e.deletedAfter.length)
        }
        redoStack.addLast(e)
        scheduleRefresh()
    }

    private fun redo() {
        val ic = currentInputConnection ?: return
        val e = redoStack.removeLastOrNull() ?: return
        if (e.deletedBefore.isNotEmpty() || e.deletedAfter.isNotEmpty()) {
            ic.deleteSurroundingText(e.deletedBefore.length, e.deletedAfter.length)
        }
        if (e.inserted.isNotEmpty()) ic.commitText(e.inserted, 1)
        undoStack.addLast(e)
        scheduleRefresh()
    }

    /** Borra un rango alrededor del cursor guardándolo en el historial. */
    private fun deleteRange(before: Int, after: Int) {
        val ic = currentInputConnection ?: return
        if (before == 0 && after == 0) return
        val b = if (before > 0) ic.getTextBeforeCursor(before, 0)?.toString().orEmpty() else ""
        val a = if (after > 0) ic.getTextAfterCursor(after, 0)?.toString().orEmpty() else ""
        ic.deleteSurroundingText(b.length, a.length)
        record("", b, a)
    }

    /** Borra un carácter completo (emoji, letra con acento, carácter estilizado) hacia atrás. */
    private fun deleteOneBack() {
        val ic = currentInputConnection ?: return
        if (hasSelection) {
            val sel = ic.getSelectedText(0)?.toString()
            if (!sel.isNullOrEmpty()) {
                ic.commitText("", 1)
                record("", sel, "")
                return
            }
        }
        val before = ic.getTextBeforeCursor(12, 0)?.toString().orEmpty()
        if (before.isEmpty()) {
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
            return
        }
        var len = 1
        val last = before.last()
        if (last.code > 0x2FF) {
            val it = BreakIterator.getCharacterInstance()
            it.setText(before)
            it.last()
            val start = it.previous()
            len = if (start >= 0) before.length - start else 1
        } else if (last.code in 0x300..0x36F) {
            len = 2
        }
        ic.deleteSurroundingText(len, 0)
        record("", before.takeLast(len), "")
    }

    // ---------- KeyboardListener ----------
    override fun onKeyChar(char: Char) {
        inlineHost?.let { it.onInlineChar(char); return }
        val style = activeStyle
        val out = if (style != null) TextStyles.applyChar(style, char) else char.toString()
        commitRecorded(out)
    }

    override fun onBackspace() {
        inlineHost?.let { it.onInlineBackspace(); return }
        deleteOneBack()
        scheduleRefresh()
    }

    override fun onEnter() {
        inlineHost?.let { it.onInlineEnter(); return }
        learnLastWord()
        val ic = currentInputConnection ?: return
        val action = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
        val multiLine = (currentInputEditorInfo?.inputType ?: 0) and InputType.TYPE_TEXT_FLAG_MULTI_LINE != 0
        if (!multiLine && action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            ic.performEditorAction(action)
        } else {
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
            record("\n", "", "")
        }
        evaluateAutoCap()
        scheduleRefresh()
    }

    override fun onSpace() {
        inlineHost?.let { it.onInlineChar(' '); return }
        val ic = currentInputConnection ?: return
        ic.commitText(" ", 1)
        record(" ", "", "")
        val before = ic.getTextBeforeCursor(40, 0)?.toString().orEmpty()
        learnLastWord(before)
        evaluateAutoCap(before)
        scheduleRefresh()
    }

    override fun onShiftToggled(active: Boolean) {}

    override fun onSwitchToSymbols(showSymbols: Boolean) {}

    override fun onOpenEmoji() { showEmojiPanel() }

    override fun onOpenClipboard() { showClipboardPanel() }

    override fun onEnterSwipeWord(direction: Int, extendSelection: Boolean) {
        if (inlineHost != null) return
        moveCursorByWord(direction, extendSelection)
    }

    override fun onCursorMove(direction: Int) {
        if (inlineHost != null) return
        val ic = currentInputConnection ?: return
        moveByRelative(ic, direction)
    }

    override fun onDeleteWord(direction: Int) {
        if (inlineHost != null) { inlineHost?.onInlineBackspace(); return }
        val n = wordSpan(direction)
        if (direction < 0) deleteRange(n, 0) else deleteRange(0, n)
        scheduleRefresh()
    }

    override fun onSpanSwipe(dir: Int, delete: Boolean) {
        if (inlineHost != null) return
        val ic = currentInputConnection ?: return
        when (dir) {
            0, 1 -> {
                val n = wordSpan(if (dir == 0) -1 else 1)
                if (n == 0) return
                if (delete) { if (dir == 0) deleteRange(n, 0) else deleteRange(0, n) }
                else extendSelectionBy(ic, forward = dir == 1, count = n)
            }
            2 -> { // arriba: hasta el inicio de la línea (o el salto anterior)
                val before = ic.getTextBeforeCursor(600, 0)?.toString().orEmpty()
                if (before.isEmpty()) return
                val idx = before.lastIndexOf('\n')
                val n = if (idx == before.length - 1) 1 else before.length - (idx + 1)
                if (delete) deleteRange(n, 0) else extendSelectionBy(ic, forward = false, count = n)
            }
            3 -> { // abajo: hasta el final de la línea (o el salto siguiente)
                val after = ic.getTextAfterCursor(600, 0)?.toString().orEmpty()
                if (after.isEmpty()) return
                val idx = after.indexOf('\n')
                val n = if (idx == 0) 1 else if (idx < 0) after.length else idx
                if (delete) deleteRange(0, n) else extendSelectionBy(ic, forward = true, count = n)
            }
        }
        scheduleRefresh()
    }

    /** Nº de caracteres de la palabra siguiente/anterior respecto al cursor (incluye espacios intermedios). */
    private fun wordSpan(direction: Int): Int {
        val ic = currentInputConnection ?: return 0
        if (direction > 0) {
            val after = ic.getTextAfterCursor(120, 0)?.toString().orEmpty()
            var i = 0
            while (i < after.length && after[i].isWhitespace()) i++
            while (i < after.length && !after[i].isWhitespace()) i++
            return i
        }
        val before = ic.getTextBeforeCursor(120, 0)?.toString().orEmpty()
        var i = before.length
        while (i > 0 && before[i - 1].isWhitespace()) i--
        while (i > 0 && !before[i - 1].isWhitespace()) i--
        return before.length - i
    }

    // ---------- mayúsculas automáticas ----------
    private fun autoCapAllowed(): Boolean {
        val type = currentInputEditorInfo?.inputType ?: return false
        if (type and InputType.TYPE_MASK_CLASS != InputType.TYPE_CLASS_TEXT) return false
        return when (type and InputType.TYPE_MASK_VARIATION) {
            InputType.TYPE_TEXT_VARIATION_PASSWORD, InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD, InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_URI, InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS -> false
            else -> true
        }
    }

    private fun evaluateAutoCap(known: String? = null) {
        if (!::keyboardView.isInitialized || inlineHost != null) return
        if (!autoCapAllowed()) { keyboardView.setAutoShift(false); return }
        val before = known?.takeLast(6) ?: (currentInputConnection ?: return).getTextBeforeCursor(6, 0)?.toString().orEmpty()
        val need = before.isEmpty() ||
            before.last() == '\n' ||
            before.last() == '¿' || before.last() == '¡' ||
            Regex("[.!?…]\\s+$").containsMatchIn(before)
        keyboardView.setAutoShift(need)
    }

    // ---------- sugerencias / ortografía / aprendizaje ----------
    private val refreshRunnable = Runnable { updateSuggestions(); evaluateAutoCap() }

    private fun scheduleRefresh() {
        uiHandler.removeCallbacks(refreshRunnable)
        uiHandler.postDelayed(refreshRunnable, 110)
    }

    private fun learnLastWord(text: String? = null) {
        val before = (text ?: currentInputConnection?.getTextBeforeCursor(40, 0)?.toString().orEmpty()).trimEnd()
        val word = before.takeLastWhile { it.isLetter() }.lowercase()
        if (word.length >= 3) {
            userFreq[word] = (userFreq[word] ?: 0) + 1
            if (++learnCount % 10 == 0) saveFreq()
        }
    }

    private fun saveFreq() { prefs.saveWordFreq(userFreq) }

    private fun matchCase(typed: String, suggestion: String): String =
        if (typed.isNotEmpty() && typed.first().isUpperCase()) suggestion.replaceFirstChar { it.uppercase() } else suggestion

    private fun updateSuggestions() {
        if (!::suggestionBar.isInitialized) return
        if (!prefs.wordSuggestionsEnabled) {
            suggestionBar.visibility = View.GONE
            return
        }
        if (inlineHost != null) return
        val ic = currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(30, 0)?.toString() ?: ""
        val currentWord = before.takeLastWhile { it.isLetter() }
        val theme = ThemeCatalog.theme(prefs.themeId)
        val suggestions = when {
            currentWord.isEmpty() -> WordSuggester.suggestionsFor("", userFreq)
            prefs.spellCheckEnabled && !WordSuggester.isSpelledCorrectly(currentWord, userFreq) ->
                WordSuggester.spellingCorrections(currentWord)
            else -> WordSuggester.suggestionsFor(currentWord, userFreq)
        }.map { matchCase(currentWord, it) }
        suggestionBar.visibility = View.VISIBLE
        val clip = if (currentWord.isEmpty()) prefs.pendingClip else null
        suggestionBar.setSuggestions(suggestions, theme, clip) {
            prefs.pendingClip = null
            commitRecorded(clip.orEmpty())
            updateSuggestions()
        }
    }

    private fun replaceCurrentWord(newWord: String) {
        val ic = currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(30, 0)?.toString() ?: ""
        val currentWord = before.takeLastWhile { it.isLetter() }
        if (currentWord.isNotEmpty()) deleteRange(currentWord.length, 0)
        val styled = activeStyle?.let { TextStyles.convert(it, newWord) } ?: newWord
        commitRecorded("$styled ")
        val key = newWord.lowercase()
        userFreq[key] = (userFreq[key] ?: 0) + 1
        evaluateAutoCap()
    }

    // ---------- movimiento del cursor ----------
    private fun moveCursorByWord(direction: Int, extendSelection: Boolean) {
        val ic = currentInputConnection ?: return
        val n = wordSpan(direction)
        if (n == 0) return
        if (extendSelection) extendSelectionBy(ic, forward = direction > 0, count = n)
        else moveByRelative(ic, direction * n)
    }

    private fun moveByRelative(ic: InputConnection, delta: Int) {
        val keyCode = if (delta > 0) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT
        repeat(kotlin.math.abs(delta)) {
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        }
    }

    private fun extendSelectionBy(ic: InputConnection, forward: Boolean, count: Int) {
        val keyCode = if (forward) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT
        repeat(count) {
            val meta = KeyEvent.META_SHIFT_ON
            ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, keyCode, 0, meta))
            ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, keyCode, 0, meta))
        }
    }

    /** Borra el último emoji (o carácter) sin partir secuencias de varios puntos de código. */
    private fun deleteLastEmojiOrChar() {
        val ic = currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(16, 0)?.toString() ?: return
        if (before.isEmpty()) return
        val end = before.length
        var start = Character.offsetByCodePoints(before, end, -1)
        while (start > 0) {
            val cp = Character.codePointAt(before, start)
            val prevStart = Character.offsetByCodePoints(before, start, -1)
            val prevCp = Character.codePointAt(before, prevStart)
            val isJoinerOrModifier = prevCp == 0x200D || cp == 0xFE0F || cp == 0x200D ||
                (cp in 0x1F3FB..0x1F3FF) ||
                (cp in 0x1F1E6..0x1F1FF && prevCp in 0x1F1E6..0x1F1FF)
            if (isJoinerOrModifier) start = prevStart else break
        }
        deleteRange(end - start, 0)
    }
}
