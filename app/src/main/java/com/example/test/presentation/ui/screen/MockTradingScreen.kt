package com.example.test.presentation.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.test.components.CandlestickData
import com.example.test.components.LightweightChart
import com.example.test.components.SeriesType
import com.example.test.domain.model.ChartInterval
import com.example.test.presentation.viewmodel.MockChartViewModel
import com.example.test.presentation.viewmodel.StockListViewModel
import com.example.test.presentation.viewmodel.TradingViewModel
import com.example.test.utils.toCandlestickData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockTradingScreen(
    onBackClick: () -> Unit = {},
    stockListViewModel: StockListViewModel = hiltViewModel(),
    chartViewModel: MockChartViewModel = hiltViewModel(),
    tradingViewModel: TradingViewModel = hiltViewModel()
) {
    val stockListState by stockListViewModel.uiState.collectAsState()
    val chartState by chartViewModel.uiState.collectAsState()
    val tradingState by tradingViewModel.uiState.collectAsState()
    val transactionsState by tradingViewModel.transactionsUiState.collectAsState()
    
    var selectedStockId by remember { mutableStateOf<String?>(null) }
    var showTradingDialog by remember { mutableStateOf(false) }
    var tradeType by remember { mutableStateOf("BUY") }
    var quantity by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("모의투자 & 차트 시스템") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Text("←")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 주식 목록
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "주식 목록",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (stockListState.isLoading) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(stockListState.stocks) { stock ->
                                FilterChip(
                                    onClick = { 
                                        selectedStockId = stock.stockId
                                        chartViewModel.loadChartData(stock.stockId, ChartInterval.DAY)
                                        tradingViewModel.loadTransactions(stock.stockId)
                                    },
                                    label = { Text("${stock.name} (${stock.code})") },
                                    selected = selectedStockId == stock.stockId
                                )
                            }
                        }
                    }
                    
                    stockListState.error?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            
            // 차트 영역
            if (selectedStockId != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column {
                        // 차트 간격 선택
                        LazyRow(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(ChartInterval.values()) { interval ->
                                FilterChip(
                                    onClick = { chartViewModel.changeInterval(interval) },
                                    label = { Text(interval.name) },
                                    selected = chartState.currentInterval == interval
                                )
                            }
                        }
                        
                        // 차트
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp)
                        ) {
                            if (chartState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            } else if (chartState.chartData != null) {
                                LightweightChart(
                                    candlestickData = chartState.chartData!!.candles.map { it.toCandlestickData() },
                                    seriesType = SeriesType.CANDLESTICK,
                                    modifier = Modifier.fillMaxSize(),
                                    onChartReady = { /* Chart ready */ }
                                )
                            } else {
                                Text(
                                    text = "차트 데이터를 불러올 수 없습니다",
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                        
                        // 패턴 분석 결과
                        chartState.patternAnalysis?.let { analysis ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "패턴 분석: ${analysis.patternType}",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "신뢰도: ${(analysis.confidence * 100).toInt()}%",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = analysis.description,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
                
                // 매매 버튼 및 거래 내역
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { 
                            tradeType = "BUY"
                            showTradingDialog = true 
                        },
                        enabled = !tradingState.isTrading,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("매수")
                    }
                    
                    Button(
                        onClick = { 
                            tradeType = "SELL"
                            showTradingDialog = true 
                        },
                        enabled = !tradingState.isTrading,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("매도")
                    }
                }
                
                // 거래 내역
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "거래 내역",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (transactionsState.isLoading) {
                            CircularProgressIndicator()
                        } else {
                            LazyColumn(
                                modifier = Modifier.height(200.dp)
                            ) {
                                items(transactionsState.transactions) { transaction ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = transaction.tradeType.name,
                                                    color = if (transaction.tradeType.name == "BUY") 
                                                        MaterialTheme.colorScheme.primary 
                                                    else 
                                                        MaterialTheme.colorScheme.error
                                                )
                                                Text("${transaction.quantity}주")
                                                Text("${transaction.price}원")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // 매매 다이얼로그
    if (showTradingDialog) {
        AlertDialog(
            onDismissRequest = { showTradingDialog = false },
            title = { Text(if (tradeType == "BUY") "매수 주문" else "매도 주문") },
            text = {
                Column {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("수량") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("가격") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedStockId?.let { stockId ->
                            val qty = quantity.toIntOrNull() ?: 0
                            val prc = price.toDoubleOrNull() ?: 0.0
                            
                            if (tradeType == "BUY") {
                                tradingViewModel.buyStock(stockId, qty, prc)
                            } else {
                                tradingViewModel.sellStock(stockId, qty, prc)
                            }
                        }
                        showTradingDialog = false
                        quantity = ""
                        price = ""
                    }
                ) {
                    Text(if (tradeType == "BUY") "매수" else "매도")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTradingDialog = false }) {
                    Text("취소")
                }
            }
        )
    }
    
    // 스낵바 메시지 처리
    tradingState.message?.let { message ->
        LaunchedEffect(message) {
            // Show snackbar or toast
            tradingViewModel.clearMessage()
        }
    }
    
    tradingState.error?.let { error ->
        LaunchedEffect(error) {
            // Show error snackbar or toast
            tradingViewModel.clearError()
        }
    }
}