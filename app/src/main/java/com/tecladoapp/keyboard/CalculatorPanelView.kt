package com.tecladoapp.keyboard

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.math.BigDecimal
import java.math.MathContext

/** Calculadora básica; el resultado se puede insertar en el texto. */
class CalculatorPanelView(context: Context) : LinearLayout(context) {

    interface Listener {
        fun onInsert(text: String)
        fun onClose()
    }
    var listener: Listener? = null

    private val density = context.resources.displayMetrics.density
    private fun px(dp: Float) = (dp * density).toInt()

    private val expr = StringBuilder()
    private val display = TextView(context)
    private var lastResult: String? = null

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.parseColor("#161616"))
        setPadding(px(8f), px(6f), px(8f), px(6f))

        val top = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        display.apply {
            textSize = 30f
            setTextColor(Color.WHITE)
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            maxLines = 1
            setPadding(px(12f), 0, px(12f), 0)
            text = "0"
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
        }
        val close = TextView(context).apply {
            text = "✕"; textSize = 26f
            setTextColor(Color.parseColor("#CCCCCC"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(px(56f), ViewGroup.LayoutParams.MATCH_PARENT)
            setOnClickListener { listener?.onClose() }
        }
        top.addView(display)
        top.addView(close)
        top.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.1f)
        addView(top)

        val rows = listOf(
            listOf("C", "⌫", "%", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "−"),
            listOf("1", "2", "3", "+"),
            listOf("0", ".", "=", "Insertar")
        )
        rows.forEach { r ->
            val row = LinearLayout(context).apply {
                orientation = HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            }
            r.forEach { label -> row.addView(makeButton(label)) }
            addView(row)
        }
    }

    private fun makeButton(label: String): Button {
        val isOp = label in listOf("÷", "×", "−", "+", "=")
        val isFn = label in listOf("C", "⌫", "%")
        val isInsert = label == "Insertar"
        val color = when {
            isInsert -> "#2E7D32"
            isOp -> "#4C6FFF"
            isFn -> "#555555"
            else -> "#2E2E2E"
        }
        return Button(context).apply {
            text = label
            isAllCaps = false
            textSize = if (isInsert) 17f else 26f
            setTextColor(Color.WHITE)
            stateListAnimator = null
            minWidth = 0; minimumWidth = 0; minHeight = 0; minimumHeight = 0
            setPadding(0, 0, 0, 0)
            background = GradientDrawable().apply { setColor(Color.parseColor(color)); cornerRadius = px(12f).toFloat() }
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, if (isInsert) 1.4f else 1f)
                .apply { setMargins(px(3f), px(3f), px(3f), px(3f)) }
            setOnClickListener { press(label) }
        }
    }

    private fun press(label: String) {
        when (label) {
            "C" -> { expr.setLength(0); lastResult = null }
            "⌫" -> if (expr.isNotEmpty()) expr.deleteCharAt(expr.length - 1)
            "=" -> evaluate()
            "Insertar" -> {
                if (expr.isNotEmpty() && lastResult == null) evaluate()
                val out = lastResult ?: expr.toString()
                if (out.isNotEmpty() && out != "Error") listener?.onInsert(out)
            }
            "+", "−", "×", "÷", "%" -> {
                if (expr.isEmpty() && lastResult != null) expr.append(lastResult)
                if (expr.isNotEmpty()) {
                    if (expr.last() in "+−×÷%" ) expr.deleteCharAt(expr.length - 1)
                    expr.append(label)
                }
                lastResult = null
            }
            else -> {
                if (lastResult != null) { expr.setLength(0); lastResult = null }
                expr.append(label)
            }
        }
        display.text = if (expr.isEmpty()) "0" else expr.toString()
        display.textSize = if (display.text.length > 14) 20f else 30f
    }

    private fun evaluate() {
        val result = try { calc(expr.toString()) } catch (e: Exception) { null }
        if (result == null) { display.text = "Error"; expr.setLength(0); lastResult = null; return }
        val s = format(result)
        expr.setLength(0); expr.append(s)
        lastResult = s
    }

    private fun format(v: BigDecimal): String {
        val r = v.round(MathContext(12)).stripTrailingZeros()
        return r.toPlainString()
    }

    /** Evaluador con precedencia: ×÷% antes que +−. */
    private fun calc(input: String): BigDecimal {
        val nums = mutableListOf<BigDecimal>()
        val ops = mutableListOf<Char>()
        var cur = StringBuilder()
        var i = 0
        if (input.isEmpty()) return BigDecimal.ZERO
        for (ch in input) {
            if (ch in "+−×÷%") {
                if (cur.isEmpty()) { if (ch == '−') { cur.append('-'); continue } else throw IllegalArgumentException() }
                nums.add(BigDecimal(cur.toString())); cur = StringBuilder(); ops.add(ch)
            } else cur.append(ch)
        }
        if (cur.isEmpty() || cur.toString() == "-") { if (ops.isEmpty()) throw IllegalArgumentException() else ops.removeAt(ops.size - 1) } else nums.add(BigDecimal(cur.toString()))
        // pasada 1: × ÷ %
        val n2 = mutableListOf(nums[0]); val o2 = mutableListOf<Char>()
        for (k in ops.indices) {
            val op = ops[k]; val b = nums[k + 1]
            when (op) {
                '×' -> n2[n2.size - 1] = n2.last().multiply(b)
                '÷' -> { if (b.signum() == 0) throw ArithmeticException(); n2[n2.size - 1] = n2.last().divide(b, MathContext(15)) }
                '%' -> { if (b.signum() == 0) throw ArithmeticException(); n2[n2.size - 1] = n2.last().remainder(b) }
                else -> { o2.add(op); n2.add(b) }
            }
        }
        var res = n2[0]
        for (k in o2.indices) res = if (o2[k] == '+') res.add(n2[k + 1]) else res.subtract(n2[k + 1])
        return res
    }
}
