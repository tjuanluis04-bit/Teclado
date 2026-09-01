package com.tecladoapp.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.MotionEvent
import android.view.View
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

class KeyboardView(context: Context) : View(context) {

    var listener: KeyboardListener? = null
    var theme: KeyboardTheme = ThemeCatalog.themes[0]
        set(value) { field = value; invalidate() }
    var fontFamily: String? = null
        set(value) { field = value; invalidate() }
    var heightScale: Float = 1.0f
        set(value) { field = value; requestLayout() }

    private var shiftOn = false
    private var showSymbols = false
    private var showNumberRow = true

    private data class LaidKey(val key: Key, val rect: RectF)
    private val rows = mutableListOf<List<LaidKey>>()

    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val bgPaint = Paint()

    private val baseRowHeightDp = 46f
    private val density = context.resources.displayMetrics.density

    private var pressedRect: RectF? = null

    // --- gesto de deslizar el Enter ---
    private var enterSwipeActive = false
    private var enterSwipeStartX = 0f
    private var enterSwipeAccum = 0f
    private val wordSwipeThresholdPx = 40 * density

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val rowsCount = 5 // numeros + 3 filas letras + fila inferior
        val rowH = baseRowHeightDp * density * heightScale
        val height = (rowH * rowsCount).toInt()
        layoutKeys(width, height, rowH)
        setMeasuredDimension(width, height)
    }

    private fun currentLetterRows(): List<List<Key>> = listOf(
        KeyDefs.lettersRow1(), KeyDefs.lettersRow2(), KeyDefs.lettersRow3()
    )

    private fun layoutKeys(width: Int, height: Int, rowH: Float) {
        rows.clear()
        var y = 0f

        fun layoutRow(keys: List<Key>, leftPad: Float = 0f, rightPad: Float = 0f): List<LaidKey> {
            val totalWeight = keys.sumOf { it.weight.toDouble() }.toFloat()
            val usable = width - leftPad - rightPad
            var x = leftPad
            val laid = mutableListOf<LaidKey>()
            keys.forEach { k ->
                val w = usable * (k.weight / totalWeight)
                laid.add(LaidKey(k, RectF(x, y, x + w, y + rowH)))
                x += w
            }
            return laid
        }

        if (showNumberRow && !showSymbols) {
            rows.add(layoutRow(KeyDefs.numberRow))
            y += rowH
        }

        if (!showSymbols) {
            val (r1, r2, r3) = Triple(KeyDefs.lettersRow1(), KeyDefs.lettersRow2(), KeyDefs.lettersRow3())
            rows.add(layoutRow(r1))
            y += rowH
            rows.add(layoutRow(r2, leftPad = width * 0.05f, rightPad = width * 0.05f))
            y += rowH
            val row3keys = mutableListOf<Key>()
            row3keys.add(Key(CODE_SHIFT, "⇧", weight = 1.5f, isFunctionKey = true))
            row3keys.addAll(r3)
            row3keys.add(Key(CODE_BACKSPACE, "⌫", weight = 1.5f, isFunctionKey = true))
            rows.add(layoutRow(row3keys))
            y += rowH
        } else {
            rows.add(layoutRow(KeyDefs.symbolsRow1))
            y += rowH
            rows.add(layoutRow(KeyDefs.symbolsRow2))
            y += rowH
            val row3keys = mutableListOf<Key>()
            row3keys.add(Key(CODE_SHIFT, "⇧", weight = 1.5f, isFunctionKey = true))
            row3keys.addAll(KeyDefs.symbolsRow3)
            row3keys.add(Key(CODE_BACKSPACE, "⌫", weight = 1.5f, isFunctionKey = true))
            rows.add(layoutRow(row3keys))
            y += rowH
        }

        val bottomRow = listOf(
            Key(CODE_SYMBOLS, if (showSymbols) "ABC" else "?123", weight = 1.4f, isFunctionKey = true),
            Key(CODE_EMOJI, "🙂", weight = 1f, isFunctionKey = true),
            Key(CODE_CLIPBOARD, "📋", weight = 1f, isFunctionKey = true),
            Key(CODE_SPACE, "espacio", weight = 3.2f, isFunctionKey = true),
            Key(CODE_PERIOD, ".", weight = 0.8f),
            Key(CODE_ENTER, "⏎", weight = 1.6f, isFunctionKey = true)
        )
        rows.add(layoutRow(bottomRow))
    }

    override fun onDraw(canvas: Canvas) {
        bgPaint.color = theme.backgroundColor
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val tf = fontFamily?.let { Typeface.create(it, Typeface.NORMAL) } ?: Typeface.DEFAULT
        textPaint.typeface = tf

        rows.forEach { row ->
            row.forEach { laid ->
                val margin = 2.5f * density
                val r = RectF(laid.rect.left + margin, laid.rect.top + margin, laid.rect.right - margin, laid.rect.bottom - margin)
                keyPaint.color = if (laid.key.isFunctionKey) darken(theme.accentColor) else theme.keyColor
                if (pressedRect === laid.rect) keyPaint.color = theme.accentColor
                canvas.drawRoundRect(r, 10f * density, 10f * density, keyPaint)

                textPaint.color = if (laid.key.isFunctionKey) theme.backgroundColor else theme.keyTextColor
                textPaint.textSize = 16f * density
                val label = displayLabel(laid.key)
                canvas.drawText(label, r.centerX(), r.centerY() - (textPaint.descent() + textPaint.ascent()) / 2, textPaint)
            }
        }
    }

    private fun displayLabel(key: Key): String {
        if (key.code == CODE_SPACE) return ""
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

    private fun findKeyAt(x: Float, y: Float): LaidKey? {
        rows.forEach { row -> row.forEach { if (it.rect.contains(x, y)) return it } }
        return null
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val laid = findKeyAt(event.x, event.y)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pressedRect = laid?.rect
                invalidate()
                if (laid?.key?.code == CODE_ENTER) {
                    enterSwipeActive = true
                    enterSwipeStartX = event.x
                    enterSwipeAccum = 0f
                }
                vibrate()
            }
            MotionEvent.ACTION_MOVE -> {
                if (enterSwipeActive) {
                    val dx = event.x - enterSwipeStartX
                    enterSwipeStartX = event.x
                    enterSwipeAccum += dx
                    while (abs(enterSwipeAccum) >= wordSwipeThresholdPx) {
                        val dir = if (enterSwipeAccum > 0) 1 else -1
                        listener?.onEnterSwipeWord(dir, extendSelection = enterMoved)
                        enterSwipeAccum -= dir * wordSwipeThresholdPx
                        enterMoved = true // a partir del 2º salto de palabra, se extiende la selección
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                if (laid != null && laid.key.code == CODE_ENTER) {
                    if (!enterMoved) {
                        // no hubo deslizamiento suficiente: es un toque normal de Enter
                        listener?.onEnter()
                    }
                } else if (laid != null) {
                    handleTap(laid.key)
                }
                enterSwipeActive = false
                enterMoved = false
                enterSwipeAccum = 0f
                pressedRect = null
                invalidate()
            }
            MotionEvent.ACTION_CANCEL -> {
                enterSwipeActive = false
                enterMoved = false
                enterSwipeAccum = 0f
                pressedRect = null
                invalidate()
            }
        }
        return true
    }

    private var enterMoved = false

    private fun handleTap(key: Key) {
        when (key.code) {
            CODE_SHIFT -> { shiftOn = !shiftOn; listener?.onShiftToggled(shiftOn) }
            CODE_BACKSPACE -> listener?.onBackspace()
            CODE_SPACE -> listener?.onSpace()
            CODE_SYMBOLS -> { showSymbols = !showSymbols; requestLayout(); listener?.onSwitchToSymbols(showSymbols) }
            CODE_EMOJI -> listener?.onOpenEmoji()
            CODE_CLIPBOARD -> listener?.onOpenClipboard()
            CODE_PERIOD -> listener?.onKeyChar('.')
            else -> {
                var c = key.code.toChar()
                if (shiftOn && key.altLabel != null) c = key.altLabel.first()
                listener?.onKeyChar(c)
                if (shiftOn) { shiftOn = false; listener?.onShiftToggled(false) }
            }
        }
    }

    private fun vibrate() {
        val vib = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (vib.hasVibrator()) {
            vib.vibrate(VibrationEffect.createOneShot(8, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    fun refreshLayoutMode() { requestLayout(); invalidate() }
}
