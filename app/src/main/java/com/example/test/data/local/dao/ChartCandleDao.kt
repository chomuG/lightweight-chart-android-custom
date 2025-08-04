package com.example.test.data.local.dao

import androidx.room.*
import com.example.test.data.local.entity.ChartCandleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChartCandleDao {
    
    @Query("SELECT * FROM chart_candles WHERE stockId = :stockId AND interval = :interval ORDER BY timestamp ASC")
    suspend fun getCandlesByStockAndInterval(stockId: String, interval: String): List<ChartCandleEntity>
    
    @Query("SELECT * FROM chart_candles WHERE stockId = :stockId AND interval = :interval AND timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp ASC")
    suspend fun getCandlesByStockAndIntervalInRange(
        stockId: String, 
        interval: String, 
        startTime: Long, 
        endTime: Long
    ): List<ChartCandleEntity>
    
    @Query("SELECT * FROM chart_candles WHERE id = :id")
    suspend fun getCandleById(id: String): ChartCandleEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCandle(candle: ChartCandleEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCandles(candles: List<ChartCandleEntity>)
    
    @Update
    suspend fun updateCandle(candle: ChartCandleEntity)
    
    @Delete
    suspend fun deleteCandle(candle: ChartCandleEntity)
    
    @Query("DELETE FROM chart_candles WHERE stockId = :stockId AND interval = :interval")
    suspend fun deleteCandlesByStockAndInterval(stockId: String, interval: String)
    
    @Query("DELETE FROM chart_candles WHERE timestamp < :beforeTime")
    suspend fun deleteOldCandles(beforeTime: Long)
    
    @Query("SELECT * FROM chart_candles WHERE stockId = :stockId AND interval = :interval ORDER BY timestamp ASC")
    fun getCandlesByStockAndIntervalFlow(stockId: String, interval: String): Flow<List<ChartCandleEntity>>
}