package com.example.test.di

import android.content.Context
import androidx.room.Room
import com.example.test.data.local.AppDatabase
import com.example.test.data.local.dao.ChartCandleDao
import com.example.test.data.local.dao.InterestStockDao
import com.example.test.data.local.dao.StockDao
import com.example.test.data.local.dao.StockTradeDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "mock_trading_database"
        )
        .fallbackToDestructiveMigration() // For development only
        .build()
    }
    
    @Provides
    fun provideStockDao(database: AppDatabase): StockDao = database.stockDao()
    
    @Provides
    fun provideStockTradeDao(database: AppDatabase): StockTradeDao = database.stockTradeDao()
    
    @Provides
    fun provideInterestStockDao(database: AppDatabase): InterestStockDao = database.interestStockDao()
    
    @Provides
    fun provideChartCandleDao(database: AppDatabase): ChartCandleDao = database.chartCandleDao()
}