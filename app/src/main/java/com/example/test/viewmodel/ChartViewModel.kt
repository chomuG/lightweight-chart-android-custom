package com.example.test.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.test.components.*
import com.example.test.data.ChartDataProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for managing chart state and interactions
 */
class ChartViewModel : ViewModel() {
    
    // Chart configuration
    private val _chartOptions = MutableStateFlow(
        ChartOptions(
            width = 600,
            height = 400,
            layout = LayoutOptions(
                backgroundColor = "#FFFFFF",
                textColor = "#333333"
            )
        )
    )
    val chartOptions: StateFlow<ChartOptions> = _chartOptions.asStateFlow()
    
    // Chart data
    private val _lineData = MutableStateFlow<List<ChartData>>(emptyList())
    val lineData: StateFlow<List<ChartData>> = _lineData.asStateFlow()
    
    private val _candlestickData = MutableStateFlow<List<CandlestickData>>(emptyList())
    val candlestickData: StateFlow<List<CandlestickData>> = _candlestickData.asStateFlow()
    
    // Chart type
    private val _seriesType = MutableStateFlow(SeriesType.LINE)
    val seriesType: StateFlow<SeriesType> = _seriesType.asStateFlow()
    
    // Chart events
    private val _chartEvents = MutableSharedFlow<ChartEvent>()
    val chartEvents: SharedFlow<ChartEvent> = _chartEvents.asSharedFlow()
    
    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        loadSampleData()
    }
    
    /**
     * Load sample data for demonstration
     */
    fun loadSampleData() {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val sampleLineData = ChartDataProvider.generateSampleLineData(30)
                _lineData.value = sampleLineData
                
                val sampleCandlestickData = ChartDataProvider.generateSampleCandlestickData(30)
                _candlestickData.value = sampleCandlestickData
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Load cryptocurrency sample data
     */
    fun loadCryptoData() {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val cryptoData = ChartDataProvider.getSampleCryptoPriceData()
                _lineData.value = cryptoData
                _seriesType.value = SeriesType.AREA
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Load stock sample data
     */
    fun loadStockData() {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val stockData = ChartDataProvider.getSampleStockData()
                _candlestickData.value = stockData
                _seriesType.value = SeriesType.CANDLESTICK
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Load volume data
     */
    fun loadVolumeData() {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val volumeData = ChartDataProvider.getSampleVolumeData()
                _lineData.value = volumeData
                _seriesType.value = SeriesType.HISTOGRAM
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Start real-time data updates
     */
    fun startRealTimeUpdates() {
        viewModelScope.launch {
            val lastValue = _lineData.value.lastOrNull()?.value ?: 100.0
            
            ChartDataProvider.generateRealTimeData(lastValue)
                .collect { newData ->
                    val currentData = _lineData.value.toMutableList()
                    currentData.add(newData)
                    
                    // Keep only last 50 points for performance
                    if (currentData.size > 50) {
                        currentData.removeAt(0)
                    }
                    
                    _lineData.value = currentData
                    _seriesType.value = SeriesType.LINE
                }
        }
    }
    
    /**
     * Change chart series type
     */
    fun setSeriesType(type: SeriesType) {
        _seriesType.value = type
    }
    
    /**
     * Update chart options
     */
    fun updateChartOptions(options: ChartOptions) {
        _chartOptions.value = options
    }
    
    /**
     * Handle chart data point click
     */
    fun onDataPointClick(time: String, value: Double) {
        viewModelScope.launch {
            _chartEvents.emit(
                ChartEvent.DataPointClicked(time, value)
            )
        }
    }
    
    /**
     * Handle crosshair move
     */
    fun onCrosshairMove(time: String?, value: Double?) {
        viewModelScope.launch {
            _chartEvents.emit(
                ChartEvent.CrosshairMoved(time, value)
            )
        }
    }
    
    /**
     * Handle chart ready event
     */
    fun onChartReady() {
        viewModelScope.launch {
            _chartEvents.emit(ChartEvent.ChartReady)
        }
    }
    
    /**
     * Toggle dark/light theme
     */
    fun toggleTheme() {
        val currentOptions = _chartOptions.value
        val isDark = currentOptions.layout.backgroundColor == "#1E1E1E"
        
        val newLayout = if (isDark) {
            LayoutOptions(
                backgroundColor = "#FFFFFF",
                textColor = "#333333"
            )
        } else {
            LayoutOptions(
                backgroundColor = "#1E1E1E",
                textColor = "#FFFFFF"
            )
        }
        
        _chartOptions.value = currentOptions.copy(layout = newLayout)
    }
    
    /**
     * Add random data point
     */
    fun addRandomDataPoint() {
        viewModelScope.launch {
            val currentData = _lineData.value.toMutableList()
            val lastValue = currentData.lastOrNull()?.value ?: 100.0
            val newValue = lastValue + (-10..10).random()
            
            val newPoint = ChartData(
                time = "2024-${(1..12).random().toString().padStart(2, '0')}-${(1..28).random().toString().padStart(2, '0')}",
                value = newValue.coerceAtLeast(0.1)
            )
            
            currentData.add(newPoint)
            _lineData.value = currentData
        }
    }
}

/**
 * Chart events for handling user interactions
 */
sealed class ChartEvent {
    data object ChartReady : ChartEvent()
    data class DataPointClicked(val time: String, val value: Double) : ChartEvent()
    data class CrosshairMoved(val time: String?, val value: Double?) : ChartEvent()
}