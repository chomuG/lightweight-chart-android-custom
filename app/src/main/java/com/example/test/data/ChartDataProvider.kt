package com.example.test.data

import com.example.test.components.CandlestickData
import com.example.test.components.ChartData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

/**
 * Data provider for chart examples and real-time updates
 */
object ChartDataProvider {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    /**
     * Generate sample line chart data
     */
    fun generateSampleLineData(count: Int = 50): List<ChartData> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -count)
        
        val data = mutableListOf<ChartData>()
        var lastValue = 100.0
        
        repeat(count) {
            val variation = Random.nextDouble(-5.0, 5.0)
            lastValue += variation
            
            data.add(
                ChartData(
                    time = dateFormat.format(calendar.time),
                    value = lastValue.coerceAtLeast(0.1)
                )
            )
            
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        return data
    }
    
    /**
     * Generate sample candlestick data
     */
    fun generateSampleCandlestickData(count: Int = 50): List<CandlestickData> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -count)
        
        val data = mutableListOf<CandlestickData>()
        var lastClose = 100.0
        
        repeat(count) {
            val open = lastClose + Random.nextDouble(-2.0, 2.0)
            val close = open + Random.nextDouble(-5.0, 5.0)
            val high = maxOf(open, close) + Random.nextDouble(0.0, 3.0)
            val low = minOf(open, close) - Random.nextDouble(0.0, 3.0)
            
            data.add(
                CandlestickData(
                    time = dateFormat.format(calendar.time),
                    open = open.coerceAtLeast(0.1),
                    high = high.coerceAtLeast(0.1),
                    low = low.coerceAtLeast(0.1),
                    close = close.coerceAtLeast(0.1)
                )
            )
            
            lastClose = close
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        return data
    }
    
    /**
     * Generate real-time data updates
     */
    fun generateRealTimeData(initialValue: Double = 100.0): Flow<ChartData> = flow {
        var currentValue = initialValue
        val calendar = Calendar.getInstance()
        
        while (true) {
            delay(1000) // Update every second
            
            val variation = Random.nextDouble(-2.0, 2.0)
            currentValue += variation
            currentValue = currentValue.coerceAtLeast(0.1)
            
            emit(
                ChartData(
                    time = dateFormat.format(calendar.time),
                    value = currentValue
                )
            )
            
            calendar.add(Calendar.SECOND, 1)
        }
    }
    
    /**
     * Sample cryptocurrency price data
     */
    fun getSampleCryptoPriceData(): List<ChartData> = listOf(
        ChartData("2024-01-01", 42000.0),
        ChartData("2024-01-02", 43500.0),
        ChartData("2024-01-03", 41800.0),
        ChartData("2024-01-04", 44200.0),
        ChartData("2024-01-05", 46800.0),
        ChartData("2024-01-06", 45300.0),
        ChartData("2024-01-07", 47900.0),
        ChartData("2024-01-08", 49200.0),
        ChartData("2024-01-09", 48100.0),
        ChartData("2024-01-10", 50500.0),
        ChartData("2024-01-11", 52300.0),
        ChartData("2024-01-12", 51700.0),
        ChartData("2024-01-13", 53800.0),
        ChartData("2024-01-14", 52900.0),
        ChartData("2024-01-15", 54600.0)
    )
    
    /**
     * Sample stock price data
     */
    fun getSampleStockData(): List<CandlestickData> = listOf(
        CandlestickData("2024-01-01", 150.0, 155.0, 148.0, 152.0),
        CandlestickData("2024-01-02", 152.0, 158.0, 151.0, 156.0),
        CandlestickData("2024-01-03", 156.0, 159.0, 154.0, 157.0),
        CandlestickData("2024-01-04", 157.0, 162.0, 155.0, 160.0),
        CandlestickData("2024-01-05", 160.0, 165.0, 158.0, 163.0),
        CandlestickData("2024-01-06", 163.0, 166.0, 161.0, 164.0),
        CandlestickData("2024-01-07", 164.0, 168.0, 162.0, 167.0),
        CandlestickData("2024-01-08", 167.0, 170.0, 165.0, 169.0),
        CandlestickData("2024-01-09", 169.0, 172.0, 167.0, 171.0),
        CandlestickData("2024-01-10", 171.0, 174.0, 169.0, 173.0),
        CandlestickData("2024-01-11", 173.0, 176.0, 171.0, 175.0),
        CandlestickData("2024-01-12", 175.0, 178.0, 173.0, 177.0),
        CandlestickData("2024-01-13", 177.0, 180.0, 175.0, 179.0),
        CandlestickData("2024-01-14", 179.0, 182.0, 177.0, 181.0),
        CandlestickData("2024-01-15", 181.0, 184.0, 179.0, 183.0)
    )
    
    /**
     * Sample volume data for histogram
     */
    fun getSampleVolumeData(): List<ChartData> = listOf(
        ChartData("2024-01-01", 1250000.0),
        ChartData("2024-01-02", 1350000.0),
        ChartData("2024-01-03", 980000.0),
        ChartData("2024-01-04", 1420000.0),
        ChartData("2024-01-05", 1680000.0),
        ChartData("2024-01-06", 1530000.0),
        ChartData("2024-01-07", 1790000.0),
        ChartData("2024-01-08", 1920000.0),
        ChartData("2024-01-09", 1810000.0),
        ChartData("2024-01-10", 2050000.0),
        ChartData("2024-01-11", 2230000.0),
        ChartData("2024-01-12", 2170000.0),
        ChartData("2024-01-13", 2380000.0),
        ChartData("2024-01-14", 2290000.0),
        ChartData("2024-01-15", 2460000.0)
    )
}