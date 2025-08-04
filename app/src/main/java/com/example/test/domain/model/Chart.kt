package com.example.test.domain.model

import java.time.LocalDateTime

/**
 * 차트 데이터 도메인 모델
 */
data class ChartData(
    val stockId: String,
    val interval: ChartInterval,
    val candles: List<Candle>
)

/**
 * 캔들 데이터 도메인 모델
 */
data class Candle(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long
)

/**
 * 차트 패턴 분석 도메인 모델
 */
data class PatternAnalysis(
    val stockId: String,
    val patternType: String,
    val confidence: Double,
    val description: String,
    val analyzedAt: LocalDateTime
)

/**
 * 차트 간격 타입
 */
enum class ChartInterval {
    MINUTE, HOUR, DAY, WEEK, MONTH
}