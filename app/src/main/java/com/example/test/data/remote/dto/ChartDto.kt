package com.example.test.data.remote.dto

import com.example.test.domain.model.ChartData
import com.example.test.domain.model.ChartInterval
import com.example.test.domain.model.Candle
import com.example.test.domain.model.PatternAnalysis
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Chart Data API Response DTO
 */
data class ChartDataDto(
    val stockId: String,
    val interval: String,
    val candles: List<CandleDto>
)

/**
 * Candle Data DTO
 */
data class CandleDto(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long
)

/**
 * Pattern Analysis API Response DTO
 */
data class PatternAnalysisDto(
    val stockId: String,
    val patternType: String,
    val confidence: Double,
    val description: String,
    val analyzedAt: String
)

// Extension functions for mapping
fun ChartDataDto.toDomain(): ChartData = ChartData(
    stockId = stockId,
    interval = ChartInterval.valueOf(interval.uppercase()),
    candles = candles.map { it.toDomain() }
)

fun CandleDto.toDomain(): Candle = Candle(
    timestamp = timestamp,
    open = open,
    high = high,
    low = low,
    close = close,
    volume = volume
)

fun PatternAnalysisDto.toDomain(): PatternAnalysis = PatternAnalysis(
    stockId = stockId,
    patternType = patternType,
    confidence = confidence,
    description = description,
    analyzedAt = LocalDateTime.parse(analyzedAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
)