package com.example.cryptopatternscanner

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.cryptopatternscanner.data.CandleAggregator
import com.example.cryptopatternscanner.data.CoinDcxApi
import com.example.cryptopatternscanner.model.Candle
import com.example.cryptopatternscanner.pattern.CustomPatternDetector
import com.example.cryptopatternscanner.ui.CandleChartView
import java.util.concurrent.Executors
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private val executor = Executors.newFixedThreadPool(6)

    private lateinit var chart: CandleChartView
    private lateinit var title: TextView
    private lateinit var status: TextView
    private lateinit var patternText: TextView
    private lateinit var scannerList: LinearLayout

    private var pairs: List<String> = emptyList()
    private var currentIndex = 0
    private var timeframe = "30m"
    private var currentCandles = emptyList<Candle>()
    private var currentMatch: CustomPatternDetector.Match? = null

    private val dark = Color.rgb(9, 11, 16)
    private val card = Color.rgb(18, 22, 30)
    private val green = Color.rgb(0, 208, 132)
    private val orange = Color.rgb(255, 117, 24)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        loadPairs()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 12, 14, 12)
            setBackgroundColor(dark)
        }

        title = tv("CRYPTO PATTERN SCANNER", 20f, Color.WHITE, true)
        root.addView(title)

        status = tv("Connecting to CoinDCX Futures…", 12f, Color.LTGRAY)
        root.addView(status)

        val tabs = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        listOf("30m", "1H", "2H").forEach { tf ->
            val b = Button(this).apply {
                text = tf
                setTextColor(Color.WHITE)
                setOnClickListener {
                    timeframe = tf.lowercase()
                    loadCurrent()
                }
            }
            tabs.addView(b, LinearLayout.LayoutParams(0, 52, 1f))
        }
        root.addView(tabs)

        title.setOnTouchListener(SwipeListener())

        chart = CandleChartView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f
            )
        }
        chart.setOnTouchListener(SwipeListener())
        root.addView(chart)

        patternText = tv("Pattern: scanning…", 14f, Color.WHITE, true)
        patternText.setPadding(10, 10, 10, 10)
        root.addView(patternText)

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val prev = Button(this).apply {
            text = "← PREV"
            setOnClickListener { move(-1) }
        }
        val next = Button(this).apply {
            text = "NEXT →"
            setOnClickListener { move(1) }
        }
        val scan = Button(this).apply {
            text = "SCAN ALL"
            setOnClickListener { scanAll() }
        }
        controls.addView(prev, LinearLayout.LayoutParams(0, 52, 1f))
        controls.addView(scan, LinearLayout.LayoutParams(0, 52, 1f))
        controls.addView(next, LinearLayout.LayoutParams(0, 52, 1f))
        root.addView(controls)

        val listTitle = tv("MATCHED COINS", 15f, Color.WHITE, true)
        root.addView(listTitle)

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 190
            )
        }
        scannerList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scroll.addView(scannerList)
        root.addView(scroll)

        setContentView(root)
    }

    private fun tv(text: String, size: Float, color: Int, bold: Boolean = false): TextView =
        TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(4, 4, 4, 4)
        }

    private fun loadPairs() {
        status.text = "Loading ALL active USDT Futures…"
        executor.execute {
            try {
                val loaded = CoinDcxApi.activeUsdtFutures()
                runOnUiThread {
                    pairs = loaded.distinct().sorted()
                    status.text = "${pairs.size} active USDT Futures"
                    if (pairs.isNotEmpty()) loadCurrent()
                }
            } catch (e: Exception) {
                runOnUiThread { status.text = "API error: ${e.message}" }
            }
        }
    }

    private fun loadCurrent() {
        if (pairs.isEmpty()) return
        val pair = pairs[currentIndex.coerceIn(pairs.indices)]
        title.text = "${pair.removePrefix("B-")}  •  $timeframe"
        status.text = "Loading $pair…"
        executor.execute {
            try {
                val candles = fetchTimeframe(pair, timeframe)
                val match = CustomPatternDetector.detect(candles)
                runOnUiThread {
                    currentCandles = candles
                    currentMatch = match
                    chart.setData(candles, match?.index)
                    patternText.text = if (match != null)
                        "✓ CUSTOM FORMATION DETECTED  • score ${match.score}/10"
                    else
                        "— custom formation not present"
                    patternText.setTextColor(if (match != null) green else Color.LTGRAY)
                    status.text = "${currentIndex + 1}/${pairs.size}  •  live REST data"
                }
            } catch (e: Exception) {
                runOnUiThread { status.text = "Load failed: ${e.message}" }
            }
        }
    }

    private fun fetchTimeframe(pair: String, tf: String): List<Candle> {
        return when (tf) {
            "30m" -> CandleAggregator.to30m(
                CoinDcxApi.candles(pair, "1", 6 * 60)
            ).takeLast(120)
            "1h" -> CoinDcxApi.candles(pair, "60", 120 * 60).takeLast(120)
            "2h" -> CandleAggregator.to2h(
                CoinDcxApi.candles(pair, "60", 240 * 60)
            ).takeLast(120)
            else -> emptyList()
        }
    }

    private fun move(delta: Int) {
        if (pairs.isEmpty()) return
        currentIndex = (currentIndex + delta + pairs.size) % pairs.size
        loadCurrent()
    }

    private inner class SwipeListener : View.OnTouchListener {
        private var downX = 0f
        override fun onTouch(v: View?, event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.x
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    val dx = event.x - downX
                    if (abs(dx) > 90f) {
                        move(if (dx < 0) 1 else -1)
                        return true
                    }
                }
            }
            return true
        }
    }

    private fun scanAll() {
        if (pairs.isEmpty()) return
        status.text = "Scanning ${pairs.size} Futures…"
        scannerList.removeAllViews()

        executor.execute {
            val matches = java.util.Collections.synchronizedList(
                mutableListOf<Pair<String, Int>>()
            )
            pairs.forEachIndexed { idx, pair ->
                executor.execute {
                    try {
                        val candles = fetchTimeframe(pair, timeframe)
                        val m = CustomPatternDetector.detect(candles)
                        if (m != null) matches += pair to m.score
                    } catch (_: Exception) { }
                    if (idx == pairs.lastIndex) {
                        runOnUiThread {
                            matches.sortByDescending { it.second }
                            scannerList.removeAllViews()
                            matches.forEach { (p, score) ->
                                val b = Button(this).apply {
                                    text = "✓ ${p.removePrefix("B-")}   score $score"
                                    setTextColor(Color.WHITE)
                                    setBackgroundColor(card)
                                    setOnClickListener {
                                        currentIndex = pairs.indexOf(p).coerceAtLeast(0)
                                        loadCurrent()
                                    }
                                }
                                scannerList.addView(
                                    b,
                                    LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT, 48
                                    )
                                )
                            }
                            status.text = "Scan complete: ${matches.size} matches"
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}
