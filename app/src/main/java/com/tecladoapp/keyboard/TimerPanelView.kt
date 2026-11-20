package com.tecladoapp.keyboard

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Cronómetro y temporizador para practicar la escritura; el teléfono vibra al iniciar, cada minuto y al terminar. */
class TimerPanelView(context: Context) : LinearLayout(context) {

    interface Listener { fun onClose() }
    var listener: Listener? = null

    private var showTimer = false
    private val tabSw = TextView(context)
    private val tabTm = TextView(context)
    private val body = LinearLayout(context)
    private val display = TextView(context)
    private val lapsBox = LinearLayout(context)
    private var startBtn: TextView? = null

    init {
        PracticeTimer.init(context)
        orientation = VERTICAL
        setBackgroundColor(Color.parseColor("#161616"))
        addView(PanelUi.topBar(context, "Cronómetro y temporizador") { listener?.onClose() })

        val tabs = LinearLayout(context).apply { orientation = HORIZONTAL }
        listOf(tabSw to "Cronómetro", tabTm to "Temporizador").forEachIndexed { i, (tv, label) ->
            tv.apply {
                text = label; textSize = 17f; gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#EAEAEA")); setPadding(0, PanelUi.dp(context, 12f), 0, PanelUi.dp(context, 12f))
                layoutParams = LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener { showTimer = i == 1; render() }
            }
            tabs.addView(tv)
        }
        addView(tabs)
        body.orientation = VERTICAL
        body.layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        addView(body)
        render()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        PracticeTimer.onTick = { updateDisplay() }
        PracticeTimer.onStateChanged = { renderLaps(); updateStartLabel() }
        updateDisplay()
    }

    override fun onDetachedFromWindow() {
        PracticeTimer.onTick = null
        PracticeTimer.onStateChanged = null
        super.onDetachedFromWindow()
    }

    private fun updateDisplay() {
        display.text = if (showTimer) PracticeTimer.format(PracticeTimer.tmRemaining(), false)
        else PracticeTimer.format(PracticeTimer.swElapsed(), true)
    }

    private fun updateStartLabel() {
        val running = if (showTimer) PracticeTimer.tmRunning else PracticeTimer.swRunning
        startBtn?.text = if (running) "Pausar" else "Iniciar"
    }

    private fun render() {
        tabSw.setBackgroundColor(if (!showTimer) Color.parseColor("#33445577") else Color.TRANSPARENT)
        tabTm.setBackgroundColor(if (showTimer) Color.parseColor("#33445577") else Color.TRANSPARENT)
        body.removeAllViews()

        display.apply {
            textSize = 52f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.MONOSPACE
            layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, PanelUi.dp(context, 84f))
        }
        (display.parent as? ViewGroup)?.removeView(display)
        body.addView(display)

        if (showTimer) {
            val presets = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER }
            listOf(1, 2, 3, 5, 10, 15).forEach { m ->
                presets.addView(PanelUi.chip(context, "$m min", PracticeTimer.tmTotal == m * 60_000L) { PracticeTimer.tmSetTotal(m * 60_000L); render() })
            }
            body.addView(presets)
            val adj = LinearLayout(context).apply {
                orientation = HORIZONTAL
                layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, PanelUi.dp(context, 44f))
                setPadding(PanelUi.dp(context, 4f), 0, PanelUi.dp(context, 4f), 0)
            }
            adj.addView(PanelUi.button(context, "− 10 s", "#3A3A3A") { PracticeTimer.tmSetTotal(PracticeTimer.tmTotal - 10_000L); render() })
            adj.addView(PanelUi.button(context, "+ 10 s", "#3A3A3A") { PracticeTimer.tmSetTotal(PracticeTimer.tmTotal + 10_000L); render() })
            adj.addView(PanelUi.button(context, "− 1 min", "#3A3A3A") { PracticeTimer.tmSetTotal(PracticeTimer.tmTotal - 60_000L); render() })
            adj.addView(PanelUi.button(context, "+ 1 min", "#3A3A3A") { PracticeTimer.tmSetTotal(PracticeTimer.tmTotal + 60_000L); render() })
            body.addView(adj)
        }

        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, PanelUi.dp(context, 56f)).apply { topMargin = PanelUi.dp(context, 8f) }
            setPadding(PanelUi.dp(context, 6f), 0, PanelUi.dp(context, 6f), 0)
        }
        val start = PanelUi.button(context, "Iniciar", "#2E7D32") {
            if (showTimer) PracticeTimer.tmToggle() else PracticeTimer.swToggle()
            updateStartLabel()
        }
        startBtn = start
        row.addView(start)
        if (!showTimer) row.addView(PanelUi.button(context, "Vuelta", "#4C6FFF") { PracticeTimer.swLap() })
        row.addView(PanelUi.button(context, "Reiniciar", "#7A2E2E") { if (showTimer) PracticeTimer.tmReset() else PracticeTimer.swReset(); renderLaps(); updateStartLabel() })
        body.addView(row)

        val note = TextView(context).apply {
            text = if (showTimer) "Vibra al iniciar y al terminar. Sigue corriendo aunque cierres este panel."
            else "Vibra al iniciar, cada minuto y al pausar. Sigue corriendo aunque cierres este panel."
            textSize = 13f; setTextColor(Color.parseColor("#888888")); gravity = Gravity.CENTER
            setPadding(PanelUi.dp(context, 12f), PanelUi.dp(context, 8f), PanelUi.dp(context, 12f), PanelUi.dp(context, 4f))
        }
        body.addView(note)

        lapsBox.orientation = VERTICAL
        (lapsBox.parent as? ViewGroup)?.removeView(lapsBox)
        val sv = ScrollView(context).apply { layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f); addView(lapsBox) }
        body.addView(sv)
        renderLaps()
        updateStartLabel()
        updateDisplay()
    }

    private fun renderLaps() {
        lapsBox.removeAllViews()
        if (showTimer) return
        PracticeTimer.laps.forEachIndexed { i, ms ->
            lapsBox.addView(TextView(context).apply {
                text = "Vuelta ${PracticeTimer.laps.size - i}   ${PracticeTimer.format(ms, true)}"
                textSize = 16f; setTextColor(Color.parseColor("#CCCCCC"))
                setPadding(PanelUi.dp(context, 20f), PanelUi.dp(context, 6f), 0, PanelUi.dp(context, 6f))
            })
        }
    }
}
