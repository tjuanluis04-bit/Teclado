package com.tecladoapp.keyboard

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.SystemClock

/**
 * Cronómetro y temporizador para practicar escritura. Vive fuera del panel: sigue corriendo
 * (y vibra al terminar) aunque cierres el panel y escribas en otra app.
 */
object PracticeTimer {

    private val ui = Handler(Looper.getMainLooper())
    private var appContext: Context? = null

    // cronómetro
    var swRunning = false; private set
    private var swStart = 0L
    private var swAccum = 0L
    val laps = mutableListOf<Long>()
    private var swNextBuzz = 60_000L

    // temporizador
    var tmRunning = false; private set
    private var tmEnd = 0L
    private var tmRemainingPaused = 0L
    var tmTotal = 5 * 60_000L

    var onTick: (() -> Unit)? = null
    var onStateChanged: (() -> Unit)? = null

    val anyRunning get() = swRunning || tmRunning

    private val ticker = object : Runnable {
        override fun run() {
            if (swRunning) {
                val e = swElapsed()
                if (e >= swNextBuzz) { buzz(longArrayOf(0, 120)); swNextBuzz += 60_000L } // una vibración corta por minuto
            }
            if (tmRunning && tmRemaining() <= 0L) {
                tmRunning = false; tmRemainingPaused = 0L
                buzz(longArrayOf(0, 500, 200, 500, 200, 800))
                onStateChanged?.invoke()
            }
            onTick?.invoke()
            if (swRunning || tmRunning) ui.postDelayed(this, 50)
        }
    }

    fun init(context: Context) { appContext = context.applicationContext }

    @Suppress("DEPRECATION")
    fun buzz(pattern: LongArray) {
        val v = appContext?.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        try { v.vibrate(VibrationEffect.createWaveform(pattern, -1)) } catch (_: Exception) {}
    }

    private fun kick() { ui.removeCallbacks(ticker); ui.post(ticker) }

    // ---- cronómetro ----
    fun swElapsed(): Long = swAccum + if (swRunning) SystemClock.elapsedRealtime() - swStart else 0L
    fun swToggle() {
        if (swRunning) { swAccum = swElapsed(); swRunning = false; buzz(longArrayOf(0, 60, 60, 60)) }
        else { swStart = SystemClock.elapsedRealtime(); swRunning = true; swNextBuzz = ((swAccum / 60_000L) + 1) * 60_000L; buzz(longArrayOf(0, 80)) }
        kick(); onStateChanged?.invoke()
    }
    fun swLap() { if (swRunning) { laps.add(0, swElapsed()); buzz(longArrayOf(0, 40)); onStateChanged?.invoke() } }
    fun swReset() { swRunning = false; swAccum = 0; laps.clear(); swNextBuzz = 60_000L; onStateChanged?.invoke(); onTick?.invoke() }

    // ---- temporizador ----
    fun tmRemaining(): Long =
        if (tmRunning) (tmEnd - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        else if (tmRemainingPaused > 0) tmRemainingPaused else tmTotal
    fun tmToggle() {
        if (tmRunning) { tmRemainingPaused = tmRemaining(); tmRunning = false }
        else {
            val rem = if (tmRemainingPaused > 0) tmRemainingPaused else tmTotal
            tmEnd = SystemClock.elapsedRealtime() + rem; tmRunning = true; buzz(longArrayOf(0, 80))
        }
        kick(); onStateChanged?.invoke()
    }
    fun tmReset() { tmRunning = false; tmRemainingPaused = 0L; onStateChanged?.invoke(); onTick?.invoke() }
    fun tmSetTotal(ms: Long) { tmTotal = ms.coerceIn(5_000L, 180 * 60_000L); tmRunning = false; tmRemainingPaused = 0L; onStateChanged?.invoke(); onTick?.invoke() }

    fun format(ms: Long, centis: Boolean): String {
        val totalSec = ms / 1000
        val m = totalSec / 60
        val s = totalSec % 60
        val base = String.format("%02d:%02d", m, s)
        return if (centis) base + String.format(".%02d", (ms % 1000) / 10) else base
    }
}
