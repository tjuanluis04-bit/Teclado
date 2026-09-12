package com.tecladoapp.keyboard

import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.inputmethodservice.InputMethodService
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class KeyboardService : InputMethodService(), KeyboardListener {

    private lateinit var prefs: Prefs
    private lateinit var rootLayout: LinearLayout
    private lateinit var toolbar: LinearLayout
    private lateinit var suggestionBar: SuggestionBarView
    private lateinit var contentContainer: FrameLayout
    private lateinit var keyboardView: KeyboardView

    private var emojiSearchActive = false
    private var currentEmojiPanel: EmojiPanelView? = null

    private var clipboardManager: ClipboardManager? = null
    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        val clip = clipboardManager?.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).coerceToText(this)?.toString()
            if (!text.isNullOrBlank()) prefs.addNewClip(text)
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager?.addPrimaryClipChangedListener(clipListener)
    }

    override fun onDestroy() {
        clipboardManager?.removePrimaryClipChangedListener(clipListener)
        super.onDestroy()
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    private fun px(dp: Float) = (dp * resources.displayMetrics.density).toInt()
    private fun toolbarHeightPx() = px(52f)
    private fun suggestionBarHeightPx() = px(46f)
    private fun keyboardHeightPx() = px(52f * 5 * prefs.keyboardHeightScale)

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

        rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(theme.backgroundColor)
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        toolbar = buildToolbar(theme)
        // Alto FIJO en píxeles exactos: WRAP_CONTENT se estiraba de forma
        // impredecible en algunos dispositivos, así que no lo volvemos a usar aquí.
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

    private fun buildToolbar(theme: KeyboardTheme): LinearLayout {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 10, 16, 10)
            setBackgroundColor(darken(theme.backgroundColor))
        }
        val spacer = android.view.View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
        }
        fun iconButton(resId: Int, onClick: () -> Unit) = android.widget.ImageView(this).apply {
            setImageResource(resId)
            setColorFilter(theme.accentColor)
            layoutParams = LinearLayout.LayoutParams(px(40f), px(40f)).apply { marginStart = px(10f) }
            setPadding(px(6f), px(6f), px(6f), px(6f))
            setOnClickListener { onClick() }
        }
        val fontButton = TextView(this).apply {
            text = "Aa"
            textSize = 16f
            setTextColor(theme.accentColor)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(px(40f), px(40f)).apply { marginStart = px(10f) }
            setOnClickListener { showFontPicker(fontButton) }
        }
        bar.addView(spacer)
        bar.addView(fontButton)
        bar.addView(iconButton(R.drawable.ic_clipboard) { showClipboardPanel() })
        bar.addView(iconButton(R.drawable.ic_emoji) { showEmojiPanel() })
        bar.addView(iconButton(R.drawable.ic_settings) { openSettings() })
        return bar
    }

    private fun showFontPicker(anchor: View) {
        val fonts = ThemeCatalog.fonts
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#2A2A2A"))
                cornerRadius = px(10f).toFloat()
            }
        }
        val popup = android.widget.PopupWindow(
            list, px(220f), ViewGroup.LayoutParams.WRAP_CONTENT, true
        )
        popup.isOutsideTouchable = true

        fonts.forEach { font ->
            val row = TextView(this).apply {
                text = font.displayName
                textSize = 15f
                setTextColor(Color.WHITE)
                setPadding(px(20f), px(14f), px(20f), px(14f))
                typeface = font.fontFamily?.let { android.graphics.Typeface.create(it, android.graphics.Typeface.NORMAL) }
                    ?: android.graphics.Typeface.DEFAULT
                if (font.id == prefs.fontId) {
                    setBackgroundColor(Color.parseColor("#3A3A5A"))
                }
                setOnClickListener {
                    prefs.fontId = font.id
                    if (::keyboardView.isInitialized) keyboardView.fontFamily = font.fontFamily
                    suggestionBar.fontFamily = font.fontFamily
                    popup.dismiss()
                }
            }
            list.addView(row)
        }
        popup.showAsDropDown(anchor, 0, 0)
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

    private fun showKeyboardView() {
        exitEmojiSearchIfNeeded()
        currentEmojiPanel = null
        contentContainer.removeAllViews()
        contentContainer.addView(keyboardView)
        suggestionBar.visibility = if (prefs.wordSuggestionsEnabled) View.VISIBLE else View.GONE
    }

    private fun showEmojiPanel() {
        val panel = EmojiPanelView(this, prefs)
        currentEmojiPanel = panel
        panel.listener = object : EmojiPanelView.Listener {
            override fun onEmojiSelected(emoji: String) {
                currentInputConnection?.commitText(emoji, 1)
            }
            override fun onClose() {
                exitEmojiSearchIfNeeded()
                currentEmojiPanel = null
                showKeyboardView()
            }
            override fun onEnterSearchMode(keyboardSlot: FrameLayout) {
                (keyboardView.parent as? ViewGroup)?.removeView(keyboardView)
                keyboardSlot.removeAllViews()
                keyboardSlot.addView(keyboardView)
                emojiSearchActive = true
            }
            override fun onExitSearchMode() {
                (keyboardView.parent as? ViewGroup)?.removeView(keyboardView)
                emojiSearchActive = false
            }
            override fun onDeleteLastEmoji() {
                deleteLastEmojiOrChar()
            }
        }
        contentContainer.removeAllViews()
        contentContainer.addView(panel)
        suggestionBar.visibility = View.GONE
    }

    private fun exitEmojiSearchIfNeeded() {
        if (emojiSearchActive) {
            (keyboardView.parent as? ViewGroup)?.removeView(keyboardView)
            emojiSearchActive = false
        }
    }

    /** Borra el último emoji (o carácter) antes del cursor, sin partir secuencias
     *  de varios puntos de código (banderas, ZWJ, selectores de variación, etc). */
    private fun deleteLastEmojiOrChar() {
        val ic = currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(16, 0)?.toString() ?: return
        if (before.isEmpty()) return
        var end = before.length
        var start = Character.offsetByCodePoints(before, end, -1)
        while (start > 0) {
            val cp = Character.codePointAt(before, start)
            val prevStart = Character.offsetByCodePoints(before, start, -1)
            val prevCp = Character.codePointAt(before, prevStart)
            val isJoinerOrModifier = prevCp == 0x200D || cp == 0xFE0F ||
                (prevCp in 0x1F3FB..0x1F3FF) ||
                (cp in 0x1F1E6..0x1F1FF && prevCp in 0x1F1E6..0x1F1FF)
            if (isJoinerOrModifier) {
                start = prevStart
            } else break
        }
        ic.deleteSurroundingText(end - start, 0)
    }

    private fun showClipboardPanel() {
        exitEmojiSearchIfNeeded()
        currentEmojiPanel = null
        val panel = ClipboardPanelView(this, prefs)
        panel.listener = object : ClipboardPanelView.Listener {
            override fun onPaste(text: String) {
                currentInputConnection?.commitText(text, 1)
            }
            override fun onClose() { showKeyboardView() }
        }
        contentContainer.removeAllViews()
        contentContainer.addView(panel)
        suggestionBar.visibility = View.GONE
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        try {
            if (!::keyboardView.isInitialized) return
            val theme = ThemeCatalog.theme(prefs.themeId)
            val font = ThemeCatalog.font(prefs.fontId)
            keyboardView.theme = theme
            keyboardView.fontFamily = font.fontFamily
            suggestionBar.fontFamily = font.fontFamily
            keyboardView.heightScale = prefs.keyboardHeightScale
            val kh = keyboardHeightPx()
            keyboardView.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, kh)
            contentContainer.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, kh)
            showKeyboardView()
            updateSuggestions()
        } catch (e: Throwable) {
            if (::contentContainer.isInitialized) {
                contentContainer.removeAllViews()
                contentContainer.addView(errorView("onStartInputView", e))
            }
        }
    }

    // ---------- KeyboardListener ----------
    override fun onKeyChar(char: Char) {
        if (emojiSearchActive) {
            currentEmojiPanel?.appendSearchChar(char)
            return
        }
        currentInputConnection?.commitText(char.toString(), 1)
        updateSuggestions()
    }

    override fun onBackspace() {
        if (emojiSearchActive) {
            currentEmojiPanel?.removeSearchChar()
            return
        }
        currentInputConnection?.deleteSurroundingText(1, 0)
        updateSuggestions()
    }

    override fun onEnter() {
        if (emojiSearchActive) {
            currentEmojiPanel?.closeSearchFromKeyboard()
            return
        }
        currentInputConnection?.sendKeyEvent(
            android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER)
        )
        currentInputConnection?.sendKeyEvent(
            android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_ENTER)
        )
    }

    override fun onSpace() {
        if (emojiSearchActive) {
            currentEmojiPanel?.appendSearchChar(' ')
            return
        }
        currentInputConnection?.commitText(" ", 1)
        updateSuggestions()
    }

    override fun onShiftToggled(active: Boolean) {
        keyboardView.invalidate()
    }

    override fun onSwitchToSymbols(showSymbols: Boolean) {
        keyboardView.invalidate()
    }

    override fun onOpenEmoji() { showEmojiPanel() }

    override fun onOpenClipboard() { showClipboardPanel() }

    override fun onEnterSwipeWord(direction: Int, extendSelection: Boolean) {
        if (emojiSearchActive) return
        moveCursorByWord(direction, extendSelection)
    }

    override fun onCursorMove(direction: Int) {
        if (emojiSearchActive) return
        val ic = currentInputConnection ?: return
        moveByRelative(ic, direction)
    }

    // ---------- Sugerencias / ortografía ----------
    private fun updateSuggestions() {
        if (!prefs.wordSuggestionsEnabled) {
            suggestionBar.visibility = View.GONE
            return
        }
        val ic = currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(30, 0)?.toString() ?: ""
        val currentWord = before.takeLastWhile { it.isLetter() }
        if (currentWord.isEmpty()) {
            suggestionBar.setSuggestions(emptyList(), ThemeCatalog.theme(prefs.themeId))
            return
        }
        val suggestions = if (prefs.spellCheckEnabled && !WordSuggester.isSpelledCorrectly(currentWord)) {
            WordSuggester.spellingCorrections(currentWord)
        } else {
            WordSuggester.suggestionsFor(currentWord)
        }
        suggestionBar.visibility = View.VISIBLE
        suggestionBar.setSuggestions(suggestions, ThemeCatalog.theme(prefs.themeId))
    }

    private fun replaceCurrentWord(newWord: String) {
        val ic = currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(30, 0)?.toString() ?: ""
        val currentWord = before.takeLastWhile { it.isLetter() }
        if (currentWord.isNotEmpty()) {
            ic.deleteSurroundingText(currentWord.length, 0)
        }
        ic.commitText("$newWord ", 1)
        updateSuggestions()
    }

    /**
     * Mueve el cursor (o extiende la selección) una palabra a la izquierda/derecha,
     * usado por el gesto de deslizar la tecla Enter.
     */
    private fun moveCursorByWord(direction: Int, extendSelection: Boolean) {
        val ic = currentInputConnection ?: return
        val windowSize = 60

        if (direction > 0) {
            val after = ic.getTextAfterCursor(windowSize, 0)?.toString() ?: return
            if (after.isEmpty()) return
            var i = 0
            while (i < after.length && after[i].isWhitespace()) i++
            while (i < after.length && !after[i].isWhitespace()) i++
            if (i == 0) return
            if (extendSelection) {
                extendSelectionBy(ic, forward = true, count = i)
            } else {
                moveByRelative(ic, i)
            }
        } else {
            val before = ic.getTextBeforeCursor(windowSize, 0)?.toString() ?: return
            if (before.isEmpty()) return
            var i = before.length
            while (i > 0 && before[i - 1].isWhitespace()) i--
            while (i > 0 && !before[i - 1].isWhitespace()) i--
            val count = before.length - i
            if (count == 0) return
            if (extendSelection) {
                extendSelectionBy(ic, forward = false, count = count)
            } else {
                moveByRelative(ic, -count)
            }
        }
    }

    /** Mueve el cursor 'delta' caracteres respecto a su posición actual (sin selección). */
    private fun moveByRelative(ic: android.view.inputmethod.InputConnection, delta: Int) {
        val keyCode = if (delta > 0) android.view.KeyEvent.KEYCODE_DPAD_RIGHT else android.view.KeyEvent.KEYCODE_DPAD_LEFT
        repeat(kotlin.math.abs(delta)) {
            ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyCode))
            ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, keyCode))
        }
    }

    /** Extiende la selección actual 'count' caracteres hacia adelante o atrás usando shift+flecha. */
    private fun extendSelectionBy(ic: android.view.inputmethod.InputConnection, forward: Boolean, count: Int) {
        val keyCode = if (forward) android.view.KeyEvent.KEYCODE_DPAD_RIGHT else android.view.KeyEvent.KEYCODE_DPAD_LEFT
        repeat(count) {
            val meta = android.view.KeyEvent.META_SHIFT_ON
            ic.sendKeyEvent(android.view.KeyEvent(0, 0, android.view.KeyEvent.ACTION_DOWN, keyCode, 0, meta))
            ic.sendKeyEvent(android.view.KeyEvent(0, 0, android.view.KeyEvent.ACTION_UP, keyCode, 0, meta))
        }
    }
}
