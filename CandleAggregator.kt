package com.example.cryptopatternscanner.data

import com.example.cryptopatternscanner.model.Candle

object CandleAggregator {

    fun to30m(oneMinute: List<Candle>): List<Candle> =
        aggregate(oneMinute, 30 * 60_000L)

    fun to2h(oneHour: List<Candle>): List<Candle> =
        aggregate(oneHour, 2 * 60 * 60_000L)

    private fun aggregate(src: List<Candle>, bucketMs: Long): List<Candle> {
        if (src.isEmpty()) return emptyList()
        val sorted = src.sortedBy { it.time }
        val result = ArrayList<Candle>()
        var bucketStart = -1L
        var first: Candle? = null
        var last: Candle? = null
        var high = Double.NEGATIVE_INFINITY
        var low = Double.POSITIVE_INFINITY
        var volume = 0.0

        fun flush() {
            val f = first ?: return
            val l = last ?: return
            result += Candle(
                time = bucketStart,
                open = f.open,
                high = high,
                low = low,
                close = l.close,
                volume = volume
            )
        }

        for (c in sorted) {
            val b = (c.time / bucketMs) * bucketMs
            if (bucketStart != -1L && b != bucketStart) {
                flush()
                first = null
                high = Double.NEGATIVE_INFINITY
                low = Double.POSITIVE_INFINITY
                volume = 0.0
            }
            if (first == null) {
                bucketStart = b
                first = c
            }
            last = c
            high = maxOf(high, c.high)
            low = minOf(low, c.low)
            volume += c.volume
        }
        flush()
        return result
    }
}
