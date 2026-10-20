package com.tecladoapp.keyboard

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

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
    /** direction: -1 un carácter a la izquierda, +1 un carácter a la derecha (deslizar la barra espaciadora) */
    fun onCursorMove(direction: Int)
    /** Borra una palabra completa: -1 hacia atrás, +1 hacia adelante. */
    fun onDeleteWord(direction: Int)
    /**
     * Gesto combinado en la barra espaciadora. dir: 0 izquierda, 1 derecha, 2 arriba, 3 abajo.
     * delete=true borra lo abarcado (Borrar+Enter+Espacio); false lo selecciona (Shift+Espacio).
     */
    fun onSpanSwipe(dir: Int, delete: Boolean)
}

/**
 * Teclado construido con botones reales de Android. Las teclas se accionan al tocar
 * (sin depender de "click") para que escribir rápido no pierda pulsaciones, y los
 * cambios de mayúsculas se actualizan en sitio sin reconstruir las vistas.
 */
class KeyboardView(context: Context) : LinearLayout(context) {

    companion object {
        const val BASE_ROW_DP = 56f
    }

    var listener: KeyboardListener? = null

    var theme: KeyboardTheme = ThemeCatalog.themes[0]
        set(value) { if (field != value) { field = value; rebuild() } }
    var fontFamily: String? = null
        set(value) { if (field != value) { field = value; rebuild() } }
    var heightScale: Float = 1.0f
        set(value) { if (field != value) { field = value; rebuild() } }

    private var shiftOn = false
    private var capsLock = false
    private var autoShift = false
    private var lastShiftTapTime = 0L
    private var showSymbols = false
    private var symbolsSecondPage = false

    // teclas físicas mantenidas (para los gestos combinados)
    private var backspaceHeld = false
    private var enterHeld = false
    private var shiftHeld = false
    private var backspaceComboUsed = false
    private var enterComboUsed = false
    private var shiftComboUsed = false
    private var cancelBackspaceRepeat: (() -> Unit)? = null

    private val density = context.resources.displayMetrics.density
    private val keyViews = mutableListOf<Pair<Key, View>>()
    private val uiHandler = Handler(Looper.getMainLooper())

    init {
        orientation = VERTICAL
        isMotionEventSplittingEnabled = true
        minimumHeight = (BASE_ROW_DP * density * 5).toInt()
        rebuild()
    }

    private fun px(dp: Float) = (dp * density).toInt()

    // ---------- estado público ----------
    fun resetToDefault() {
        val changedLayout = showSymbols || symbolsSecondPage
        val changedShift = shiftOn || capsLock || autoShift
        shiftOn = false; capsLock = false; autoShift = false
        showSymbols = false; symbolsSecondPage = false
        backspaceHeld = false; enterHeld = false; shiftHeld = false
        dismissAccentPopup()
        if (changedLayout) rebuild() else if (changedShift) refreshState()
    }

    /** Mayúscula automática al empezar oración. No pisa un Bloq Mayús ni un shift manual. */
    fun setAutoShift(on: Boolean) {
        if (capsLock) return
        if (on && !shiftOn) {
            shiftOn = true; autoShift = true; refreshState()
        } else if (!on && autoShift && shiftOn) {
            shiftOn = false; autoShift = false; refreshState()
        }
    }

