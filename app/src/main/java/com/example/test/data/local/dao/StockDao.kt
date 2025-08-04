package com.example.test.data.local.dao

import androidx.room.*
import com.example.test.data.local.entity.StockEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDao {
    
    @Query("SELECT * FROM stocks ORDER BY name ASC")
    suspend fun getAllStocks(): List<StockEntity>
    
    @Query("SELECT * FROM stocks WHERE stockId = :stockId")
    suspend fun getStockById(stockId: String): StockEntity?
    
    @Query("SELECT * FROM stocks WHERE name LIKE '%' || :query || '%' OR code LIKE '%' || :query || '%'")
    suspend fun searchStocks(query: String): List<StockEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStock(stock: StockEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStocks(stocks: List<StockEntity>)
    
    @Update
    suspend fun updateStock(stock: StockEntity)
    
    @Delete
    suspend fun deleteStock(stock: StockEntity)
    
    @Query("DELETE FROM stocks")
    suspend fun deleteAllStocks()
    
    @Query("SELECT * FROM stocks ORDER BY name ASC")
    fun getAllStocksFlow(): Flow<List<StockEntity>>
}