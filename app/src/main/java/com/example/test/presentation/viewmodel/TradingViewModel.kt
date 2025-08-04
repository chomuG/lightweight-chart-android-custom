package com.example.test.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.test.domain.repository.StockRepository
import com.example.test.presentation.ui.state.TradingUiState
import com.example.test.presentation.ui.state.StockTransactionsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TradingViewModel @Inject constructor(
    private val stockRepository: StockRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TradingUiState())
    val uiState: StateFlow<TradingUiState> = _uiState.asStateFlow()
    
    private val _transactionsUiState = MutableStateFlow(StockTransactionsUiState())
    val transactionsUiState: StateFlow<StockTransactionsUiState> = _transactionsUiState.asStateFlow()
    
    fun buyStock(stockId: String, quantity: Int, price: Double) {
        viewModelScope.launch {
            _uiState.update { it.copy(isTrading = true, error = null) }
            
            stockRepository.buyStock(stockId, quantity, price).fold(
                onSuccess = { result ->
                    _uiState.update { 
                        it.copy(
                            isTrading = false,
                            tradeResult = result,
                            message = "매수 주문이 완료되었습니다",
                            error = null
                        ) 
                    }
                    // 거래 완료 후 거래 내역 갱신
                    loadTransactions(stockId)
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(
                            isTrading = false,
                            error = error.message ?: "매수 주문 실패",
                            tradeResult = null
                        ) 
                    }
                }
            )
        }
    }
    
    fun sellStock(stockId: String, quantity: Int, price: Double) {
        viewModelScope.launch {
            _uiState.update { it.copy(isTrading = true, error = null) }
            
            stockRepository.sellStock(stockId, quantity, price).fold(
                onSuccess = { result ->
                    _uiState.update { 
                        it.copy(
                            isTrading = false,
                            tradeResult = result,
                            message = "매도 주문이 완료되었습니다",
                            error = null
                        ) 
                    }
                    // 거래 완료 후 거래 내역 갱신
                    loadTransactions(stockId)
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(
                            isTrading = false,
                            error = error.message ?: "매도 주문 실패",
                            tradeResult = null
                        ) 
                    }
                }
            )
        }
    }
    
    fun loadTransactions(stockId: String) {
        viewModelScope.launch {
            _transactionsUiState.update { it.copy(isLoading = true) }
            
            stockRepository.getMyTransactions(stockId).collect { result ->
                result.fold(
                    onSuccess = { transactions ->
                        _transactionsUiState.update { 
                            it.copy(
                                transactions = transactions,
                                isLoading = false,
                                error = null
                            ) 
                        }
                    },
                    onFailure = { error ->
                        _transactionsUiState.update { 
                            it.copy(
                                error = error.message ?: "거래 내역 조회 실패",
                                isLoading = false
                            ) 
                        }
                    }
                )
            }
        }
    }
    
    fun clearTradeResult() {
        _uiState.update { it.copy(tradeResult = null) }
    }
    
    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
        _transactionsUiState.update { it.copy(error = null) }
    }
}