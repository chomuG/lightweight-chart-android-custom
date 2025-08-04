# Multi-Panel Chart Implementation (Native TradingView API)

This document describes the multi-panel chart implementation using TradingView Lightweight Charts' **native `addPane()` API** in Android Jetpack Compose.

## ⚠️ Important: Using Native TradingView API

This implementation **does NOT create custom features**. Instead, it uses the built-in TradingView Lightweight Charts API:
- `chart.addPane(true)` - Creates new chart panes
- `pane.setStretchFactor()` - Adjusts pane heights  
- `chart.addSeries(SeriesType, options, paneIndex)` - Adds series to specific panes
- `chart.panes()` - Gets all chart panes

## Features

### 📊 Multi-Panel Support
- **Main Price Panel**: Displays candlestick/OHLC data with full chart functionality
- **Technical Indicator Panels**: Separate panels below the main chart for indicators
- **Dynamic Panel Management**: Add/remove indicator panels on-the-fly
- **Synchronized Crosshairs**: Crosshair movement synchronized across all panels
- **Independent Scaling**: Each panel has its own price scale and zoom level

### 🔧 Technical Indicators
- **RSI (Relative Strength Index)**: 14-period RSI with 30/70 overbought/oversold lines
- **MACD**: Moving Average Convergence Divergence with signal line and histogram
- **Volume**: Trading volume with customizable colors
- **Stochastic**: %K oscillator for momentum analysis
- **Bollinger Bands**: Coming soon

### 🎨 Customization
- **Dark/Light Themes**: Toggle between dark and light chart themes
- **Indicator Colors**: Customizable colors for each indicator
- **Panel Heights**: Adjustable height for each indicator panel
- **Line Styles**: Different line widths and styles for indicators

## File Structure

```
app/src/main/java/com/example/test/
├── components/
│   ├── LightweightChart.kt          # Original single-panel chart
│   ├── MultiPanelChart.kt           # Multi-panel chart implementation
│   └── SecureLightweightChart.kt    # Security-enhanced version
├── viewmodel/
│   ├── ChartViewModel.kt            # Original chart view model
│   └── MultiPanelViewModel.kt       # Multi-panel chart view model
└── MainActivity.kt                  # Main activity with both demos
```

## Technical Implementation

### MultiPanelChart.kt
```kotlin
@Composable
fun MultiPanelChart(
    data: MultiPanelData,
    chartOptions: ChartOptions = ChartOptions(),
    modifier: Modifier = Modifier,
    onChartReady: (() -> Unit)? = null,
    onDataPointClick: ((String, Double, String) -> Unit)? = null,
    onCrosshairMove: ((String?, Double?, String?) -> Unit)? = null
)
```

**Key Features:**
- WebView-based integration with TradingView Lightweight Charts
- Base64 encoding for secure data transmission
- Dynamic HTML generation for multiple chart panels
- JavaScript-Android bridge for event handling
- Automatic panel creation based on indicator data

### Data Structures

```kotlin
@Serializable
data class MultiPanelData(
    val priceData: List<CandlestickData>,
    val indicators: List<IndicatorData> = emptyList()
)

@Serializable
data class IndicatorData(
    val type: IndicatorType,
    val name: String,
    val data: List<ChartData>,
    val options: IndicatorOptions = IndicatorOptions()
)

enum class IndicatorType {
    RSI, MACD, VOLUME, STOCHASTIC, BOLLINGER_BANDS
}
```

### Technical Indicator Calculations

#### RSI (Relative Strength Index)
```kotlin
fun calculateRSI(prices: List<Double>, period: Int = 14): List<ChartData>
```
- Uses Wilder's smoothing method
- 14-period default with configurable period
- Returns values from 0-100

#### MACD
```kotlin
fun calculateMACD(
    prices: List<Double>, 
    fastPeriod: Int = 12, 
    slowPeriod: Int = 26, 
    signalPeriod: Int = 9
): Triple<List<ChartData>, List<ChartData>, List<ChartData>>
```
- Returns MACD line, signal line, and histogram
- Uses exponential moving averages (EMA)
- Standard 12,26,9 parameters

## HTML/JavaScript Implementation

### Chart Creation Using Native API
**Single chart instance** with multiple panes using built-in API:

