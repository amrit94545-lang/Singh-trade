package com.example.cryptopatternscanner.data

import com.example.cryptopatternscanner.model.Candle
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object CoinDcxApi {
    private const val ACTIVE =
        "https://api.coindcx.com/exchange/v1/derivatives/futures/data/active_instruments?margin_currency_short_name[]=USDT"
    private const val CANDLES =
        "https://public.coindcx.com/market_data/candlesticks"

    fun activeUsdtFutures(): List<String> {
        val text = get(ACTIVE)
        val arr = JSONArray(text)
        return buildList {
            for (i in 0 until arr.length()) add(arr.getString(i))
        }
    }

    /**
     * CoinDCX Futures REST docs currently expose 1m, 5m, 1h and 1d
     * through the candlestick endpoint. We use 1m to build 30m candles,
     * and 1h to build 1h/2h candles.
     */
    fun candles(pair: String, resolution: String, minutesBack: Int): List<Candle> {
        val now = System.currentTimeMillis() / 1000L
        val from = now - minutesBack * 60L
        val q = "?pair=${URLEncoder.encode(pair, "UTF-8")}" +
                "&from=$from&to=$now&resolution=$resolution&pcode=f"
        val text = get(CANDLES + q)
        val root = JSONObject(text)
        val data = root.optJSONArray("data") ?: JSONArray()
        val out = ArrayList<Candle>(data.length())
        for (i in 0 until data.length()) {
            val o = data.getJSONObject(i)
            out += Candle(
                time = o.optLong("time"),
                open = o.optDouble("open"),
                high = o.optDouble("high"),
                low = o.optDouble("low"),
                close = o.optDouble("close"),
                volume = o.optDouble("volume")
            )
        }
        return out.sortedBy { it.time }
    }

    private fun get(urlString: String): String {
        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12_000
            readTimeout = 15_000
            useCaches = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "CryptoPatternScanner/1.0")
        }
        return try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream.bufferedReader().use { it.readText() }
            if (code !in 200..299) error("HTTP $code: $body")
            body
        } finally {
            conn.disconnect()
        }
    }
}
