package com.tecladoapp.keyboard

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import kotlin.math.abs

interface KeyboardListener {
    fun onKeyChar(char: Char)
    fun onBackspace()
    fun onEnter()
    fun onSpace()
    fun onShiftToggled(active: Boolean)
    fun onSwitchToSymbols(showSymbols: Boolean)
    fun onOpenEmoji()
    fun onOpenClipboard()
    /** direction: -1 palabra a la izquierda, +1 palabra a la derecha */
    fun onEnterSwipeWord(direction: Int, extendSelection: Boolean)
}

/**
 * Teclado construido con botones reales de Android (no Canvas), para máxima
 * fiabilidad de medida/layout en cualquier dispositivo.
 */
class KeyboardView(context: Context) : LinearLayout(context) {

    var listener: KeyboardListener? = null

    var theme: KeyboardTheme = ThemeCatalog.themes[0]
        set(value) { field = value; rebuild() }
    var fontFamily: String? = null
        set(value) { field = value; rebuild() }
    var heightScale: Float = 1.0f
        set(value) { field = value; rebuild() }

    private var shiftOn = false
    private var showSymbols = false

    private val density = context.resources.displayMetrics.density
    private val baseRowHeightDp = 46f

    init {
        orientation = VERTICAL
        minimumHeight = (baseRowHeightDp * density * 5).toInt() // respaldo defensivo
        rebuild()
    }

    private fun px(dp: Float) = (dp * density).toInt()

    private fun rebuild() {
        removeAllViews()
        val rowH = px(baseRowHeightDp * heightScale)

        if (!showSymbols) {
            addRow(rowH, KeyDefs.numberRow)
            addRow(rowH, KeyDefs.lettersRow1())
            addRow(rowH, KeyDefs.lettersRow2(), sidePaddingWeight = 0.5f)
            val row3 = mutableListOf<Key>()
            row3.add(shiftKey())
            row3.addAll(KeyDefs.lettersRow3())
            row3.add(backspaceKey())
            addRow(rowH, row3)
        } else {
            addRow(rowH, KeyDefs.symbolsRow1)
            addRow(rowH, KeyDefs.symbolsRow2)
            val row3 = mutableListOf<Key>()
            row3.add(shiftKey())
            row3.addAll(KeyDefs.symbolsRow3)
            row3.add(backspaceKey())
            addRow(rowH, row3)
        }

        val bottomRow = listOf(
            Key(CODE_SYMBOLS, if (showSymbols) "ABC" else "?123", weight = 1.4f, isFunctionKey = true),
            Key(CODE_EMOJI, "", weight = 1f, isFunctionKey = true, iconRes = R.drawable.ic_emoji),
            Key(CODE_CLIPBOARD, "", weight = 1f, isFunctionKey = true, iconRes = R.drawable.ic_clipboard),
            Key(CODE_SPACE, "espacio", weight = 3.2f, isFunctionKey = true),
            Key('.'.code, ".", weight = 0.8f),
            Key(CODE_ENTER, "", weight = 1.6f, isFunctionKey = true, iconRes = R.drawable.ic_enter)
        )
        addRow(rowH, bottomRow, isBottomRow = true)
    }

    private fun shiftKey() = Key(
        CODE_SHIFT, "", weight = 1.5f, isFunctionKey = true,
        iconRes = if (shiftOn) R.drawable.ic_shift_active else R.drawable.ic_shift
    )

    private fun backspaceKey() = Key(CODE_BACKSPACE, "", weight = 1.5f, isFunctionKey = true, iconRes = R.drawable.ic_backspace)

