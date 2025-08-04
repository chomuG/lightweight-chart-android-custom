package com.example.test.domain.model

/**
 * 계좌 정보 도메인 모델
 */
data class Account(
    val accountId: String,
    val userId: String,
    val totalAssets: Double,
    val cash: Double,
    val createdAt: Long
)

/**
 * 관심 종목 도메인 모델
 */
data class InterestStock(
    val interestId: String,
    val userId: String,
    val stockId: String,
    val addedAt: Long
)

/**
 * 매매 결과 도메인 모델
 */
data class TradeResult(
    val tradeId: String,
    val accountId: String,
    val success: Boolean,
    val message: String,
    val remainingCash: Double,
    val timestamp: Long
)