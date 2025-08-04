package com.example.test.domain.repository

import com.example.test.domain.model.ChartData
import com.example.test.domain.model.ChartInterval
import com.example.test.domain.model.PatternAnalysis
import kotlinx.coroutines.flow.Flow

/**
 * Chart Repository Interface
 */
interface ChartRepository {
    suspend fun getChartData(stockId: String, interval: ChartInterval): Flow<Result<ChartData>>
    suspend fun getPatternAnalysis(stockId: String): Flow<Result<PatternAnalysis>>
    suspend fun getCachedChartData(stockId: String, interval: ChartInterval): ChartData?
    suspend fun cacheChartData(chartData: ChartData)
}