# 모의투자 & 차트 시스템 MVVM + Hilt 아키텍처

## 📊 핵심 도메인 엔티티

### 1. Stock 관련
```kotlin
// 주식 정보
data class Stock(
    val stockId: String,
    val code: String,
    val name: String,
    val market: String,
    val currentPrice: Double,
    val changeRate: Double,
    val volume: Long,
    val marketCap: Long
)

// 실시간 주가
data class StockPrice(
    val stockId: String,
    val price: Double,
    val changeAmount: Double,
    val changeRate: Double,
    val volume: Long,
    val timestamp: Long
)
```

### 2. Trading 관련
```kotlin
// 매매 내역
data class StockTrade(
    val tradeId: String,
    val accountId: String,
    val stockId: String,
    val tradeType: TradeType, // BUY, SELL
    val quantity: Int,
    val price: Double,
    val totalAmount: Double,
    val tradedAt: LocalDateTime
)

// 포트폴리오 보유 종목
data class StockHolding(
    val holdingId: String,
    val accountId: String,
    val stockId: String,
    val quantity: Int,
    val avgBuyPrice: Double,
    val currentPrice: Double,
    val profitLoss: Double,
    val profitRate: Double
)
```

### 3. Chart 관련
```kotlin
// 차트 데이터
data class ChartData(
    val stockId: String,
    val interval: ChartInterval, // MINUTE, HOUR, DAY, WEEK, MONTH
    val candles: List<Candle>
)

data class Candle(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long
)

// 차트 패턴 분석
data class PatternAnalysis(
    val stockId: String,
    val patternType: String,
    val confidence: Double,
    val description: String,
    val analyzedAt: LocalDateTime
)
```

## 🗄️ Room Database 엔티티

```kotlin
@Entity(tableName = "stocks")
data class StockEntity(
    @PrimaryKey val stockId: String,
    val code: String,
    val name: String,
    val market: String,
    val currentPrice: Double,
    val changeRate: Double,
    val volume: Long,
    val updatedAt: Long
)

@Entity(tableName = "stock_trades")
data class StockTradeEntity(
    @PrimaryKey val tradeId: String,
    val accountId: String,
    val stockId: String,
    val tradeType: String,
    val quantity: Int,
    val price: Double,
    val totalAmount: Double,
    val tradedAt: Long
)

@Entity(tableName = "interest_stocks")
data class InterestStockEntity(
    @PrimaryKey val interestId: String,
    val userId: String,
    val stockId: String,
    val addedAt: Long
)

@Entity(tableName = "chart_candles")
data class ChartCandleEntity(
    @PrimaryKey val id: String,
    val stockId: String,
    val interval: String,
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long
)
```

## 📡 API 서비스 인터페이스

```kotlin
interface StockApiService {
    @GET("/api/stocks")
    suspend fun getStocks(): Response<List<StockDto>>
    
    @GET("/api/stocks/search")
    suspend fun searchStocks(@Query("q") query: String): Response<List<StockDto>>
    
    @POST("/api/stocks/buy")
    suspend fun buyStock(@Body request: BuyStockRequest): Response<TradeResult>
    
    @POST("/api/stocks/sell")
    suspend fun sellStock(@Body request: SellStockRequest): Response<TradeResult>
    
    @GET("/api/stocks/{stockId}/my-transactions")
    suspend fun getMyTransactions(@Path("stockId") stockId: String): Response<List<StockTradeDto>>
    
    @POST("/api/stocks/interest")
    suspend fun addInterestStock(@Body request: AddInterestRequest): Response<Unit>
    
    @DELETE("/api/stocks/interest/{stockId}")
    suspend fun removeInterestStock(@Path("stockId") stockId: String): Response<Unit>
}

interface ChartApiService {
    @GET("/api/charts/{stockId}")
    suspend fun getChartData(
        @Path("stockId") stockId: String,
        @Query("interval") interval: String
    ): Response<ChartDataDto>
    
    @GET("/api/charts/{stockId}/pattern-analysis")
    suspend fun getPatternAnalysis(@Path("stockId") stockId: String): Response<PatternAnalysisDto>
}
```

## 🔄 Repository 계층