    // ---------- construcción ----------
    private fun rebuild() {
        removeAllViews()
        keyViews.clear()
        val rowH = px(BASE_ROW_DP * heightScale)

        if (!showSymbols) {
            addRow(rowH, KeyDefs.numberRow)
            addRow(rowH, KeyDefs.lettersRow1())
            addRow(rowH, KeyDefs.lettersRow2(), sidePaddingWeight = 0.5f)
            val row3 = mutableListOf<Key>()
            row3.add(shiftKey())
            row3.addAll(KeyDefs.lettersRow3())
            row3.add(backspaceKey())
            addRow(rowH, row3)
        } else if (!symbolsSecondPage) {
            addRow(rowH, KeyDefs.numberRow)
            addRow(rowH, KeyDefs.symbolsPage1Row1)
            addRow(rowH, KeyDefs.symbolsPage1Row2)
            val row3 = mutableListOf<Key>()
            row3.add(symbolsPageToggleKey())
            row3.addAll(KeyDefs.symbolsPage1Row3)
            row3.add(backspaceKey())
            addRow(rowH, row3)
        } else {
            addRow(rowH, KeyDefs.numberRow)
            addRow(rowH, KeyDefs.symbolsPage2Row1)
            addRow(rowH, KeyDefs.symbolsPage2Row2)
            val row3 = mutableListOf<Key>()
            row3.add(symbolsPageToggleKey())
            row3.addAll(KeyDefs.symbolsPage2Row3)
            row3.add(backspaceKey())
            addRow(rowH, row3)
        }

        val bottomRow = listOf(
            Key(CODE_SYMBOLS, if (showSymbols) "ABC" else "?123", weight = 1.5f, isFunctionKey = true),
            Key(','.code, ",", weight = 0.9f),
            Key(CODE_SPACE, "", weight = 3.4f, isFunctionKey = true),
            Key('@'.code, "@", weight = 0.9f),
            Key('.'.code, ".", weight = 0.9f),
            Key(CODE_ENTER, "", weight = 1.6f, isFunctionKey = true, iconRes = R.drawable.ic_enter)
        )
        addRow(rowH, bottomRow)
    }

    /** Actualiza etiquetas/icono de shift sin reconstruir (rápido y sin perder toques). */
    private fun refreshState() {
        val upper = shiftOn || capsLock
        for ((key, view) in keyViews) {
            if (view is Button && key.altLabel != null) {
                view.text = if (upper) key.altLabel else key.label
            } else if (view is ImageButton && key.code == CODE_SHIFT) {
                view.setImageResource(if (upper) R.drawable.ic_shift_active else R.drawable.ic_shift)
                view.background = if (capsLock) keyBackground(theme.accentColor, theme.accentColor) else functionBg()
            }
        }
    }

    private fun shiftKey() = Key(
        CODE_SHIFT, "", weight = 1.5f, isFunctionKey = true,
        iconRes = if (shiftOn || capsLock) R.drawable.ic_shift_active else R.drawable.ic_shift
    )

    private fun backspaceKey() = Key(CODE_BACKSPACE, "", weight = 1.5f, isFunctionKey = true, iconRes = R.drawable.ic_backspace)

    private fun symbolsPageToggleKey() = Key(
        CODE_SYMBOLS_PAGE,
        if (symbolsSecondPage) "?123" else "=\\<",
        weight = 1.5f, isFunctionKey = true
    )

