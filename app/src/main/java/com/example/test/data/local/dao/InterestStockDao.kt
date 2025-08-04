package com.example.test.data.local.dao

import androidx.room.*
import com.example.test.data.local.entity.InterestStockEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InterestStockDao {
    
    @Query("SELECT * FROM interest_stocks WHERE userId = :userId ORDER BY addedAt DESC")
    suspend fun getInterestStocksByUser(userId: String): List<InterestStockEntity>
    
    @Query("SELECT * FROM interest_stocks WHERE interestId = :interestId")
    suspend fun getInterestStockById(interestId: String): InterestStockEntity?
    
    @Query("SELECT EXISTS(SELECT 1 FROM interest_stocks WHERE userId = :userId AND stockId = :stockId)")
    suspend fun isStockInInterest(userId: String, stockId: String): Boolean
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInterestStock(interestStock: InterestStockEntity)
    
    @Query("DELETE FROM interest_stocks WHERE userId = :userId AND stockId = :stockId")
    suspend fun removeInterestStock(userId: String, stockId: String)
    
    @Delete
    suspend fun deleteInterestStock(interestStock: InterestStockEntity)
    
    @Query("DELETE FROM interest_stocks WHERE userId = :userId")
    suspend fun deleteAllInterestStocksByUser(userId: String)
    
    @Query("SELECT * FROM interest_stocks WHERE userId = :userId ORDER BY addedAt DESC")
    fun getInterestStocksByUserFlow(userId: String): Flow<List<InterestStockEntity>>
}