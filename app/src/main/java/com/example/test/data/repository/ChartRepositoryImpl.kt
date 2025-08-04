package com.example.test.data.repository

import com.example.test.data.local.dao.ChartCandleDao
import com.example.test.data.local.entity.toDomain
import com.example.test.data.local.entity.toEntity
import com.example.test.data.remote.api.ChartApiService
import com.example.test.data.remote.dto.toDomain
import com.example.test.domain.model.ChartData
import com.example.test.domain.model.ChartInterval
import com.example.test.domain.model.PatternAnalysis
import com.example.test.domain.repository.ChartRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChartRepositoryImpl @Inject constructor(
    private val chartApiService: ChartApiService,
    private val chartDao: ChartCandleDao
) : ChartRepository {
    
    override suspend fun getChartData(stockId: String, interval: ChartInterval): Flow<Result<ChartData>> = flow {
        try {
            // 캐시된 데이터 확인
            val cachedData = getCachedChartData(stockId, interval)
            if (cachedData != null && !isDataStale(cachedData)) {
                emit(Result.success(cachedData))
                return@flow
            }
            
            // API 호출
            val response = chartApiService.getChartData(stockId, interval.name.lowercase())
            if (response.isSuccessful) {
                val chartData = response.body()!!.toDomain()
                // 캐시 업데이트
                cacheChartData(chartData)
                emit(Result.success(chartData))
            } else {
                // API 실패 시 캐시된 데이터라도 반환
                if (cachedData != null) {
                    emit(Result.success(cachedData))
                } else {
                    emit(Result.failure(Exception("Failed to fetch chart data: ${response.code()}")))
                }
            }
        } catch (e: Exception) {
            // 예외 발생 시 캐시된 데이터 확인
            val cachedData = getCachedChartData(stockId, interval)
            if (cachedData != null) {
                emit(Result.success(cachedData))
            } else {
                emit(Result.failure(e))
            }
        }
    }
    
    override suspend fun getPatternAnalysis(stockId: String): Flow<Result<PatternAnalysis>> = flow {
        try {
            val response = chartApiService.getPatternAnalysis(stockId)
            if (response.isSuccessful) {
                val patternAnalysis = response.body()!!.toDomain()
                emit(Result.success(patternAnalysis))
            } else {
                emit(Result.failure(Exception("Failed to fetch pattern analysis: ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    override suspend fun getCachedChartData(stockId: String, interval: ChartInterval): ChartData? {
        return try {
            val candles = chartDao.getCandlesByStockAndInterval(stockId, interval.name.lowercase())
                .map { it.toDomain() }
            
            if (candles.isNotEmpty()) {
                ChartData(
                    stockId = stockId,
                    interval = interval,
                    candles = candles
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun cacheChartData(chartData: ChartData) {
        try {
            // 기존 캐시 삭제
            chartDao.deleteCandlesByStockAndInterval(
                chartData.stockId, 
                chartData.interval.name.lowercase()
            )
            
            // 새 데이터 저장
            val entities = chartData.candles.map { candle ->
                candle.toEntity(chartData.stockId, chartData.interval.name.lowercase())
            }
            chartDao.insertCandles(entities)
        } catch (e: Exception) {
            // 캐시 저장 실패는 무시 (네트워크 데이터는 정상적으로 반환됨)
            e.printStackTrace()
        }
    }
    
    private fun isDataStale(chartData: ChartData): Boolean {
        val now = System.currentTimeMillis()
        val latestCandle = chartData.candles.maxByOrNull { it.timestamp }
        return latestCandle?.let { now - it.timestamp > 60_000 } ?: true // 1분 이상 지나면 stale
    }
}