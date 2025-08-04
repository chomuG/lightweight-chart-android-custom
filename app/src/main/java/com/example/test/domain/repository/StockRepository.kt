package com.example.test.domain.repository

import com.example.test.domain.model.Stock
import com.example.test.domain.model.StockTrade
import com.example.test.domain.model.TradeResult
import kotlinx.coroutines.flow.Flow

/**
 * Stock Repository Interface
 */
interface StockRepository {
    suspend fun getStocks(): Flow<Result<List<Stock>>>
    suspend fun searchStocks(query: String): Flow<Result<List<Stock>>>
    suspend fun buyStock(stockId: String, quantity: Int, price: Double): Result<TradeResult>
    suspend fun sellStock(stockId: String, quantity: Int, price: Double): Result<TradeResult>
    suspend fun getMyTransactions(stockId: String): Flow<Result<List<StockTrade>>>
    suspend fun getInterestStocks(): Flow<Result<List<Stock>>>
    suspend fun addInterestStock(stockId: String): Result<Unit>
    suspend fun removeInterestStock(stockId: String): Result<Unit>
}