```kotlin
interface StockRepository {
    suspend fun getStocks(): Flow<Result<List<Stock>>>
    suspend fun searchStocks(query: String): Flow<Result<List<Stock>>>
    suspend fun buyStock(stockId: String, quantity: Int, price: Double): Result<TradeResult>
    suspend fun sellStock(stockId: String, quantity: Int, price: Double): Result<TradeResult>
    suspend fun getMyTransactions(stockId: String): Flow<Result<List<StockTrade>>>
    suspend fun getInterestStocks(): Flow<Result<List<Stock>>>
    suspend fun addInterestStock(stockId: String): Result<Unit>
    suspend fun removeInterestStock(stockId: String): Result<Unit>
}

interface ChartRepository {
    suspend fun getChartData(stockId: String, interval: ChartInterval): Flow<Result<ChartData>>
    suspend fun getPatternAnalysis(stockId: String): Flow<Result<PatternAnalysis>>
    suspend fun getCachedChartData(stockId: String, interval: ChartInterval): ChartData?
    suspend fun cacheChartData(chartData: ChartData)
}

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
                emit(Result.failure(Exception("Failed to fetch stocks")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    override suspend fun buyStock(stockId: String, quantity: Int, price: Double): Result<TradeResult> {
        return try {
            val request = BuyStockRequest(stockId, quantity, price)
            val response = stockApiService.buyStock(request)
            if (response.isSuccessful) {
                val result = response.body()!!
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
                Result.failure(Exception("Failed to buy stock"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

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
                emit(Result.failure(Exception("Failed to fetch chart data")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    private fun isDataStale(chartData: ChartData): Boolean {
        val now = System.currentTimeMillis()
        val latestCandle = chartData.candles.maxByOrNull { it.timestamp }
        return latestCandle?.let { now - it.timestamp > 60_000 } ?: true // 1분 이상 지나면 stale
    }
}
```

## 🎯 ViewModel 계층

```kotlin
@HiltViewModel
class StockListViewModel @Inject constructor(
    private val stockRepository: StockRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(StockListUiState())
    val uiState: StateFlow<StockListUiState> = _uiState.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    init {
        loadStocks()
        observeSearchQuery()
    }
    
    private fun loadStocks() {
        viewModelScope.launch {
            stockRepository.getStocks().collect { result ->
                result.fold(
                    onSuccess = { stocks ->
                        _uiState.update { it.copy(stocks = stocks, isLoading = false) }
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(error = error.message, isLoading = false) }
                    }
                )
            }
        }
    }
    
    private fun observeSearchQuery() {
        viewModelScope.launch {
            searchQuery
                .debounce(300)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.isNotEmpty()) {
                        searchStocks(query)
                    } else {
                        loadStocks()
                    }
                }
        }
    }
    
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }
    
    private fun searchStocks(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            stockRepository.searchStocks(query).collect { result ->
                result.fold(
                    onSuccess = { stocks ->
                        _uiState.update { it.copy(stocks = stocks, isLoading = false) }
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(error = error.message, isLoading = false) }
                    }
                )
            }
        }
    }
    
    fun addToInterest(stockId: String) {
        viewModelScope.launch {
            stockRepository.addInterestStock(stockId).fold(
                onSuccess = {
                    _uiState.update { 
                        it.copy(message = "관심종목에 추가되었습니다") 
                    }
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(error = error.message) 
                    }
                }
            )
        }
    }
}

@HiltViewModel
class TradingViewModel @Inject constructor(
    private val stockRepository: StockRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TradingUiState())
    val uiState: StateFlow<TradingUiState> = _uiState.asStateFlow()
    
    fun buyStock(stockId: String, quantity: Int, price: Double) {
        viewModelScope.launch {
            _uiState.update { it.copy(isTrading = true) }
            
            stockRepository.buyStock(stockId, quantity, price).fold(
                onSuccess = { result ->
                    _uiState.update { 
                        it.copy(
                            isTrading = false,
                            tradeResult = result,
                            message = "매수 주문이 완료되었습니다"
                        ) 
                    }
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(
                            isTrading = false,
                            error = error.message
                        ) 
                    }
                }
            )
        }
    }
    
    fun sellStock(stockId: String, quantity: Int, price: Double) {
        viewModelScope.launch {
            _uiState.update { it.copy(isTrading = true) }
            
            stockRepository.sellStock(stockId, quantity, price).fold(
                onSuccess = { result ->
                    _uiState.update { 
                        it.copy(
                            isTrading = false,
                            tradeResult = result,
                            message = "매도 주문이 완료되었습니다"
                        ) 
                    }
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(
                            isTrading = false,
                            error = error.message
                        ) 
                    }
                }
            )
        }
    }
}

@HiltViewModel
class ChartViewModel @Inject constructor(
    private val chartRepository: ChartRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChartUiState())
    val uiState: StateFlow<ChartUiState> = _uiState.asStateFlow()
    
    fun loadChartData(stockId: String, interval: ChartInterval) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            chartRepository.getChartData(stockId, interval).collect { result ->
                result.fold(
                    onSuccess = { chartData ->
                        _uiState.update { 
                            it.copy(
                                chartData = chartData,
                                isLoading = false,
                                currentInterval = interval
                            ) 
                        }
                        
                        // 패턴 분석도 함께 로드
                        loadPatternAnalysis(stockId)
                    },
                    onFailure = { error ->
                        _uiState.update { 
                            it.copy(
                                error = error.message,
                                isLoading = false
                            ) 
                        }
                    }
                )
            }
        }
    }
    
    private fun loadPatternAnalysis(stockId: String) {
        viewModelScope.launch {
            chartRepository.getPatternAnalysis(stockId).collect { result ->
                result.fold(
                    onSuccess = { analysis ->
                        _uiState.update { it.copy(patternAnalysis = analysis) }
                    },
                    onFailure = { /* 패턴 분석 실패는 무시 */ }
                )
            }
        }
    }
    
    fun changeInterval(interval: ChartInterval) {
        val currentStockId = _uiState.value.chartData?.stockId
        if (currentStockId != null) {
            loadChartData(currentStockId, interval)
        }
    }
}
```

