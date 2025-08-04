package com.example.test.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.example.test.data.local.dao.*
import com.example.test.data.local.entity.*

@Database(
    entities = [
        StockEntity::class,
        StockTradeEntity::class,
        InterestStockEntity::class,
        ChartCandleEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun stockDao(): StockDao
    abstract fun stockTradeDao(): StockTradeDao
    abstract fun interestStockDao(): InterestStockDao
    abstract fun chartCandleDao(): ChartCandleDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}