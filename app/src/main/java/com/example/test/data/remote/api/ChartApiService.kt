package com.example.test.data.remote.api

import com.example.test.data.remote.dto.ChartDataDto
import com.example.test.data.remote.dto.PatternAnalysisDto
import retrofit2.Response
import retrofit2.http.*

/**
 * Chart API Service Interface
 * Based on D203 team API specifications
 */
interface ChartApiService {
    
    /**
     * 차트 보기 - 실시간 차트 정보 제공
     */
    @GET("/api/charts/{stockId}")
    suspend fun getChartData(
        @Path("stockId") stockId: String,
        @Query("interval") interval: String
    ): Response<ChartDataDto>
    
    /**
     * 자동 패턴 분석 - 차트 패턴을 감지하여 힌트 제공
     */
    @GET("/api/charts/{stockId}/pattern-analysis")
    suspend fun getPatternAnalysis(@Path("stockId") stockId: String): Response<PatternAnalysisDto>
}