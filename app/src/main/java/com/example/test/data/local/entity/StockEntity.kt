package com.example.test.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.test.domain.model.Stock

@Entity(tableName = "stocks")
data class StockEntity(
    @PrimaryKey val stockId: String,
    val code: String,
    val name: String,
    val market: String,
    val currentPrice: Double,
    val changeRate: Double,
    val volume: Long,
    val updatedAt: Long
)

fun StockEntity.toDomain(): Stock = Stock(
    stockId = stockId,
    code = code,
    name = name,
    market = market,
    currentPrice = currentPrice,
    changeRate = changeRate,
    volume = volume,
    marketCap = 0L // marketCap은 API에서만 제공되므로 기본값
)

fun Stock.toEntity(): StockEntity = StockEntity(
    stockId = stockId,
    code = code,
    name = name,
    market = market,
    currentPrice = currentPrice,
    changeRate = changeRate,
    volume = volume,
    updatedAt = System.currentTimeMillis()
)