    private fun addRow(rowH: Int, keys: List<Key>, sidePaddingWeight: Float = 0f, isBottomRow: Boolean = false) {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, rowH)
        }
        if (sidePaddingWeight > 0f) row.addView(spacer(sidePaddingWeight))
        keys.forEach { key -> row.addView(makeKeyView(key, rowH, isBottomRow)) }
        if (sidePaddingWeight > 0f) row.addView(spacer(sidePaddingWeight))
        addView(row)
    }

    private fun spacer(weight: Float): View = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight)
    }

    private fun keyBackground(normalColor: Int, pressedColor: Int): StateListDrawable {
        val normal = GradientDrawable().apply { setColor(normalColor); cornerRadius = px(6f).toFloat() }
        val pressed = GradientDrawable().apply { setColor(pressedColor); cornerRadius = px(6f).toFloat() }
        val sld = StateListDrawable()
        sld.addState(intArrayOf(android.R.attr.state_pressed), pressed)
        sld.addState(intArrayOf(), normal)
        return sld
    }

    private fun applyMargins(view: View, weight: Float, height: Int) {
        val m = px(2.5f)
        view.layoutParams = LinearLayout.LayoutParams(0, height, weight).apply {
            setMargins(m, m, m, m)
        }
    }

    private fun makeKeyView(key: Key, rowH: Int, isBottomRow: Boolean): View {
        val functionBg = keyBackground(darken(theme.accentColor), theme.accentColor)
        val normalBg = keyBackground(theme.keyColor, theme.accentColor)
        val tf = fontFamily?.let { Typeface.create(it, Typeface.NORMAL) } ?: Typeface.DEFAULT

        if (key.iconRes != null) {
            val btn = ImageButton(context)
            btn.setImageResource(key.iconRes)
            btn.setColorFilter(theme.backgroundColor)
            btn.background = functionBg
            btn.scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
            val pad = px(12f)
            btn.setPadding(pad, pad, pad, pad)
            applyMargins(btn, key.weight, rowH)
            wireKey(btn, key)
            return btn
        }

        val btn = Button(context)
        btn.text = if (key.code == CODE_SPACE) "" else displayLabel(key)
        btn.isAllCaps = false
        btn.typeface = tf
        btn.textSize = 16f
        btn.setPadding(0, 0, 0, 0)
        btn.minWidth = 0
        btn.minimumWidth = 0
        btn.minHeight = 0
        btn.minimumHeight = 0
        btn.gravity = Gravity.CENTER
        btn.stateListAnimator = null
        btn.elevation = 0f
        if (key.isFunctionKey) {
            btn.background = functionBg
            btn.setTextColor(theme.backgroundColor)
        } else {
            btn.background = normalBg
            btn.setTextColor(theme.keyTextColor)
        }
        applyMargins(btn, key.weight, rowH)
        wireKey(btn, key)
        return btn
    }

    private fun displayLabel(key: Key): String {
        if (key.altLabel != null && shiftOn) return key.altLabel
        return key.label
    }

    private fun darken(color: Int): Int {
        val a = (color shr 24) and 0xFF
        val r = ((color shr 16) and 0xFF) * 0.75
        val g = ((color shr 8) and 0xFF) * 0.75
        val b = (color and 0xFF) * 0.75
        return (a shl 24) or (r.toInt() shl 16) or (g.toInt() shl 8) or b.toInt()
    }

    private fun wireKey(view: View, key: Key) {
        if (key.code == CODE_ENTER) {
            wireEnterKey(view)
            return
        }
        val alternates = AccentMaps.forKey(key.code, key.isFunctionKey)
        if (alternates.isNotEmpty()) {
            wireKeyWithAccentPopup(view, key, alternates)
        } else {
            view.setOnClickListener {
                vibrate()
                handleTap(key)
            }
        }
    }

    // --- popup de acentos/símbolos: mantener presionado + deslizar para elegir ---
    private val longPressHandler = Handler(Looper.getMainLooper())
    private var activePopup: PopupWindow? = null
    private var activePopupItems: List<TextView> = emptyList()
    private var activePopupChars: List<Char> = emptyList()
    private var selectedPopupIndex = 0

    private fun wireKeyWithAccentPopup(view: View, key: Key, alternates: List<Char>) {
        var longPressTriggered = false
        var pendingLongPress: Runnable? = null

        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    longPressTriggered = false
                    vibrate()
                    val r = Runnable {
                        longPressTriggered = true
                        showAccentPopup(v, alternates)
                        selectedPopupIndex = 0
                        highlightPopupIndex(0)
                    }
                    pendingLongPress = r
                    longPressHandler.postDelayed(r, 350)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (longPressTriggered) {
                        val itemW = v.width.coerceAtLeast(1)
                        var idx = (event.x / itemW).toInt()
                        if (idx < 0) idx = 0
                        if (idx > activePopupChars.lastIndex) idx = activePopupChars.lastIndex
                        if (idx != selectedPopupIndex) {
                            selectedPopupIndex = idx
                            highlightPopupIndex(idx)
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    pendingLongPress?.let { longPressHandler.removeCallbacks(it) }
                    if (longPressTriggered) {
                        val ch = activePopupChars.getOrNull(selectedPopupIndex)
                        dismissAccentPopup()
                        if (ch != null) commitChar(ch)
                    } else {
                        handleTap(key)
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    pendingLongPress?.let { longPressHandler.removeCallbacks(it) }
                    dismissAccentPopup()
                    true
                }
                else -> false
            }
        }
    }

    private fun showAccentPopup(anchor: View, chars: List<Char>) {
        activePopupChars = chars
        val itemW = anchor.width.coerceAtLeast(px(40f))
        val itemH = px(48f)
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = px(8f).toFloat()
            }
        }
        val tvs = chars.map { ch ->
            TextView(context).apply {
                text = ch.toString()
                textSize = 18f
                gravity = Gravity.CENTER
                setTextColor(Color.BLACK)
                layoutParams = LinearLayout.LayoutParams(itemW, itemH)
            }
        }
        tvs.forEach { row.addView(it) }
        activePopupItems = tvs

        val pw = PopupWindow(row, itemW * chars.size, itemH, false)
        pw.isOutsideTouchable = true
        activePopup = pw
        pw.showAsDropDown(anchor, 0, -(anchor.height + itemH))
    }

    private fun highlightPopupIndex(index: Int) {
        activePopupItems.forEachIndexed { i, tv ->
            if (i == index) {
                tv.setBackgroundColor(theme.accentColor)
                tv.setTextColor(Color.WHITE)
            } else {
                tv.setBackgroundColor(Color.WHITE)
                tv.setTextColor(Color.BLACK)
            }
        }
    }

    private fun dismissAccentPopup() {
        activePopup?.dismiss()
        activePopup = null
        activePopupItems = emptyList()
        activePopupChars = emptyList()
    }

    private fun commitChar(ch: Char) {
        val c = if (shiftOn && ch.isLetter()) ch.uppercaseChar() else ch
        listener?.onKeyChar(c)
        if (shiftOn) { shiftOn = false; listener?.onShiftToggled(false); rebuild() }
    }

    private fun handleTap(key: Key) {
        when (key.code) {
            CODE_SHIFT -> { shiftOn = !shiftOn; listener?.onShiftToggled(shiftOn); rebuild() }
            CODE_BACKSPACE -> listener?.onBackspace()
            CODE_SPACE -> listener?.onSpace()
            CODE_SYMBOLS -> { showSymbols = !showSymbols; listener?.onSwitchToSymbols(showSymbols); rebuild() }
            CODE_EMOJI -> listener?.onOpenEmoji()
            CODE_CLIPBOARD -> listener?.onOpenClipboard()
            else -> {
                var c = key.code.toChar()
                if (shiftOn && key.altLabel != null) c = key.altLabel.first()
                listener?.onKeyChar(c)
                if (shiftOn) { shiftOn = false; listener?.onShiftToggled(false); rebuild() }
            }
        }
    }

    // --- gesto de deslizar el Enter: mover/seleccionar palabra por palabra ---
    private fun wireEnterKey(view: View) {
        var startX = 0f
        var accum = 0f
        var moved = false
        val thresholdPx = px(40f)

        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.x
                    accum = 0f
                    moved = false
                    vibrate()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.x - startX
                    startX = event.x
                    accum += dx
                    while (abs(accum) >= thresholdPx) {
                        val dir = if (accum > 0) 1 else -1
                        listener?.onEnterSwipeWord(dir, extendSelection = moved)
                        accum -= dir * thresholdPx
                        moved = true
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) listener?.onEnter()
                    true
                }
                MotionEvent.ACTION_CANCEL -> true
                else -> false
            }
        }
    }

    private fun vibrate() {
        val vib = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (vib.hasVibrator()) {
            vib.vibrate(VibrationEffect.createOneShot(8, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }
}