    private fun addRow(rowH: Int, keys: List<Key>, sidePaddingWeight: Float = 0f) {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            isMotionEventSplittingEnabled = true
            layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, rowH)
        }
        if (sidePaddingWeight > 0f) row.addView(spacer(sidePaddingWeight))
        keys.forEach { key -> row.addView(makeKeyView(key, rowH)) }
        if (sidePaddingWeight > 0f) row.addView(spacer(sidePaddingWeight))
        addView(row)
    }

    private fun spacer(weight: Float): View = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight)
    }

    private fun keyBackground(normalColor: Int, pressedColor: Int): StateListDrawable {
        val r = px(10f).toFloat()
        val normal = GradientDrawable().apply { setColor(normalColor); cornerRadius = r }
        val pressed = GradientDrawable().apply { setColor(pressedColor); cornerRadius = r }
        val sld = StateListDrawable()
        sld.addState(intArrayOf(android.R.attr.state_pressed), pressed)
        sld.addState(intArrayOf(), normal)
        return sld
    }

    private fun functionBg() = keyBackground(darken(theme.accentColor), theme.accentColor)

    /** Separación como la imagen de referencia: poco espacio horizontal, más vertical. */
    private fun applyMargins(view: View, weight: Float, height: Int) {
        val mh = px(3f)
        val mv = px(5f)
        view.layoutParams = LinearLayout.LayoutParams(0, height, weight).apply { setMargins(mh, mv, mh, mv) }
    }

    private fun baseTypeface(): Typeface {
        val tf = fontFamily?.let { Typeface.create(it, Typeface.NORMAL) } ?: Typeface.DEFAULT
        return if (Build.VERSION.SDK_INT >= 28) Typeface.create(tf, 500, false) else tf
    }

    private fun makeKeyView(key: Key, rowH: Int): View {
        val normalBg = keyBackground(theme.keyColor, theme.accentColor)

        if (key.iconRes != null) {
            val btn = ImageButton(context)
            btn.setImageResource(key.iconRes)
            btn.setColorFilter(theme.backgroundColor)
            btn.background = if (key.code == CODE_SHIFT && capsLock) keyBackground(theme.accentColor, theme.accentColor) else functionBg()
            btn.scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
            val pad = px(10f)
            btn.setPadding(pad, pad, pad, pad)
            applyMargins(btn, key.weight, rowH)
            wireKey(btn, key)
            keyViews.add(key to btn)
            return btn
        }

        val btn = Button(context)
        btn.text = if (key.code == CODE_SPACE) "" else displayLabel(key)
        btn.isAllCaps = false
        btn.typeface = baseTypeface()
        btn.textSize = if (key.isFunctionKey) 18f else 25f
        btn.includeFontPadding = false
        btn.setPadding(0, 0, 0, 0)
        btn.minWidth = 0
        btn.minimumWidth = 0
        btn.minHeight = 0
        btn.minimumHeight = 0
        btn.gravity = Gravity.CENTER
        btn.stateListAnimator = null
        btn.elevation = 0f
        if (key.isFunctionKey) {
            btn.background = functionBg()
            btn.setTextColor(theme.backgroundColor)
        } else {
            btn.background = normalBg
            btn.setTextColor(theme.keyTextColor)
        }
        applyMargins(btn, key.weight, rowH)
        wireKey(btn, key)
        keyViews.add(key to btn)
        return btn
    }

    private fun displayLabel(key: Key): String {
        if (key.altLabel != null && (shiftOn || capsLock)) return key.altLabel
        return key.label
    }

    private fun darken(color: Int): Int {
        val a = (color shr 24) and 0xFF
        val r = ((color shr 16) and 0xFF) * 0.75
        val g = ((color shr 8) and 0xFF) * 0.75
        val b = (color and 0xFF) * 0.75
        return (a shl 24) or (r.toInt() shl 16) or (g.toInt() shl 8) or b.toInt()
    }

    // ---------- cableado de teclas ----------
    private fun wireKey(view: View, key: Key) {
        when (key.code) {
            CODE_ENTER -> { wireEnterKey(view); return }
            CODE_BACKSPACE -> { wireBackspaceKey(view); return }
            CODE_SPACE -> { wireSpaceKey(view); return }
            CODE_SHIFT -> { wireShiftKey(view); return }
        }
        val alternates = AccentMaps.forKey(key.code, key.isFunctionKey)
        if (alternates.isNotEmpty()) {
            wireKeyWithAccentPopup(view, key, alternates)
        } else {
            wirePlainKey(view, key)
        }
    }

    /** Tecla simple: actúa al instante al tocar (menor latencia); las de cambio de panel, al soltar. */
    private fun wirePlainKey(view: View, key: Key) {
        val onUp = key.code == CODE_SYMBOLS || key.code == CODE_SYMBOLS_PAGE
        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                    v.isPressed = true
                    haptic(v)
                    if (!onUp) handleTap(key)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    v.isPressed = false
                    if (onUp) handleTap(key)
                    true
                }
                MotionEvent.ACTION_CANCEL -> { v.isPressed = false; true }
                else -> true
            }
        }
    }

    private fun handleTap(key: Key) {
        when (key.code) {
            CODE_SYMBOLS -> {
                showSymbols = !showSymbols; symbolsSecondPage = false
                listener?.onSwitchToSymbols(showSymbols); rebuild()
            }
            CODE_SYMBOLS_PAGE -> { symbolsSecondPage = !symbolsSecondPage; rebuild() }
            CODE_EMOJI -> listener?.onOpenEmoji()
            CODE_CLIPBOARD -> listener?.onOpenClipboard()
            else -> {
                var c = key.code.toChar()
                if ((shiftOn || capsLock) && key.altLabel != null) c = key.altLabel.first()
                listener?.onKeyChar(c)
                consumeOneShotShift()
            }
        }
    }

    private fun consumeOneShotShift() {
        if (shiftOn && !capsLock) {
            shiftOn = false; autoShift = false
            refreshState()
            listener?.onShiftToggled(false)
        }
    }

    // ---------- Shift ----------
    private fun wireShiftKey(view: View) {
        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                    v.isPressed = true
                    haptic(v)
                    shiftHeld = true
                    shiftComboUsed = false
                    true
                }
                MotionEvent.ACTION_UP -> {
                    v.isPressed = false
                    shiftHeld = false
                    if (!shiftComboUsed) handleShiftTap()
                    true
                }
                MotionEvent.ACTION_CANCEL -> { v.isPressed = false; shiftHeld = false; true }
                else -> true
            }
        }
    }

    /** Un toque alterna mayúsculas; doble toque activa Bloq Mayús. */
    private fun handleShiftTap() {
        val now = System.currentTimeMillis()
        autoShift = false
        if (now - lastShiftTapTime < 350) {
            capsLock = !capsLock
            shiftOn = capsLock
        } else if (capsLock) {
            capsLock = false
            shiftOn = false
        } else {
            shiftOn = !shiftOn
        }
        lastShiftTapTime = now
        refreshState()
        listener?.onShiftToggled(shiftOn)
    }

    // ---------- popup de acentos/símbolos ----------
    private var activePopup: PopupWindow? = null
    private var activePopupItems: List<TextView> = emptyList()
    private var activePopupChars: List<Char> = emptyList()
    private var popupColumns = 1
    private var popupItemW = 1
    private var popupItemH = 1
    private var selectedPopupIndex = 0
    private var popupStartCol = 0

    private fun wireKeyWithAccentPopup(view: View, key: Key, alternates: List<Char>) {
        var longPressTriggered = false
        var pendingLongPress: Runnable? = null
        var baseX = 0f
        var baseY = 0f
        var lastX = 0f
        var lastY = 0f

        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                    longPressTriggered = false
                    v.isPressed = true
                    haptic(v)
                    lastX = event.rawX; lastY = event.rawY
                    val r = Runnable {
                        longPressTriggered = true
                        baseX = lastX; baseY = lastY
                        showAccentPopup(v, alternates)
                        selectedPopupIndex = popupStartIndex()
                        highlightPopupIndex(selectedPopupIndex)
                    }
                    pendingLongPress = r
                    uiHandler.postDelayed(r, 320)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    lastX = event.rawX; lastY = event.rawY
                    if (longPressTriggered) {
                        // Selección RELATIVA: deslizar desde cualquier parte del teclado
                        // (izquierda/derecha/arriba/abajo) mueve la selección.
                        val dCol = ((lastX - baseX) / popupItemW).roundToInt()
                        val dRow = ((baseY - lastY) / popupItemH).roundToInt() // arriba = fila superior
                        val n = activePopupChars.size
                        val rows = ceil(n / popupColumns.toFloat()).toInt()
                        val row = (0 + dRow).coerceIn(0, rows - 1)
                        val inRow = if (row == rows - 1) n - row * popupColumns else popupColumns
                        val col = (popupStartCol + dCol).coerceIn(0, (inRow - 1).coerceAtLeast(0))
                        val idx = (row * popupColumns + col).coerceIn(0, n - 1)
                        if (idx != selectedPopupIndex) {
                            selectedPopupIndex = idx
                            highlightPopupIndex(idx)
                            haptic(v)
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    v.isPressed = false
                    pendingLongPress?.let { uiHandler.removeCallbacks(it) }
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
                    v.isPressed = false
                    pendingLongPress?.let { uiHandler.removeCallbacks(it) }
                    dismissAccentPopup()
                    true
                }
                else -> true
            }
        }
    }

    /** Índice inicial: columna central de la fila inferior (la más cercana a la tecla). */
    private fun popupStartIndex(): Int {
        val n = activePopupChars.size
        val inFirstRow = minOf(popupColumns, n)
        popupStartCol = (inFirstRow - 1) / 2
        return popupStartCol
    }

    private fun showAccentPopup(anchor: View, chars: List<Char>) {
        activePopupChars = chars
        val itemW = maxOf(anchor.width, px(52f))
        val itemH = px(58f)
        val columns = when {
            chars.size <= 3 -> chars.size
            chars.size <= 8 -> 3
            else -> 4
        }.coerceAtLeast(1)
        val rows = ceil(chars.size / columns.toFloat()).toInt()
        popupColumns = columns
        popupItemW = itemW
        popupItemH = itemH

        val grid = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = px(12f).toFloat()
                setStroke(px(1f), Color.parseColor("#BBBBBB"))
            }
        }
        val tvs = arrayOfNulls<TextView>(chars.size)
        // fila 0 = la de abajo (más cerca de la tecla): se dibuja invertido
        for (displayRow in 0 until rows) {
            val r = rows - 1 - displayRow
            val rowLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(itemW * columns, itemH)
            }
            for (c in 0 until columns) {
                val i = r * columns + c
                val tv = TextView(context).apply {
                    text = if (i < chars.size) chars[i].toString() else ""
                    textSize = 24f
                    gravity = Gravity.CENTER
                    setTextColor(Color.BLACK)
                    layoutParams = LinearLayout.LayoutParams(itemW, itemH)
                }
                rowLayout.addView(tv)
                if (i < chars.size) tvs[i] = tv
            }
            grid.addView(rowLayout)
        }
        activePopupItems = tvs.filterNotNull()

        val popupWidth = itemW * columns
        val popupHeight = itemH * rows
        val xOff = -(popupWidth - anchor.width) / 2

        val pw = PopupWindow(grid, popupWidth, popupHeight, false)
        pw.isTouchable = false // el gesto sigue siendo de la tecla original
        activePopup = pw
        pw.showAsDropDown(anchor, xOff, -(anchor.height + popupHeight))
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
        val c = if ((shiftOn || capsLock) && ch.isLetter()) ch.uppercaseChar() else ch
        listener?.onKeyChar(c)
        consumeOneShotShift()
    }

    // ---------- Enter: toque = salto; deslizar = mover/seleccionar por palabras ----------
    private fun wireEnterKey(view: View) {
        var lastX = 0f
        var accum = 0f
        var moved = false
        var combo = false
        val thresholdPx = px(40f)

        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                    v.isPressed = true
                    lastX = event.rawX
                    accum = 0f
                    moved = false
                    enterHeld = true
                    enterComboUsed = false
                    combo = backspaceHeld
                    if (combo) { backspaceComboUsed = true; enterComboUsed = true; cancelBackspaceRepeat?.invoke() }
                    haptic(v)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - lastX
                    lastX = event.rawX
                    if (!combo) {
                        accum += dx
                        while (abs(accum) >= thresholdPx) {
                            val dir = if (accum > 0) 1 else -1
                            listener?.onEnterSwipeWord(dir, extendSelection = moved)
                            accum -= dir * thresholdPx
                            moved = true
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    v.isPressed = false
                    enterHeld = false
                    if (!moved && !combo && !enterComboUsed) listener?.onEnter()
                    true
                }
                MotionEvent.ACTION_CANCEL -> { v.isPressed = false; enterHeld = false; true }
                else -> true
            }
        }
    }

    // ---------- Borrar: toque = 1; mantener = repetir; deslizar izquierda = borrar PALABRAS ----------
    private fun wireBackspaceKey(view: View) {
        var lastX = 0f
        var startX = 0f
        var wordAccum = 0f
        var dragging = false
        var repeating = false
        var repeatCount = 0
        val activatePx = px(22f)
        val wordStepPx = px(38f)

        val repeatRunnable = object : Runnable {
            override fun run() {
                listener?.onBackspace()
                repeatCount++
                uiHandler.postDelayed(this, if (repeatCount > 12) 32L else 50L)
            }
        }
        val startRepeat = Runnable {
            if (backspaceHeld && !dragging && !backspaceComboUsed) {
                repeating = true
                repeatCount = 0
                uiHandler.post(repeatRunnable)
            }
        }
        fun stopAll() {
            uiHandler.removeCallbacks(startRepeat)
            uiHandler.removeCallbacks(repeatRunnable)
        }
        cancelBackspaceRepeat = { stopAll() }

        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                    v.isPressed = true
                    haptic(v)
                    backspaceHeld = true
                    backspaceComboUsed = false
                    lastX = event.rawX
                    startX = event.rawX
                    wordAccum = 0f
                    dragging = false
                    repeating = false
                    uiHandler.postDelayed(startRepeat, 380)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - lastX
                    lastX = event.rawX
                    if (!backspaceComboUsed) {
                        if (!dragging && startX - event.rawX > activatePx) {
                            dragging = true
                            stopAll()
                            repeating = false
                            listener?.onDeleteWord(-1) // primera palabra al empezar a deslizar
                            wordAccum = 0f
                        } else if (dragging) {
                            wordAccum += -dx
                            while (wordAccum >= wordStepPx) {
                                listener?.onDeleteWord(-1)
                                haptic(v)
                                wordAccum -= wordStepPx
                            }
                            if (wordAccum < 0) wordAccum = 0f
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    v.isPressed = false
                    stopAll()
                    backspaceHeld = false
                    if (!repeating && !dragging && !backspaceComboUsed) listener?.onBackspace()
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    v.isPressed = false
                    stopAll()
                    backspaceHeld = false
                    true
                }
                else -> true
            }
        }
    }

    // ---------- Espacio: toque = espacio; deslizar = cursor; con combos = abarcar palabras ----------
    private fun wireSpaceKey(view: View) {
        var lastX = 0f
        var lastY = 0f
        var accX = 0f
        var accY = 0f
        var moved = false
        var mode = 0 // 0 normal, 1 borrar (Borrar+Enter+Espacio), 2 seleccionar (Shift+Espacio)
        val charStep = px(16f)
        val wordStep = px(40f)
        val lineStep = px(34f)

        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                    v.isPressed = true
                    lastX = event.rawX; lastY = event.rawY
                    accX = 0f; accY = 0f
                    moved = false
                    mode = when {
                        backspaceHeld && enterHeld -> 1
                        shiftHeld -> 2
                        else -> 0
                    }
                    if (mode == 1) { backspaceComboUsed = true; enterComboUsed = true; cancelBackspaceRepeat?.invoke() }
                    if (mode == 2) shiftComboUsed = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    accX += event.rawX - lastX
                    accY += event.rawY - lastY
                    lastX = event.rawX; lastY = event.rawY
                    if (mode == 0) {
                        while (abs(accX) >= charStep) {
                            val dir = if (accX > 0) 1 else -1
                            listener?.onCursorMove(dir)
                            accX -= dir * charStep
                            moved = true
                        }
                    } else {
                        var guard = 0
                        while (guard++ < 20) {
                            val okX = abs(accX) >= wordStep
                            val okY = abs(accY) >= lineStep
                            if (!okX && !okY) break
                            if (okX && (!okY || abs(accX) / wordStep >= abs(accY) / lineStep)) {
                                val right = accX > 0
                                listener?.onSpanSwipe(if (right) 1 else 0, mode == 1)
                                accX -= (if (right) 1 else -1) * wordStep
                                accY = 0f
                            } else {
                                val down = accY > 0
                                listener?.onSpanSwipe(if (down) 3 else 2, mode == 1)
                                accY -= (if (down) 1 else -1) * lineStep
                                accX = 0f
                            }
                            moved = true
                            haptic(v)
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    v.isPressed = false
                    if (!moved && mode == 0) { haptic(v); listener?.onSpace() }
                    true
                }
                MotionEvent.ACTION_CANCEL -> { v.isPressed = false; true }
                else -> true
            }
        }
    }

    private fun haptic(v: View) {
        v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }
}
