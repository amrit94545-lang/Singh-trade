package com.example.cryptopatternscanner.ui

import android.content.Context
import android.graphics.*
import android.view.View
import com.example.cryptopatternscanner.model.Candle
import kotlin.math.max
import kotlin.math.min

class CandleChartView(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var candles: List<Candle> = emptyList()
    private var matchIndex: Int? = null

    fun setData(data: List<Candle>, matchIndex: Int?) {
        candles = data.takeLast(80)
        this.matchIndex = matchIndex
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.rgb(9, 11, 16))
        if (candles.isEmpty()) return

        val left = 18f
        val right = width - 12f
        val top = 18f
        val bottom = height - 26f
        val n = candles.size
        val minP = candles.minOf { it.low }
        val maxP = candles.maxOf { it.high }
        val range = max(maxP - minP, maxP * 0.000001)
        val step = (right - left) / n
        val bodyW = max(2f, step * 0.55f)

        fun y(price: Double): Float =
            (bottom - ((price - minP) / range * (bottom - top))).toFloat()

        // Grid.
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = Color.rgb(32, 36, 45)
        for (g in 1..4) {
            val yy = top + (bottom - top) * g / 5f
            canvas.drawLine(left, yy, right, yy, paint)
        }

        candles.forEachIndexed { idx, c ->
            val x = left + step * idx + step / 2f
            val yo = y(c.open)
            val yc = y(c.close)
            val yh = y(c.high)
            val yl = y(c.low)
            val bullish = c.close >= c.open
            paint.color = if (bullish) Color.rgb(0, 208, 132) else Color.rgb(245, 74, 91)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = max(1f, step * 0.12f)
            canvas.drawLine(x, yh, x, yl, paint)
            paint.style = Paint.Style.FILL
            val l = x - bodyW / 2f
            val r = x + bodyW / 2f
            val t = min(yo, yc)
            val b = max(yo, yc)
            canvas.drawRect(l, t, r, max(t + 2f, b), paint)
        }

        matchIndex?.let { globalIndex ->
            val visibleStart = (candles.size - 80).coerceAtLeast(0)
            val local = globalIndex - visibleStart
            if (local in candles.indices) {
                val x = left + step * local + step / 2f
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 4f
                paint.color = Color.rgb(255, 117, 24)
                canvas.drawCircle(x, y(candles[local].high) - 14f, 20f, paint)
            }
        }
    }
}
