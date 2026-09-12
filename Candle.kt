package com.example.cryptopatternscanner.model

data class Candle(
    val time: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double = 0.0
) {
    val body: Double get() = kotlin.math.abs(close - open)
    val range: Double get() = kotlin.math.max(high - low, 1e-12)
    val bullish: Boolean get() = close >= open
    val bearish: Boolean get() = close < open
    val upperWick: Double get() = high - maxOf(open, close)
    val lowerWick: Double get() = minOf(open, close) - low
}
