package com.example.test.data.repository

import com.example.test.data.local.dao.InterestStockDao
import com.example.test.data.local.dao.StockDao
import com.example.test.data.local.dao.StockTradeDao
import com.example.test.data.local.entity.InterestStockEntity
import com.example.test.data.local.entity.StockTradeEntity
import com.example.test.data.local.entity.toDomain
import com.example.test.data.local.entity.toEntity
import com.example.test.data.remote.api.StockApiService
import com.example.test.data.remote.dto.*
import com.example.test.domain.model.Stock
import com.example.test.domain.model.StockTrade
import com.example.test.domain.model.TradeResult
import com.example.test.domain.repository.StockRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StockRepositoryImpl @Inject constructor(
    private val stockApiService: StockApiService,
    private val stockDao: StockDao,
    private val tradeDao: StockTradeDao,
    private val interestDao: InterestStockDao
) : StockRepository {
    
    override suspend fun getStocks(): Flow<Result<List<Stock>>> = flow {
        try {
            // 캐시된 데이터 먼저 emit
            val cachedStocks = stockDao.getAllStocks().map { it.toDomain() }
            if (cachedStocks.isNotEmpty()) {
                emit(Result.success(cachedStocks))
            }
            
            // API 호출
            val response = stockApiService.getStocks()
            if (response.isSuccessful) {
                val stocks = response.body()?.map { it.toDomain() } ?: emptyList()
                // 캐시 업데이트
                stockDao.insertStocks(stocks.map { it.toEntity() })
                emit(Result.success(stocks))
            } else {
                emit(Result.failure(Exception("Failed to fetch stocks: ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    override suspend fun searchStocks(query: String): Flow<Result<List<Stock>>> = flow {
        try {
            // 로컬 캐시에서 먼저 검색
            val cachedResults = stockDao.searchStocks(query).map { it.toDomain() }
            if (cachedResults.isNotEmpty()) {
                emit(Result.success(cachedResults))
            }
            
            // API 검색
            val response = stockApiService.searchStocks(query)
            if (response.isSuccessful) {
                val stocks = response.body()?.map { it.toDomain() } ?: emptyList()
                // 검색 결과를 캐시에 저장
                stockDao.insertStocks(stocks.map { it.toEntity() })
                emit(Result.success(stocks))
            } else {
                if (cachedResults.isEmpty()) {
                    emit(Result.failure(Exception("Failed to search stocks: ${response.code()}")))
                }
            }
        } catch (e: Exception) {
            // 로컬 캐시 결과가 있으면 그것을 반환
            val cachedResults = stockDao.searchStocks(query).map { it.toDomain() }
            if (cachedResults.isNotEmpty()) {
                emit(Result.success(cachedResults))
            } else {
                emit(Result.failure(e))
            }
        }
    }
    
    override suspend fun buyStock(stockId: String, quantity: Int, price: Double): Result<TradeResult> {
        return try {
            val request = BuyStockRequest(stockId, quantity, price)
            val response = stockApiService.buyStock(request)
            if (response.isSuccessful) {
                val result = response.body()!!.toDomain()
                // 로컬 DB에 거래 내역 저장
                val trade = StockTradeEntity(
                    tradeId = result.tradeId,
                    accountId = result.accountId,
                    stockId = stockId,
                    tradeType = "BUY",
                    quantity = quantity,
                    price = price,
                    totalAmount = quantity * price,
                    tradedAt = System.currentTimeMillis()
                )
                tradeDao.insertTrade(trade)
                Result.success(result)
            } else {
                Result.failure(Exception("Failed to buy stock: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun sellStock(stockId: String, quantity: Int, price: Double): Result<TradeResult> {
        return try {
            val request = SellStockRequest(stockId, quantity, price)
            val response = stockApiService.sellStock(request)
            if (response.isSuccessful) {
                val result = response.body()!!.toDomain()
                // 로컬 DB에 거래 내역 저장
                val trade = StockTradeEntity(
                    tradeId = result.tradeId,
                    accountId = result.accountId,
                    stockId = stockId,
                    tradeType = "SELL",
                    quantity = quantity,
                    price = price,
                    totalAmount = quantity * price,
                    tradedAt = System.currentTimeMillis()
                )
                tradeDao.insertTrade(trade)
                Result.success(result)
            } else {
                Result.failure(Exception("Failed to sell stock: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getMyTransactions(stockId: String): Flow<Result<List<StockTrade>>> = flow {
        try {
            // 로컬 캐시에서 먼저 조회 (임시 accountId 사용)
            val cachedTrades = tradeDao.getTradesByStockAndAccount(stockId, "temp_account_id")
                .map { it.toDomain() }
            if (cachedTrades.isNotEmpty()) {
                emit(Result.success(cachedTrades))
            }
            
            // API 호출
            val response = stockApiService.getMyTransactions(stockId)
            if (response.isSuccessful) {
                val trades = response.body()?.map { it.toDomain() } ?: emptyList()
                // 캐시 업데이트
                tradeDao.insertTrades(trades.map { it.toEntity() })
                emit(Result.success(trades))
            } else {
                if (cachedTrades.isEmpty()) {
                    emit(Result.failure(Exception("Failed to fetch transactions: ${response.code()}")))
                }
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    override suspend fun getInterestStocks(): Flow<Result<List<Stock>>> = flow {
        try {
            // 로컬 관심종목 조회 (임시 userId 사용)
            val interestStocks = interestDao.getInterestStocksByUser("temp_user_id")
            val stockIds = interestStocks.map { it.stockId }
            
            if (stockIds.isNotEmpty()) {
                val stocks = mutableListOf<Stock>()
                for (stockId in stockIds) {
                    stockDao.getStockById(stockId)?.let { stockEntity ->
                        stocks.add(stockEntity.toDomain())
                    }
                }
                emit(Result.success(stocks))
            } else {
                emit(Result.success(emptyList()))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    override suspend fun addInterestStock(stockId: String): Result<Unit> {
        return try {
            val request = AddInterestRequest(stockId)
            val response = stockApiService.addInterestStock(request)
            if (response.isSuccessful) {
                // 로컬 DB에도 저장
                val interestStock = InterestStockEntity(
                    interestId = "${stockId}_${System.currentTimeMillis()}",
                    userId = "temp_user_id",
                    stockId = stockId,
                    addedAt = System.currentTimeMillis()
                )
                interestDao.insertInterestStock(interestStock)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to add interest stock: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun removeInterestStock(stockId: String): Result<Unit> {
        return try {
            val response = stockApiService.removeInterestStock(stockId)
            if (response.isSuccessful) {
                // 로컬 DB에서도 삭제
                interestDao.removeInterestStock("temp_user_id", stockId)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to remove interest stock: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}