## 📱 UI State 클래스

```kotlin
data class StockListUiState(
    val stocks: List<Stock> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

data class TradingUiState(
    val isTrading: Boolean = false,
    val tradeResult: TradeResult? = null,
    val error: String? = null,
    val message: String? = null
)

data class ChartUiState(
    val chartData: ChartData? = null,
    val patternAnalysis: PatternAnalysis? = null,
    val currentInterval: ChartInterval = ChartInterval.DAY,
    val isLoading: Boolean = false,
    val error: String? = null
)
```

## 🔧 Hilt 모듈

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    abstract fun bindStockRepository(
        stockRepositoryImpl: StockRepositoryImpl
    ): StockRepository
    
    @Binds
    abstract fun bindChartRepository(
        chartRepositoryImpl: ChartRepositoryImpl
    ): ChartRepository
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
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

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "app_database"
        ).build()
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
```

## 🎨 Compose 화면 예시

```kotlin
@Composable
fun StockListScreen(
    viewModel: StockListViewModel = hiltViewModel(),
    onStockClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        SearchBar(
            query = searchQuery,
            onQueryChange = viewModel::onSearchQueryChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
        
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            uiState.error != null -> {
                ErrorMessage(
                    message = uiState.error,
                    onRetry = { /* 재시도 로직 */ }
                )
            }
            
            else -> {
                LazyColumn {
                    items(uiState.stocks) { stock ->
                        StockItem(
                            stock = stock,
                            onClick = { onStockClick(stock.stockId) },
                            onInterestClick = { viewModel.addToInterest(stock.stockId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChartScreen(
    stockId: String,
    viewModel: ChartViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(stockId) {
        viewModel.loadChartData(stockId, ChartInterval.DAY)
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 간격 선택 버튼들
        ChartIntervalSelector(
            currentInterval = uiState.currentInterval,
            onIntervalChanged = viewModel::changeInterval
        )
        
        // 차트 영역
        if (uiState.chartData != null) {
            LightweightChart(
                candlestickData = uiState.chartData.candles.map { it.toCandlestickData() },
                seriesType = SeriesType.CANDLESTICK,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            )
        }
        
        // 패턴 분석 결과
        uiState.patternAnalysis?.let { analysis ->
            PatternAnalysisCard(analysis = analysis)
        }
    }
}
```

이 아키텍처는 다음과 같은 특징을 가집니다:

✅ **Clean Architecture**: Repository 패턴으로 데이터 계층 분리
✅ **MVVM**: ViewModel로 UI 로직과 비즈니스 로직 분리  
✅ **Hilt**: 의존성 주입으로 결합도 낮춤
✅ **Room**: 로컬 캐싱으로 오프라인 지원
✅ **Retrofit**: REST API 통신
✅ **Flow**: 반응형 데이터 스트림
✅ **Compose**: 선언적 UI

이 구조로 모의투자와 차트 기능을 확장 가능하고 테스트하기 쉬운 형태로 구현할 수 있습니다.