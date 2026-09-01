package com.tecladoapp.keyboard

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

/**
 * Dibuja iconos propios (no emojis del sistema) para las teclas de función,
 * en un estilo audaz y consistente con el icono de la app.
 */
object KeyIcons {

    enum class Icon { SHIFT, SHIFT_ACTIVE, BACKSPACE, ENTER, EMOJI, CLIPBOARD }

    fun draw(canvas: Canvas, icon: Icon, rect: RectF, paint: Paint) {
        paint.style = Paint.Style.FILL
        val w = rect.width()
        val h = rect.height()
        val cx = rect.centerX()
        val cy = rect.centerY()
        val s = minOf(w, h) * 0.42f // "radio" de referencia del glifo

        when (icon) {
            Icon.SHIFT, Icon.SHIFT_ACTIVE -> {
                val path = Path()
                // flecha hacia arriba: triángulo + tallo, estilo bold
                path.moveTo(cx, cy - s)
                path.lineTo(cx + s * 0.75f, cy)
                path.lineTo(cx + s * 0.32f, cy)
                path.lineTo(cx + s * 0.32f, cy + s * 0.7f)
                path.lineTo(cx - s * 0.32f, cy + s * 0.7f)
                path.lineTo(cx - s * 0.32f, cy)
                path.lineTo(cx - s * 0.75f, cy)
                path.close()
                paint.style = Paint.Style.FILL
                canvas.drawPath(path, paint)
            }

            Icon.BACKSPACE -> {
                val path = Path()
                val left = cx - s * 0.95f
                val right = cx + s * 0.85f
                val top = cy - s * 0.62f
                val bottom = cy + s * 0.62f
                path.moveTo(left + s * 0.4f, top)
                path.lineTo(right - s * 0.15f, top)
                path.quadTo(right, top, right, top + s * 0.15f)
                path.lineTo(right, bottom - s * 0.15f)
                path.quadTo(right, bottom, right - s * 0.15f, bottom)
                path.lineTo(left + s * 0.4f, bottom)
                path.lineTo(left, cy)
                path.close()
                canvas.drawPath(path, paint)

                // "x" dentro, recortada con modo CLEAR sobre una capa
                val strokePaint = Paint(paint).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = s * 0.16f
                    strokeCap = Paint.Cap.ROUND
                    xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
                }
                val layerRect = RectF(left, top, right, bottom)
                canvas.saveLayer(layerRect, paint)
                canvas.drawPath(path, paint)
                val xSize = s * 0.32f
                canvas.drawLine(cx - xSize * 0.1f - xSize, cy - xSize, cx - xSize * 0.1f + xSize, cy + xSize, strokePaint)
                canvas.drawLine(cx - xSize * 0.1f - xSize, cy + xSize, cx - xSize * 0.1f + xSize, cy - xSize, strokePaint)
                canvas.restore()
            }

            Icon.ENTER -> {
                val strokePaint = Paint(paint).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = s * 0.22f
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }
                val path = Path()
                path.moveTo(cx + s * 0.7f, cy - s * 0.7f)
                path.lineTo(cx + s * 0.7f, cy + s * 0.15f)
                path.lineTo(cx - s * 0.55f, cy + s * 0.15f)
                canvas.drawPath(path, strokePaint)
                // punta de flecha
                val arrow = Path()
                arrow.moveTo(cx - s * 0.15f, cy - s * 0.35f)
                arrow.lineTo(cx - s * 0.62f, cy + s * 0.15f)
                arrow.lineTo(cx - s * 0.15f, cy + s * 0.65f)
                arrow.close()
                canvas.drawPath(arrow, paint)
            }

            Icon.EMOJI -> {
                val strokePaint = Paint(paint).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = s * 0.16f
                    strokeCap = Paint.Cap.ROUND
                }
                canvas.drawCircle(cx, cy, s * 0.85f, strokePaint)
                canvas.drawCircle(cx - s * 0.32f, cy - s * 0.18f, s * 0.11f, paint)
                canvas.drawCircle(cx + s * 0.32f, cy - s * 0.18f, s * 0.11f, paint)
                val smile = Path()
                val smileRect = RectF(cx - s * 0.45f, cy - s * 0.35f, cx + s * 0.45f, cy + s * 0.55f)
                smile.arcTo(smileRect, 20f, 140f, false)
                canvas.drawPath(smile, strokePaint)
            }

            Icon.CLIPBOARD -> {
                val strokePaint = Paint(paint).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = s * 0.16f
                    strokeJoin = Paint.Join.ROUND
                }
                val body = RectF(cx - s * 0.62f, cy - s * 0.78f, cx + s * 0.62f, cy + s * 0.85f)
                canvas.drawRoundRect(body, s * 0.14f, s * 0.14f, strokePaint)
                val clip = RectF(cx - s * 0.28f, cy - s * 0.95f, cx + s * 0.28f, cy - s * 0.62f)
                canvas.drawRoundRect(clip, s * 0.08f, s * 0.08f, paint)
                val lineY1 = cy - s * 0.15f
                val lineY2 = cy + s * 0.25f
                canvas.drawLine(cx - s * 0.35f, lineY1, cx + s * 0.35f, lineY1, strokePaint)
                canvas.drawLine(cx - s * 0.35f, lineY2, cx + s * 0.1f, lineY2, strokePaint)
            }
        }
    }
}
