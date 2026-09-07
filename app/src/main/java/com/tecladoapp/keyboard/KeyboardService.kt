package com.tecladoapp.keyboard

import android.content.ClipData
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

    override fun onEvaluateFullscreenMode(): Boolean {
        // Un teclado personalizado nunca debe usar el modo de pantalla completa
        // de Android (el editor gigante que aplasta el teclado real).
        return false
    }

    override fun onCreateInputView(): View {
        val theme = ThemeCatalog.theme(prefs.themeId)
        val font = ThemeCatalog.font(prefs.fontId)

        rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(theme.backgroundColor)
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        toolbar = buildToolbar(theme)
        rootLayout.addView(toolbar)

        suggestionBar = SuggestionBarView(this).apply {
            onSuggestionTap = { word -> replaceCurrentWord(word) }
        }
        rootLayout.addView(suggestionBar)

        contentContainer = FrameLayout(this)
        contentContainer.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        rootLayout.addView(contentContainer)

        keyboardView = KeyboardView(this).apply {
            listener = this@KeyboardService
            this.theme = theme
            this.fontFamily = font.fontFamily
            this.heightScale = prefs.keyboardHeightScale
        }
        showKeyboardView()

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
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        fun iconButton(resId: Int, onClick: () -> Unit) = android.widget.ImageView(this).apply {
            setImageResource(resId)
            setColorFilter(theme.accentColor)
            layoutParams = LinearLayout.LayoutParams(72, 72).apply { marginStart = 16 }
            setPadding(12, 12, 12, 12)
            setOnClickListener { onClick() }
        }
        bar.addView(spacer)
        bar.addView(iconButton(R.drawable.ic_clipboard) { showClipboardPanel() })
        bar.addView(iconButton(R.drawable.ic_emoji) { showEmojiPanel() })
        bar.addView(iconButton(R.drawable.ic_settings) { openSettings() })
        return bar
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
        contentContainer.removeAllViews()
        contentContainer.addView(keyboardView)
        suggestionBar.visibility = if (prefs.wordSuggestionsEnabled) View.VISIBLE else View.GONE
    }

    private fun showEmojiPanel() {
        val panel = EmojiPanelView(this, prefs)
        panel.listener = object : EmojiPanelView.Listener {
            override fun onEmojiSelected(emoji: String) {
                currentInputConnection?.commitText(emoji, 1)
            }
            override fun onClose() { showKeyboardView() }
        }
        contentContainer.removeAllViews()
        contentContainer.addView(panel)
        suggestionBar.visibility = View.GONE
    }

    private fun showClipboardPanel() {
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
        val theme = ThemeCatalog.theme(prefs.themeId)
        val font = ThemeCatalog.font(prefs.fontId)
        keyboardView.theme = theme
        keyboardView.fontFamily = font.fontFamily
        keyboardView.heightScale = prefs.keyboardHeightScale
        showKeyboardView()
        updateSuggestions()
    }

    // ---------- KeyboardListener ----------
    override fun onKeyChar(char: Char) {
        currentInputConnection?.commitText(char.toString(), 1)
        updateSuggestions()
    }

    override fun onBackspace() {
        currentInputConnection?.deleteSurroundingText(1, 0)
        updateSuggestions()
    }

    override fun onEnter() {
        currentInputConnection?.sendKeyEvent(
            android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER)
        )
        currentInputConnection?.sendKeyEvent(
            android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_ENTER)
        )
    }

    override fun onSpace() {
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
        moveCursorByWord(direction, extendSelection)
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
