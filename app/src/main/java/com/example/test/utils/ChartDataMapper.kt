package com.example.test.utils

import com.example.test.components.CandlestickData
import com.example.test.components.ChartData
import com.example.test.domain.model.Candle
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility functions to convert between domain models and UI chart components
 */

/**
 * Convert domain Candle to UI CandlestickData
 */
fun Candle.toCandlestickData(): CandlestickData {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val date = Date(timestamp)
    
    return CandlestickData(
        time = dateFormat.format(date),
        open = open,
        high = high,
        low = low,
        close = close
    )
}

/**
 * Convert domain Candle to UI ChartData (using close price)
 */
fun Candle.toChartData(): ChartData {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val date = Date(timestamp)
    
    return ChartData(
        time = dateFormat.format(date),
        value = close
    )
}

/**
 * Convert list of domain Candles to UI CandlestickData list
 */
fun List<Candle>.toCandlestickDataList(): List<CandlestickData> {
    return this.map { it.toCandlestickData() }
}

/**
 * Convert list of domain Candles to UI ChartData list
 */
fun List<Candle>.toChartDataList(): List<ChartData> {
    return this.map { it.toChartData() }
}