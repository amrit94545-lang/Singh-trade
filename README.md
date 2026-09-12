# Crypto Pattern Scanner — CoinDCX Futures

Android app source for a mobile-friendly CoinDCX Futures candlestick scanner.

## Included
- ALL active USDT Futures instruments are loaded dynamically from CoinDCX.
- 30m, 1H and 2H chart views.
- 2H is built by aggregating 1H candles.
- 30m is built by aggregating 1m candles because the current CoinDCX Futures REST candle endpoint documents 1m/5m/1h/1d resolutions.
- Swipe left/right between coins.
- SCAN ALL scans the active Futures list and puts only the custom formation at the top.
- No order placement, API key, secret, buy/sell signal, TP/SL or trading automation.

## Custom formation
The detector is deliberately based on the formation marked in the user's screenshot:
large bearish candle -> smaller bearish candle -> bullish reaction with lower wick -> stronger bullish recovery -> bullish continuation -> small/indecision candle near the upper area.

The exact thresholds are in:
`app/src/main/kotlin/com/example/cryptopatternscanner/pattern/CustomPatternDetector.kt`

They are relative to recent candle body size so they can work across different coin prices.

## Build on Android phone
1. Install AndroidIDE on the Android phone.
2. Extract this ZIP.
3. Open the extracted folder as a Gradle Android project.
4. Let Gradle sync.
5. Build/Run the app.

No CoinDCX API key is required because this app uses public market-data endpoints only.

## Important
CoinDCX's REST candlestick endpoint and rate limits can change. The app handles errors per pair, but if CoinDCX rate-limits a large scan, reduce the executor pool in `MainActivity.kt` from 6 to 2–3 or implement a websocket scanner.

Official API documentation:
https://docs.coindcx.com/
