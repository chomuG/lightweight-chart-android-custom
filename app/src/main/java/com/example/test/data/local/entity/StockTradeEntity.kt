package com.example.test.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.test.domain.model.StockTrade
import com.example.test.domain.model.TradeType
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Entity(tableName = "stock_trades")
data class StockTradeEntity(
    @PrimaryKey val tradeId: String,
    val accountId: String,
    val stockId: String,
    val tradeType: String,
    val quantity: Int,
    val price: Double,
    val totalAmount: Double,
    val tradedAt: Long
)

fun StockTradeEntity.toDomain(): StockTrade = StockTrade(
    tradeId = tradeId,
    accountId = accountId,
    stockId = stockId,
    tradeType = TradeType.valueOf(tradeType),
    quantity = quantity,
    price = price,
    totalAmount = totalAmount,
    tradedAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(tradedAt), ZoneId.systemDefault())
)

fun StockTrade.toEntity(): StockTradeEntity = StockTradeEntity(
    tradeId = tradeId,
    accountId = accountId,
    stockId = stockId,
    tradeType = tradeType.name,
    quantity = quantity,
    price = price,
    totalAmount = totalAmount,
    tradedAt = tradedAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
)