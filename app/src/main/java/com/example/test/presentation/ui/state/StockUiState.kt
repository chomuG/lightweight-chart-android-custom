package com.example.test.presentation.ui.state

import com.example.test.domain.model.Stock
import com.example.test.domain.model.StockTrade
import com.example.test.domain.model.TradeResult

/**
 * Stock List UI State
 */
data class StockListUiState(
    val stocks: List<Stock> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

/**
 * Trading UI State
 */
data class TradingUiState(
    val isTrading: Boolean = false,
    val tradeResult: TradeResult? = null,
    val error: String? = null,
    val message: String? = null
)

/**
 * Stock Transactions UI State
 */
data class StockTransactionsUiState(
    val transactions: List<StockTrade> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Interest Stocks UI State
 */
data class InterestStocksUiState(
    val interestStocks: List<Stock> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null
)