package com.example.test.domain.model

import java.time.LocalDateTime

/**
 * 주식 정보 도메인 모델
 */
data class Stock(
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
 * 실시간 주가 도메인 모델
 */
data class StockPrice(
    val stockId: String,
    val price: Double,
    val changeAmount: Double,
    val changeRate: Double,
    val volume: Long,
    val timestamp: Long
)

/**
 * 매매 내역 도메인 모델
 */
data class StockTrade(
    val tradeId: String,
    val accountId: String,
    val stockId: String,
    val tradeType: TradeType,
    val quantity: Int,
    val price: Double,
    val totalAmount: Double,
    val tradedAt: LocalDateTime
)

/**
 * 포트폴리오 보유 종목 도메인 모델
 */
data class StockHolding(
    val holdingId: String,
    val accountId: String,
    val stockId: String,
    val quantity: Int,
    val avgBuyPrice: Double,
    val currentPrice: Double,
    val profitLoss: Double,
    val profitRate: Double
)

/**
 * 매매 타입
 */
enum class TradeType {
    BUY, SELL
}