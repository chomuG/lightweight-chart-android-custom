package com.example.test.di

import com.example.test.data.remote.api.ChartApiService
import com.example.test.data.remote.api.StockApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.example.com/") // TODO: Replace with actual base URL from BuildConfig
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    @Provides
    @Singleton
    fun provideStockApiService(retrofit: Retrofit): StockApiService {
        return retrofit.create(StockApiService::class.java)
    }
    
    @Provides
    @Singleton
    fun provideChartApiService(retrofit: Retrofit): ChartApiService {
        return retrofit.create(ChartApiService::class.java)
    }
}