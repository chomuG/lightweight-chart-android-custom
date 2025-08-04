package com.example.test.data.local.dao

import androidx.room.*
import com.example.test.data.local.entity.StockTradeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockTradeDao {
    
    @Query("SELECT * FROM stock_trades WHERE accountId = :accountId ORDER BY tradedAt DESC")
    suspend fun getTradesByAccount(accountId: String): List<StockTradeEntity>
    
    @Query("SELECT * FROM stock_trades WHERE stockId = :stockId AND accountId = :accountId ORDER BY tradedAt DESC")
    suspend fun getTradesByStockAndAccount(stockId: String, accountId: String): List<StockTradeEntity>
    
    @Query("SELECT * FROM stock_trades WHERE tradeId = :tradeId")
    suspend fun getTradeById(tradeId: String): StockTradeEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrade(trade: StockTradeEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrades(trades: List<StockTradeEntity>)
    
    @Update
    suspend fun updateTrade(trade: StockTradeEntity)
    
    @Delete
    suspend fun deleteTrade(trade: StockTradeEntity)
    
    @Query("DELETE FROM stock_trades WHERE accountId = :accountId")
    suspend fun deleteTradesByAccount(accountId: String)
    
    @Query("SELECT * FROM stock_trades WHERE accountId = :accountId ORDER BY tradedAt DESC")
    fun getTradesByAccountFlow(accountId: String): Flow<List<StockTradeEntity>>
}