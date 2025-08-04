package com.example.test.data.remote.dto

import com.example.test.domain.model.Stock
import com.example.test.domain.model.StockTrade
import com.example.test.domain.model.TradeType
import com.example.test.domain.model.TradeResult
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Stock API Response DTO
 */
data class StockDto(
    val stockId: String,
    val code: String,
    val name: String,
    val market: String,
    val currentPrice: Double,
    val changeRate: Double,
    val volume: Long,
    val marketCap: Long
)

/**
 * Stock Trade API Response DTO
 */
data class StockTradeDto(
    val tradeId: String,
    val accountId: String,
    val stockId: String,
    val tradeType: String,
    val quantity: Int,
    val price: Double,
    val totalAmount: Double,
    val tradedAt: String
)

/**
 * Buy Stock Request DTO
 */
data class BuyStockRequest(
    val stockId: String,
    val quantity: Int,
    val price: Double
)

/**
 * Sell Stock Request DTO
 */
data class SellStockRequest(
    val stockId: String,
    val quantity: Int,
    val price: Double
)

/**
 * Add Interest Stock Request DTO
 */
data class AddInterestRequest(
    val stockId: String
)

/**
 * Trade Result DTO
 */
data class TradeResultDto(
    val tradeId: String,
    val accountId: String,
    val success: Boolean,
    val message: String,
    val remainingCash: Double,
    val timestamp: Long
)

// Extension functions for mapping
fun StockDto.toDomain(): Stock = Stock(
    stockId = stockId,
    code = code,
    name = name,
    market = market,
    currentPrice = currentPrice,
    changeRate = changeRate,
    volume = volume,
    marketCap = marketCap
)

fun StockTradeDto.toDomain(): StockTrade = StockTrade(
    tradeId = tradeId,
    accountId = accountId,
    stockId = stockId,
    tradeType = TradeType.valueOf(tradeType),
    quantity = quantity,
    price = price,
    totalAmount = totalAmount,
    tradedAt = LocalDateTime.parse(tradedAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
)

fun TradeResultDto.toDomain(): TradeResult = TradeResult(
    tradeId = tradeId,
    accountId = accountId,
    success = success,
    message = message,
    remainingCash = remainingCash,
    timestamp = timestamp
)