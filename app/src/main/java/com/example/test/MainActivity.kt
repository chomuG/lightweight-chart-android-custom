package com.example.test

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.test.components.*
import com.example.test.presentation.ui.screen.MockTradingScreen
import com.example.test.ui.theme.TestTheme
import com.example.test.viewmodel.ChartEvent
import com.example.test.viewmodel.ChartViewModel
import com.example.test.viewmodel.MultiPanelViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            TestTheme {
                var currentScreen by remember { mutableStateOf("main") }
                
                when (currentScreen) {
                    "main" -> ChartDemoScreen(
                        onMultiPanelClick = { currentScreen = "multiPanel" },
                        onLineToolsClick = { currentScreen = "lineTools" },
                        onMockTradingClick = { currentScreen = "mockTrading" }
                    )
                    "multiPanel" -> MultiPanelDemoScreen(
                        onBackClick = { currentScreen = "main" }
                    )
                    "lineTools" -> LineToolsDemoScreen(
                        onBackClick = { currentScreen = "main" }
                    )
                    "mockTrading" -> MockTradingScreen(
                        onBackClick = { currentScreen = "main" }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartDemoScreen(
    viewModel: ChartViewModel = viewModel(),
    onMultiPanelClick: () -> Unit = {},
    onLineToolsClick: () -> Unit = {},
    onMockTradingClick: () -> Unit = {}
) {
    val chartOptions by viewModel.chartOptions.collectAsState()
    val lineData by viewModel.lineData.collectAsState()
    val candlestickData by viewModel.candlestickData.collectAsState()
    val seriesType by viewModel.seriesType.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    var drawingMode by remember { mutableStateOf(false) }
    
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    
    // Handle chart events
    LaunchedEffect(Unit) {
        viewModel.chartEvents.collect { event ->
            when (event) {
                is ChartEvent.ChartReady -> {
                    snackbarMessage = "Chart is ready!"
                    showSnackbar = true
                }
                is ChartEvent.DataPointClicked -> {
                    snackbarMessage = "Clicked: ${event.time} - ${event.value}"
                    showSnackbar = true
                }
                is ChartEvent.CrosshairMoved -> {
                    // Handle crosshair move if needed
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lightweight Charts Demo") },
                actions = {
                    IconButton(onClick = onMultiPanelClick) {
                        Text("📊")
                    }
                    IconButton(onClick = onLineToolsClick) {
                        Text("📏")
                    }
                    IconButton(onClick = onMockTradingClick) {
                        Text("💰")
                    }
                    IconButton(onClick = { viewModel.toggleTheme() }) {
                        Text("🌓")
                    }
                }
            )
        },
        snackbarHost = {
            if (showSnackbar) {
                Snackbar(
                    action = {
                        TextButton(onClick = { showSnackbar = false }) {
                            Text("OK")
                        }
                    }
                ) {
                    Text(snackbarMessage)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Chart controls
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Chart Controls",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Data source buttons
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            listOf(
                                "Sample" to { viewModel.loadSampleData() },
                                "Crypto" to { viewModel.loadCryptoData() },
                                "Stock" to { viewModel.loadStockData() },
                                "Volume" to { viewModel.loadVolumeData() },
                                "Real-time" to { viewModel.startRealTimeUpdates() }
                            )
                        ) { (label, action) ->
                            Button(
                                onClick = action,
                                enabled = !isLoading
                            ) {
                                Text(label)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Chart type buttons
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(SeriesType.values()) { type ->
                            FilterChip(
                                onClick = { viewModel.setSeriesType(type) },
                                label = { Text(type.name) },
                                selected = seriesType == type
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Additional controls
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.addRandomDataPoint() },
                            enabled = !isLoading
                        ) {
                            Text("Add Point")
                        }
                        
                        FilterChip(
                            onClick = { drawingMode = !drawingMode },
                            label = { Text(if (drawingMode) "추세선 OFF" else "추세선 ON") },
                            selected = drawingMode,
                            leadingIcon = if (drawingMode) {
                                { Text("📏") }
                            } else null
                        )
                    }
                }
            }
            
            // Chart
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LightweightChart(
                            data = if (seriesType != SeriesType.CANDLESTICK) lineData else emptyList(),
                            candlestickData = if (seriesType == SeriesType.CANDLESTICK) candlestickData else emptyList(),
                            seriesType = seriesType,
                            options = chartOptions,
                            drawingMode = drawingMode,
                            modifier = Modifier.fillMaxSize(),
                            onChartReady = { viewModel.onChartReady() },
                            onDataPointClick = { time, value ->
                                if (!drawingMode) {
                                    viewModel.onDataPointClick(time, value)
                                }
                            },
                            onCrosshairMove = { time, value ->
                                if (!drawingMode) {
                                    viewModel.onCrosshairMove(time, value)
                                }
                            },
                            onLineToolCreated = { toolType, points ->
                                snackbarMessage = "추세선이 그려졌습니다!"
                                showSnackbar = true
                            }
                        )
                    }
                }
            }
            
            // Status bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Type: ${seriesType.name}")
                    Text("Points: ${if (seriesType == SeriesType.CANDLESTICK) candlestickData.size else lineData.size}")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiPanelDemoScreen(
    viewModel: MultiPanelViewModel = viewModel(),
    onBackClick: () -> Unit = {}
) {
    val multiPanelData by viewModel.multiPanelData.collectAsState()
    val selectedIndicators by viewModel.selectedIndicators.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    
    val chartOptions = remember(isDarkTheme) {
        ChartOptions(
            width = 800,
            height = 600,
            layout = LayoutOptions(
                backgroundColor = if (isDarkTheme) "#1a1a1a" else "#FFFFFF",
                textColor = if (isDarkTheme) "#d1d4dc" else "#333333"
            )
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Multi-Panel Chart Demo") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Text("←")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Text("🔄")
                    }
                    IconButton(onClick = { viewModel.toggleTheme() }) {
                        Text("🌓")
                    }
                }
            )
        },
        snackbarHost = {
            if (showSnackbar) {
                Snackbar(
                    action = {
                        TextButton(onClick = { showSnackbar = false }) {
                            Text("OK")
                        }
                    }
                ) {
                    Text(snackbarMessage)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Indicator selection controls
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Technical Indicators",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Indicator toggle buttons
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(IndicatorType.values()) { indicator ->
                            FilterChip(
                                onClick = { viewModel.toggleIndicator(indicator) },
                                label = { Text(indicator.name) },
                                selected = selectedIndicators.contains(indicator)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Selected: ${selectedIndicators.joinToString { it.name }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Multi-panel chart
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                MultiPanelChart(
                    data = multiPanelData,
                    chartOptions = chartOptions,
                    modifier = Modifier.fillMaxSize(),
                    onChartReady = {
                        snackbarMessage = "Multi-panel chart ready!"
                        showSnackbar = true
                    },
                    onDataPointClick = { time, value, panelId ->
                        snackbarMessage = "Clicked $panelId: $time - $value"
                        showSnackbar = true
                    },
                    onCrosshairMove = { time, value, panelId ->
                        // Handle crosshair move if needed
                    }
                )
            }
            
            // Status bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Price Data: ${multiPanelData.priceData.size} candles")
                        Text("Indicators: ${multiPanelData.indicators.size}")
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    multiPanelData.indicators.forEach { indicator ->
                        Text(
                            text = "${indicator.name}: ${indicator.data.size} points",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LineToolsDemoScreen(
    onBackClick: () -> Unit = {}
) {
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Line Tools Demo") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Text("←")
                    }
                }
            )
        },
        snackbarHost = {
            if (showSnackbar) {
                Snackbar(
                    action = {
                        TextButton(onClick = { showSnackbar = false }) {
                            Text("OK")
                        }
                    }
                ) {
                    Text(snackbarMessage)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Info card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "TradingView Line Tools",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "도구 선택 → 터치로 크로스헤어 표시 → 드래그로 위치 조정 → 터치로 점 설정",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "ESC: 취소/선택모드, Delete: 삭제",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            
            // Line tools chart
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                LineToolsChart(
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Status bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "v5.0.8 Compatible Line Tools",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

