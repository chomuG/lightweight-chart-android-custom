package com.example.test.presentation.ui.state

import com.example.test.domain.model.ChartData
import com.example.test.domain.model.ChartInterval
import com.example.test.domain.model.PatternAnalysis

/**
 * Chart UI State
 */
data class ChartUiState(
    val chartData: ChartData? = null,
    val patternAnalysis: PatternAnalysis? = null,
    val currentInterval: ChartInterval = ChartInterval.DAY,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Real-time Chart UI State
 */
data class RealTimeChartUiState(
    val chartData: ChartData? = null,
    val isConnected: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastUpdate: Long = 0L
)