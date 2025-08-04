package com.example.test.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.test.domain.model.ChartInterval
import com.example.test.domain.repository.ChartRepository
import com.example.test.presentation.ui.state.ChartUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MockChartViewModel @Inject constructor(
    private val chartRepository: ChartRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChartUiState())
    val uiState: StateFlow<ChartUiState> = _uiState.asStateFlow()
    
    fun loadChartData(stockId: String, interval: ChartInterval) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            chartRepository.getChartData(stockId, interval).collect { result ->
                result.fold(
                    onSuccess = { chartData ->
                        _uiState.update { 
                            it.copy(
                                chartData = chartData,
                                isLoading = false,
                                currentInterval = interval,
                                error = null
                            ) 
                        }
                        
                        // 패턴 분석도 함께 로드
                        loadPatternAnalysis(stockId)
                    },
                    onFailure = { error ->
                        _uiState.update { 
                            it.copy(
                                error = error.message ?: "차트 데이터 로드 실패",
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
                    onFailure = { 
                        // 패턴 분석 실패는 무시 (선택적 기능)
                    }
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
    
    fun refreshChartData() {
        val currentState = _uiState.value
        val stockId = currentState.chartData?.stockId
        if (stockId != null) {
            loadChartData(stockId, currentState.currentInterval)
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}