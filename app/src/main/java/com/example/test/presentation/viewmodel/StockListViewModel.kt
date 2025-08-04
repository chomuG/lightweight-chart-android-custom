package com.example.test.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.test.domain.repository.StockRepository
import com.example.test.presentation.ui.state.StockListUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

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
            _uiState.update { it.copy(isLoading = true) }
            
            stockRepository.getStocks().collect { result ->
                result.fold(
                    onSuccess = { stocks ->
                        _uiState.update { 
                            it.copy(
                                stocks = stocks, 
                                isLoading = false,
                                error = null
                            ) 
                        }
                    },
                    onFailure = { error ->
                        _uiState.update { 
                            it.copy(
                                error = error.message ?: "Unknown error occurred", 
                                isLoading = false
                            ) 
                        }
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
                        _uiState.update { 
                            it.copy(
                                stocks = stocks, 
                                isLoading = false,
                                error = null
                            ) 
                        }
                    },
                    onFailure = { error ->
                        _uiState.update { 
                            it.copy(
                                error = error.message ?: "Search failed", 
                                isLoading = false
                            ) 
                        }
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
                        it.copy(error = error.message ?: "Failed to add to interest stocks") 
                    }
                }
            )
        }
    }
    
    fun removeFromInterest(stockId: String) {
        viewModelScope.launch {
            stockRepository.removeInterestStock(stockId).fold(
                onSuccess = {
                    _uiState.update { 
                        it.copy(message = "관심종목에서 제거되었습니다") 
                    }
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(error = error.message ?: "Failed to remove from interest stocks") 
                    }
                }
            )
        }
    }
    
    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}