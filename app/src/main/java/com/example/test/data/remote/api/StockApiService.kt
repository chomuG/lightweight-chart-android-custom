package com.example.test.data.remote.api

import com.example.test.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Stock API Service Interface
 * Based on D203 team API specifications
 */
interface StockApiService {
    
    /**
     * 실시간 주식 목록 조회
     */
    @GET("/api/stocks")
    suspend fun getStocks(): Response<List<StockDto>>
    
    /**
     * 주식 종목 검색
     */
    @GET("/api/stocks/search")
    suspend fun searchStocks(@Query("q") query: String): Response<List<StockDto>>
    
    /**
     * 주식 매수
     */
    @POST("/api/stocks/buy")
    suspend fun buyStock(@Body request: BuyStockRequest): Response<TradeResultDto>
    
    /**
     * 주식 매도
     */
    @POST("/api/stocks/sell")
    suspend fun sellStock(@Body request: SellStockRequest): Response<TradeResultDto>
    
    /**
     * 종목별 매매 내역 조회
     */
    @GET("/api/stocks/{stockId}/my-transactions")
    suspend fun getMyTransactions(@Path("stockId") stockId: String): Response<List<StockTradeDto>>
    
    /**
     * 관심 종목 설정
     */
    @POST("/api/stocks/interest")
    suspend fun addInterestStock(@Body request: AddInterestRequest): Response<Unit>
    
    /**
     * 관심 종목 해제
     */
    @DELETE("/api/stocks/interest/{stockId}")
    suspend fun removeInterestStock(@Path("stockId") stockId: String): Response<Unit>
}