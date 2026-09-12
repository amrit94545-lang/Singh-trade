package com.example.cryptopatternscanner.pattern

import com.example.cryptopatternscanner.model.Candle
import kotlin.math.max

/**
 * CUSTOM FORMATION
 *
 * This detector is intentionally NOT a library of generic candle patterns.
 * It models the exact visual formation the user marked in the supplied
 * screenshot:
 *
 *   1) a comparatively large bearish candle
 *   2) another smaller bearish candle
 *   3) a bullish reaction candle with a meaningful lower wick
 *   4) a stronger bullish candle
 *   5) a bullish continuation candle
 *   6) a small/indecision candle near the upper area
 *
 * The thresholds are relative to the recent average candle body so the
 * scanner can work across coins with very different prices/volatility.
 */
object CustomPatternDetector {

    data class Match(
        val index: Int,
        val score: Int,
        val label: String = "CUSTOM FORMATION"
    )

    fun detect(candles: List<Candle>): Match? {
        if (candles.size < 14) return null

        val i = candles.lastIndex
        val w = candles.subList(i - 5, i + 1)
        val avgBody = candles.subList(max(0, i - 13), i + 1)
            .map { it.body }
            .average()
            .coerceAtLeast(1e-12)

        val c1 = w[0]
        val c2 = w[1]
        val c3 = w[2]
        val c4 = w[3]
        val c5 = w[4]
        val c6 = w[5]

        var score = 0

        // 1. Large bearish impulse.
        if (c1.bearish && c1.body >= avgBody * 1.25) score += 2
        else return null

        // 2. Smaller second bearish candle.
        if (c2.bearish && c2.body <= c1.body * 0.80) score += 1
        else return null

        // 3. First bullish reaction + lower wick.
        if (c3.bullish && c3.lowerWick >= c3.body * 0.35) score += 2
        else return null

        // 4. Strong bullish recovery.
        if (c4.bullish && c4.body >= avgBody * 0.90 && c4.close > c3.close) score += 2
        else return null

        // 5. Bullish continuation.
        if (c5.bullish && c5.close >= c4.close * 0.995) score += 1
        else return null

        // 6. Last candle small/indecision near the recovered high.
        val nearHigh = c6.close >= maxOf(c4.close, c5.close) * 0.985
        val small = c6.body <= avgBody * 0.90 || c6.range <= avgBody * 1.8
        if (nearHigh && small) score += 2
        else return null

        // Optional context: formation should reclaim a meaningful part of
        // the first bearish candle's body.
        val reclaim = c6.close > c1.open - c1.body * 0.20
        if (reclaim) score += 1

        return if (score >= 8) Match(i, score) else null
    }
}
