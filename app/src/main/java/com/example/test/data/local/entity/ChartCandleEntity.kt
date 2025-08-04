package com.example.test.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.test.domain.model.Candle

@Entity(tableName = "chart_candles")
data class ChartCandleEntity(
    @PrimaryKey val id: String,
    val stockId: String,
    val interval: String,
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long
)

fun ChartCandleEntity.toDomain(): Candle = Candle(
    timestamp = timestamp,
    open = open,
    high = high,
    low = low,
    close = close,
    volume = volume
)

fun Candle.toEntity(stockId: String, interval: String): ChartCandleEntity = ChartCandleEntity(
    id = "${stockId}_${interval}_${timestamp}",
    stockId = stockId,
    interval = interval,
    timestamp = timestamp,
    open = open,
    high = high,
    low = low,
    close = close,
    volume = volume
)