```javascript
// Create main chart (includes default pane)
const chart = LightweightCharts.createChart(container, options);

// Add main candlestick series to default pane (pane 0)
const mainSeries = chart.addSeries(LightweightCharts.CandlestickSeries, {
    upColor: '#26a69a',
    downColor: '#ef5350'
});

// Add new panes for indicators using NATIVE API
const indicatorPane = chart.addPane(true);  // <- Native TradingView API

// Add series to specific pane using paneIndex
const rsiSeries = chart.addSeries(
    LightweightCharts.LineSeries, 
    { color: '#9C27B0' }, 
    indicatorPane.paneIndex()    // <- Native pane targeting
);
```

### Series Types by Indicator
- **RSI**: LineSeries with 30/70 reference lines
- **MACD**: LineSeries for MACD + signal, HistogramSeries for histogram
- **Volume**: HistogramSeries
- **Stochastic**: LineSeries

### Pane Height Management Using Native API
```javascript
function adjustPaneHeights() {
    const allPanes = chart.panes();  // <- Native API to get all panes
    
    allPanes.forEach((pane, index) => {
        if (index === 0) {
            // Main pane gets 60% of space
            pane.setStretchFactor(300);  // <- Native API
        } else {
            // Indicator panes share remaining 40%
            pane.setStretchFactor(100);  // <- Native API
        }
    });
}
```

### Native Crosshair Synchronization
```javascript
// Single chart instance = automatic crosshair sync across all panes!
chart.subscribeCrosshairMove(param => {
    // All panes automatically synchronized by TradingView
    // No custom sync code needed!
});
```

## Usage

### Basic Usage
```kotlin
val multiPanelData = MultiPanelData(
    priceData = candlestickData,
    indicators = listOf(
        IndicatorData(
            type = IndicatorType.RSI,
            name = "RSI (14)",
            data = rsiData
        ),
        IndicatorData(
            type = IndicatorType.MACD,
            name = "MACD (12,26,9)",
            data = macdData
        )
    )
)

MultiPanelChart(
    data = multiPanelData,
    onChartReady = { /* Chart ready */ },
    onDataPointClick = { time, value, panelId -> /* Handle click */ }
)
```

### ViewModel Integration
```kotlin
class MultiPanelViewModel : ViewModel() {
    fun toggleIndicator(indicator: IndicatorType) {
        // Add/remove indicators dynamically
    }
    
    fun refreshData() {
        // Generate new data with calculations
    }
}
```

## Navigation

### From Main Chart to Multi-Panel
- Click the 📊 icon in the top bar of the main chart demo
- This switches to the multi-panel view

### From Multi-Panel back to Main Chart  
- Click the ← back button in the multi-panel demo
- This returns to the single-panel chart demo

## Performance Considerations

1. **Single Chart Instance**: All panes managed by one TradingView chart instance (native efficiency)
2. **Native Pane Management**: Uses built-in TradingView pane APIs (no custom overhead)  
3. **Automatic Synchronization**: Built-in crosshair sync across panes (no custom code)
4. **Data Transmission**: Base64 encoding prevents JavaScript injection
5. **Memory Management**: Native pane lifecycle managed by TradingView library
6. **Calculation Caching**: Technical indicators calculated once and cached in Kotlin

## Browser Compatibility

- **Chrome/Chromium WebView**: Full support
- **System WebView**: Depends on Android version
- **Network Requirements**: Requires internet for CDN script loading

## Error Handling

1. **Script Loading Failures**: Graceful fallback with error messages
2. **Calculation Errors**: Default values and null checks
3. **WebView Issues**: Reflection-based method calls with try-catch
4. **Data Validation**: Input sanitization and type checking

## Future Enhancements

- [ ] Bollinger Bands indicator
- [ ] Williams %R oscillator
- [ ] Moving averages overlay on price chart
- [ ] Volume-weighted indicators
- [ ] Custom drawing tools
- [ ] Export chart as image
- [ ] Real-time data streaming
- [ ] Multiple timeframe support

## Dependencies

- TradingView Lightweight Charts v5.0.8 (via CDN)
- Android WebView
- Jetpack Compose
- Kotlin Serialization
- Kotlin Coroutines

## License

This implementation follows the same license requirements as TradingView Lightweight Charts, including attribution requirements.