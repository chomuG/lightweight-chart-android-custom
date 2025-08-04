package com.example.test.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.test.domain.model.InterestStock

@Entity(tableName = "interest_stocks")
data class InterestStockEntity(
    @PrimaryKey val interestId: String,
    val userId: String,
    val stockId: String,
    val addedAt: Long
)

fun InterestStockEntity.toDomain(): InterestStock = InterestStock(
    interestId = interestId,
    userId = userId,
    stockId = stockId,
    addedAt = addedAt
)

fun InterestStock.toEntity(): InterestStockEntity = InterestStockEntity(
    interestId = interestId,
    userId = userId,
    stockId = stockId,
    addedAt = addedAt
)