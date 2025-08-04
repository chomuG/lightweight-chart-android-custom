package com.example.test.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.test.components.*
import kotlin.random.Random
import java.text.SimpleDateFormat
import java.util.*

class MultiPanelViewModel : ViewModel() {
    
    private val _multiPanelData = MutableStateFlow(generateSampleMultiPanelData())
    val multiPanelData: StateFlow<MultiPanelData> = _multiPanelData.asStateFlow()
    
    private val _selectedIndicators = MutableStateFlow(setOf(IndicatorType.RSI, IndicatorType.MACD))
    val selectedIndicators: StateFlow<Set<IndicatorType>> = _selectedIndicators.asStateFlow()
    
    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()
    
    fun toggleIndicator(indicator: IndicatorType) {
        val current = _selectedIndicators.value.toMutableSet()
        if (current.contains(indicator)) {
            current.remove(indicator)
        } else {
            current.add(indicator)
        }
        _selectedIndicators.value = current
        updateMultiPanelData()
    }
    
    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }
    
    fun refreshData() {
        _multiPanelData.value = generateSampleMultiPanelData()
    }
    
    private fun updateMultiPanelData() {
        val currentData = _multiPanelData.value
        val selectedIndicators = _selectedIndicators.value
        
        val newIndicators = mutableListOf<IndicatorData>()
        
        // Extract closing prices for indicator calculations
        val closingPrices = currentData.priceData.map { it.close }
        
        if (selectedIndicators.contains(IndicatorType.RSI)) {
            val rsiData = calculateRSIWithDates(currentData.priceData, period = 14)
            newIndicators.add(
                IndicatorData(
                    type = IndicatorType.RSI,
                    name = "RSI (14)",
                    data = rsiData,
                    options = IndicatorOptions(
                        color = "#9C27B0",
                        lineWidth = 2,
                        height = 150
                    )
                )
            )
        }
        
        if (selectedIndicators.contains(IndicatorType.MACD)) {
            val macdData = calculateMACDWithDates(currentData.priceData)
            newIndicators.add(
                IndicatorData(
                    type = IndicatorType.MACD,
                    name = "MACD (12,26,9)",
                    data = macdData,
                    options = IndicatorOptions(
                        color = "#2196F3",
                        lineWidth = 2,
                        height = 150
                    )
                )
            )
        }
        
        if (selectedIndicators.contains(IndicatorType.VOLUME)) {
            val volumeData = generateVolumeData(currentData.priceData.size)
            newIndicators.add(
                IndicatorData(
                    type = IndicatorType.VOLUME,
                    name = "Volume",
                    data = volumeData,
                    options = IndicatorOptions(
                        color = "#FF9800",
                        lineWidth = 1,
                        height = 120
                    )
                )
            )
        }
        
        if (selectedIndicators.contains(IndicatorType.STOCHASTIC)) {
            val stochasticData = calculateStochastic(currentData.priceData)
            newIndicators.add(
                IndicatorData(
                    type = IndicatorType.STOCHASTIC,
                    name = "Stochastic %K",
                    data = stochasticData,
                    options = IndicatorOptions(
                        color = "#4CAF50",
                        lineWidth = 2,
                        height = 150
                    )
                )
            )
        }
        
        _multiPanelData.value = currentData.copy(indicators = newIndicators)
    }
    
    private fun generateSampleMultiPanelData(): MultiPanelData {
        val priceData = generateSampleCandlestickData(200)
        
        // Extract closing prices for indicator calculations
        val closingPrices = priceData.map { it.close }
        
        val indicators = mutableListOf<IndicatorData>()
        
        // Generate RSI
        val rsiData = calculateRSIWithDates(priceData, period = 14)
        indicators.add(
            IndicatorData(
                type = IndicatorType.RSI,
                name = "RSI (14)",
                data = rsiData,
                options = IndicatorOptions(
                    color = "#9C27B0",
                    lineWidth = 2,
                    height = 150
                )
            )
        )
        
        // Generate MACD
        val macdData = calculateMACDWithDates(priceData)
        indicators.add(
            IndicatorData(
                type = IndicatorType.MACD,
                name = "MACD (12,26,9)",
                data = macdData,
                options = IndicatorOptions(
                    color = "#2196F3",
                    lineWidth = 2,
                    height = 150
                )
            )
        )
        
        return MultiPanelData(
            priceData = priceData,
            indicators = indicators
        )
    }
    
    private fun generateSampleCandlestickData(count: Int): List<CandlestickData> {
        val data = mutableListOf<CandlestickData>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -count) // count days ago
        var currentPrice = 100.0
        
        repeat(count) { i ->
            val timeString = dateFormat.format(calendar.time)
            
            // Generate realistic OHLC data
            val change = (Random.nextDouble() - 0.5) * 4.0 // ±2% change
            val open = currentPrice
            val close = currentPrice + change
            val high = maxOf(open, close) + Random.nextDouble() * 2.0
            val low = minOf(open, close) - Random.nextDouble() * 2.0
            
            data.add(
                CandlestickData(
                    time = timeString,
                    open = open,
                    high = high,
                    low = low,
                    close = close
                )
            )
            
            currentPrice = close
            calendar.add(Calendar.DAY_OF_YEAR, 1) // next day
        }
        
        return data
    }
    
    private fun generateVolumeData(count: Int): List<ChartData> {
        val data = mutableListOf<ChartData>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -count) // count days ago
        
        repeat(count) { i ->
            val timeString = dateFormat.format(calendar.time)
            val volume = Random.nextDouble(100000.0, 1000000.0)
            
            data.add(
                ChartData(
                    time = timeString,
                    value = volume
                )
            )
            
            calendar.add(Calendar.DAY_OF_YEAR, 1) // next day
        }
        
        return data
    }
    
    private fun calculateStochastic(
        candleData: List<CandlestickData>,
        kPeriod: Int = 14,
        dPeriod: Int = 3
    ): List<ChartData> {
        if (candleData.size < kPeriod) return emptyList()
        
        val stochasticData = mutableListOf<ChartData>()
        
        for (i in kPeriod - 1 until candleData.size) {
            val period = candleData.subList(i - kPeriod + 1, i + 1)
            val highest = period.maxOf { it.high }
            val lowest = period.minOf { it.low }
            val currentClose = candleData[i].close
            
            val kPercent = if (highest != lowest) {
                ((currentClose - lowest) / (highest - lowest)) * 100
            } else {
                50.0 // middle value when no range
            }
            
            stochasticData.add(
                ChartData(
                    time = candleData[i].time,
                    value = kPercent
                )
            )
        }
        
        return stochasticData
    }
    
    private fun calculateRSIWithDates(candleData: List<CandlestickData>, period: Int = 14): List<ChartData> {
        if (candleData.size < period + 1) return emptyList()
        
        val prices = candleData.map { it.close }
        val gains = mutableListOf<Double>()
        val losses = mutableListOf<Double>()
        
        // Calculate price changes
        for (i in 1 until prices.size) {
            val change = prices[i] - prices[i - 1]
            gains.add(if (change > 0) change else 0.0)
            losses.add(if (change < 0) -change else 0.0)
        }
        
        val rsiValues = mutableListOf<ChartData>()
        
        // Calculate initial averages
        var avgGain = gains.take(period).average()
        var avgLoss = losses.take(period).average()
        
        for (i in period until gains.size) {
            if (avgLoss != 0.0) {
                val rs = avgGain / avgLoss
                val rsi = 100 - (100 / (1 + rs))
                // Use the actual candle time (i+1 because gains array is offset by 1)
                rsiValues.add(ChartData(time = candleData[i + 1].time, value = rsi))
            }
            
            // Update averages using Wilder's smoothing
            avgGain = (avgGain * (period - 1) + gains[i]) / period
            avgLoss = (avgLoss * (period - 1) + losses[i]) / period
        }
        
        return rsiValues
    }
    
    private fun calculateMACDWithDates(
        candleData: List<CandlestickData>,
        fastPeriod: Int = 12,
        slowPeriod: Int = 26,
        signalPeriod: Int = 9
    ): List<ChartData> {
        if (candleData.size < slowPeriod) return emptyList()
        
        val prices = candleData.map { it.close }
        val fastEMA = calculateEMA(prices, fastPeriod)
        val slowEMA = calculateEMA(prices, slowPeriod)
        
        val macdLine = mutableListOf<ChartData>()
        
        // Calculate MACD line with proper dates
        for (i in slowPeriod - 1 until prices.size) {
            val macdValue = fastEMA[i - (slowPeriod - fastPeriod)] - slowEMA[i - (slowPeriod - 1)]
            macdLine.add(ChartData(time = candleData[i].time, value = macdValue))
        }
        
        return macdLine
    }
    
    private fun calculateEMA(prices: List<Double>, period: Int): List<Double> {
        if (prices.size < period) return emptyList()
        
        val alpha = 2.0 / (period + 1)
        val ema = mutableListOf<Double>()
        
        // Start with SMA for the first value
        ema.add(prices.take(period).average())
        
        // Calculate EMA for remaining values
        for (i in period until prices.size) {
            val emaValue = alpha * prices[i] + (1 - alpha) * ema.last()
            ema.add(emaValue)
        }
        
        return ema
    }
}