package com.example.test.components

import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.JavascriptInterface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.*

/**
 * Data classes for multi-panel chart configuration
 */
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

@Serializable
data class IndicatorOptions(
    val color: String = "#2962FF",
    val lineWidth: Int = 2,
    val height: Int = 150,
    val visible: Boolean = true,
    val precision: Int = 2
)

enum class IndicatorType {
    RSI, MACD, VOLUME, STOCHASTIC, BOLLINGER_BANDS
}

/**
 * A Jetpack Compose component that renders TradingView Lightweight Charts
 * with multiple panels for technical indicators.
 */
@Composable
fun MultiPanelChart(
    data: MultiPanelData,
    chartOptions: ChartOptions = ChartOptions(),
    modifier: Modifier = Modifier,
    onChartReady: (() -> Unit)? = null,
    onDataPointClick: ((String, Double, String) -> Unit)? = null,
    onCrosshairMove: ((String?, Double?, String?) -> Unit)? = null
) {
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    
    // Generate HTML content with embedded JavaScript
    val htmlContent = remember(data, chartOptions) {
        generateMultiPanelHtml(data, chartOptions)
    }
    
    AndroidView(
        factory = { context ->
            val webView = WebView(context)
            webView.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                
                // 네트워크 액세스 허용
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                    allowFileAccessFromFileURLs = true
                    allowUniversalAccessFromFileURLs = true
                }
                
                // Mixed content 허용 (HTTPS와 HTTP 혼용)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }
                
                // 캐시 허용
                cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
            }
            
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    onChartReady?.invoke()
                }
            }
            
            // Add JavaScript interface for callbacks using reflection
            try {
                val method = webView.javaClass.getMethod(
                    "addJavascriptInterface", 
                    Any::class.java, 
                    String::class.java
                )
                method.invoke(
                    webView,
                    MultiPanelJavaScriptInterface(
                        onDataPointClick = onDataPointClick,
                        onCrosshairMove = onCrosshairMove
                    ),
                    "Android"
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            webView
        },
        update = { view ->
            view.loadDataWithBaseURL(
                "https://unpkg.com/",
                htmlContent,
                "text/html",
                "UTF-8",
                null
            )
        },
        modifier = modifier.fillMaxSize()
    )
}

/**
 * JavaScript interface for handling multi-panel chart events
 */
class MultiPanelJavaScriptInterface(
    private val onDataPointClick: ((String, Double, String) -> Unit)?,
    private val onCrosshairMove: ((String?, Double?, String?) -> Unit)?
) {
    @JavascriptInterface
    fun onDataPointClicked(time: String, value: Double, panelId: String) {
        onDataPointClick?.invoke(time, value, panelId)
    }
    
    @JavascriptInterface
    fun onCrosshairMoved(time: String?, value: Double?, panelId: String?) {
        onCrosshairMove?.invoke(time, value, panelId)
    }
}

/**
 * Technical indicator calculation functions
 */
object TechnicalIndicators {
    
    fun calculateRSI(prices: List<Double>, period: Int = 14): List<ChartData> {
        if (prices.size < period + 1) return emptyList()
        
        val gains = mutableListOf<Double>()
        val losses = mutableListOf<Double>()
        
        // Calculate price changes
        for (i in 1 until prices.size) {
            val change = prices[i] - prices[i - 1]
            gains.add(if (change > 0) change else 0.0)
            losses.add(if (change < 0) -change else 0.0)
        }
        
        val rsiValues = mutableListOf<ChartData>()
        
        // Calculate initial averages
        var avgGain = gains.take(period).average()
        var avgLoss = losses.take(period).average()
        
        for (i in period until gains.size) {
            if (avgLoss != 0.0) {
                val rs = avgGain / avgLoss
                val rsi = 100 - (100 / (1 + rs))
                rsiValues.add(ChartData(time = (i + 1).toString(), value = rsi))
            }
            
            // Update averages using Wilder's smoothing
            avgGain = (avgGain * (period - 1) + gains[i]) / period
            avgLoss = (avgLoss * (period - 1) + losses[i]) / period
        }
        
        return rsiValues
    }
    
    fun calculateMACD(
        prices: List<Double>, 
        fastPeriod: Int = 12, 
        slowPeriod: Int = 26, 
        signalPeriod: Int = 9
    ): Triple<List<ChartData>, List<ChartData>, List<ChartData>> {
        if (prices.size < slowPeriod) return Triple(emptyList(), emptyList(), emptyList())
        
        val fastEMA = calculateEMA(prices, fastPeriod)
        val slowEMA = calculateEMA(prices, slowPeriod)
        
        val macdLine = mutableListOf<ChartData>()
        val macdValues = mutableListOf<Double>()
        
        // Calculate MACD line
        for (i in slowPeriod - 1 until prices.size) {
            val macdValue = fastEMA[i - (slowPeriod - fastPeriod)] - slowEMA[i - (slowPeriod - 1)]
            macdLine.add(ChartData(time = i.toString(), value = macdValue))
            macdValues.add(macdValue)
        }
        
        // Calculate signal line (EMA of MACD)
        val signalEMA = calculateEMA(macdValues, signalPeriod)
        val signalLine = signalEMA.mapIndexed { index, value ->
            ChartData(time = (index + slowPeriod + signalPeriod - 2).toString(), value = value)
        }
        
        // Calculate histogram
        val histogram = mutableListOf<ChartData>()
        for (i in signalLine.indices) {
            val macdIndex = i + signalPeriod - 1
            if (macdIndex < macdLine.size) {
                val histValue = macdLine[macdIndex].value - signalLine[i].value
                histogram.add(ChartData(time = signalLine[i].time, value = histValue))
            }
        }
        
        return Triple(macdLine, signalLine, histogram)
    }
    
    private fun calculateEMA(prices: List<Double>, period: Int): List<Double> {
        if (prices.size < period) return emptyList()
        
        val alpha = 2.0 / (period + 1)
        val ema = mutableListOf<Double>()
        
        // Start with SMA for the first value
        ema.add(prices.take(period).average())
        
        // Calculate EMA for remaining values
        for (i in period until prices.size) {
            val emaValue = alpha * prices[i] + (1 - alpha) * ema.last()
            ema.add(emaValue)
        }
        
        return ema
    }
}

/**
 * Generates HTML content with TradingView Lightweight Charts using native addPane API
 */
private fun generateMultiPanelHtml(
    data: MultiPanelData,
    options: ChartOptions
): String {
    val json = Json { ignoreUnknownKeys = true }
    
    val priceDataJson = json.encodeToString(data.priceData)
    val indicatorsJson = json.encodeToString(data.indicators)
    val optionsJson = json.encodeToString(options)
    
    // Base64로 인코딩하여 안전하게 전달
    val priceDataBase64 = android.util.Base64.encodeToString(priceDataJson.toByteArray(), android.util.Base64.NO_WRAP)
    val indicatorsBase64 = android.util.Base64.encodeToString(indicatorsJson.toByteArray(), android.util.Base64.NO_WRAP)
    val optionsBase64 = android.util.Base64.encodeToString(optionsJson.toByteArray(), android.util.Base64.NO_WRAP)
    
    return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <style>
        body {
            margin: 0;
            padding: 0;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
        }
        #chart-container {
            width: 100%;
            height: 100vh;
        }
        #loading {
            display: flex;
            justify-content: center;
            align-items: center;
            height: 100vh;
            font-size: 18px;
            color: #666;
        }
    </style>
</head>
<body>
    <div id="loading">Loading multi-panel chart...</div>
    <div id="chart-container" style="display: none;"></div>
    
    <script>
        // Base64 디코딩 및 JSON 파싱
        function decodeBase64(base64) {
            return atob(base64);
        }
        
        // 스크립트 동적 로딩
        function loadScript(src, callback) {
            const script = document.createElement('script');
            script.src = src;
            script.onload = callback;
            script.onerror = function() {
                console.error('Failed to load script:', src);
                document.getElementById('loading').innerHTML = 'Failed to load chart library';
            };
            document.head.appendChild(script);
        }
        
        // 전역 변수
        let chart;
        let panes = [];
        let series = [];
        
        // TradingView Lightweight Charts 라이브러리 로드 (CDN)
        loadScript('https://unpkg.com/lightweight-charts/dist/lightweight-charts.standalone.production.js', function() {
            initMultiPanelChart();
        });
        
        function initMultiPanelChart() {
            try {
                // 로딩 화면 숨기고 차트 컨테이너 표시
                document.getElementById('loading').style.display = 'none';
                document.getElementById('chart-container').style.display = 'block';
                
                // Base64에서 디코딩 후 JSON 파싱
                const priceDataJson = decodeBase64('$priceDataBase64');
                const indicatorsJson = decodeBase64('$indicatorsBase64');
                const optionsJson = decodeBase64('$optionsBase64');
                
                const priceData = JSON.parse(priceDataJson);
                const indicators = JSON.parse(indicatorsJson);
                const chartOptions = JSON.parse(optionsJson);
                
                console.log('Price data:', priceData);
                console.log('Indicators:', indicators);
                
                // 차트 생성 (기본 패널 포함)
                chart = LightweightCharts.createChart(
                    document.getElementById('chart-container'),
                    {
                        width: window.innerWidth,
                        height: window.innerHeight,
                        layout: {
                            backgroundColor: (chartOptions.layout && chartOptions.layout.backgroundColor) || '#FFFFFF',
                            textColor: (chartOptions.layout && chartOptions.layout.textColor) || '#333333',
                            fontSize: 12,
                        },
                        grid: {
                            vertLines: {
                                color: '#e1e1e1',
                            },
                            horzLines: {
                                color: '#e1e1e1',
                            },
                        },
                        priceScale: {
                            position: 'right',
                            borderVisible: false,
                        },
                        timeScale: {
                            borderVisible: false,
                            timeVisible: true,
                            secondsVisible: false,
                        },
                    }
                );
                
                // 메인 캔들스틱 시리즈 추가 (기본 패널에)
                const mainSeries = chart.addSeries(LightweightCharts.CandlestickSeries, {
                    upColor: '#26a69a',
                    downColor: '#ef5350',
                    borderVisible: false,
                    wickUpColor: '#26a69a',
                    wickDownColor: '#ef5350',
                });
                
                mainSeries.setData(priceData);
                series.push({ series: mainSeries, name: 'Price', paneIndex: 0 });
                
                // 보조지표용 패널들 추가
                indicators.forEach((indicator, index) => {
                    createIndicatorPane(indicator, index + 1);
                });
                
                // 패널 높이 조정
                adjustPaneHeights();
                
                // 이벤트 핸들러 추가
                setupEventHandlers();
                
                // 자동 리사이즈
                setupAutoResize();
                
                // 전역 접근을 위한 노출
                window.lightweightChart = chart;
                window.chartPanes = panes;
                window.chartSeries = series;
                
            } catch (error) {
                console.error('Multi-panel chart initialization error:', error);
                document.getElementById('loading').innerHTML = 'Chart initialization failed: ' + error.message;
                document.getElementById('loading').style.display = 'block';
                document.getElementById('chart-container').style.display = 'none';
            }
        }
        
        function createIndicatorPane(indicator, paneIndex) {
            // 새 패널 추가
            const pane = chart.addPane(true);
            panes.push(pane);
            
            let indicatorSeries;
            
            // 보조지표 타입에 따른 시리즈 생성
            switch (indicator.type.toLowerCase()) {
                case 'rsi':
                    indicatorSeries = chart.addSeries(LightweightCharts.LineSeries, {
                        color: indicator.options.color || '#9C27B0',
                        lineWidth: indicator.options.lineWidth || 2,
                    }, pane.paneIndex());
                    
                    // RSI 기준선 추가 (70, 30)
                    const rsiRef70 = chart.addSeries(LightweightCharts.LineSeries, {
                        color: '#666666',
                        lineWidth: 1,
                        lineStyle: LightweightCharts.LineStyle.Dashed,
                    }, pane.paneIndex());
                    
                    const rsiRef30 = chart.addSeries(LightweightCharts.LineSeries, {
                        color: '#666666',
                        lineWidth: 1,
                        lineStyle: LightweightCharts.LineStyle.Dashed,
                    }, pane.paneIndex());
                    
                    if (indicator.data.length > 0) {
                        const startTime = indicator.data[0].time;
                        const endTime = indicator.data[indicator.data.length - 1].time;
                        rsiRef70.setData([
                            { time: startTime, value: 70 },
                            { time: endTime, value: 70 }
                        ]);
                        rsiRef30.setData([
                            { time: startTime, value: 30 },
                            { time: endTime, value: 30 }
                        ]);
                    }
                    break;
                    
                case 'macd':
                    // MACD 라인
                    indicatorSeries = chart.addSeries(LightweightCharts.LineSeries, {
                        color: '#2196F3',
                        lineWidth: 2,
                    }, pane.paneIndex());
                    
                    // 제로 라인
                    const zeroLine = chart.addSeries(LightweightCharts.LineSeries, {
                        color: '#666666',
                        lineWidth: 1,
                        lineStyle: LightweightCharts.LineStyle.Dashed,
                    }, pane.paneIndex());
                    
                    if (indicator.data.length > 0) {
                        const startTime = indicator.data[0].time;
                        const endTime = indicator.data[indicator.data.length - 1].time;
                        zeroLine.setData([
                            { time: startTime, value: 0 },
                            { time: endTime, value: 0 }
                        ]);
                    }
                    break;
                    
                case 'volume':
                    indicatorSeries = chart.addSeries(LightweightCharts.HistogramSeries, {
                        color: indicator.options.color || '#FF9800',
                    }, pane.paneIndex());
                    break;
                    
                default:
                    indicatorSeries = chart.addSeries(LightweightCharts.LineSeries, {
                        color: indicator.options.color || '#2962FF',
                        lineWidth: indicator.options.lineWidth || 2,
                    }, pane.paneIndex());
            }
            
            indicatorSeries.setData(indicator.data);
            series.push({
                series: indicatorSeries,
                name: indicator.name,
                paneIndex: pane.paneIndex(),
                type: indicator.type
            });
        }
        
        function adjustPaneHeights() {
            const allPanes = chart.panes();
            const totalPanes = allPanes.length;
            
            if (totalPanes === 1) {
                // 메인 패널만 있는 경우
                return;
            }
            
            // 메인 패널은 60%, 나머지 패널들은 40%를 균등 분할
            const mainPaneStretch = 300; // 기본값의 3배
            const indicatorPaneStretch = totalPanes > 1 ? 100 : 50; // 기본값
            
            allPanes.forEach((pane, index) => {
                if (index === 0) {
                    // 메인 패널
                    pane.setStretchFactor(mainPaneStretch);
                } else {
                    // 보조지표 패널
                    pane.setStretchFactor(indicatorPaneStretch);
                }
            });
        }
        
        function setupEventHandlers() {
            chart.subscribeCrosshairMove(param => {
                try {
                    if (typeof Android !== 'undefined' && Android.onCrosshairMoved) {
                        const time = param.time ? param.time.toString() : null;
                        // 메인 시리즈의 값 가져오기
                        const mainSeriesData = series.find(s => s.paneIndex === 0);
                        const value = param.seriesData && mainSeriesData && param.seriesData.get(mainSeriesData.series) 
                            ? param.seriesData.get(mainSeriesData.series) 
                            : null;
                        Android.onCrosshairMoved(time, value, 'multi-panel');
                    }
                } catch (e) {
                    console.error('Crosshair event error:', e);
                }
            });
            
            chart.subscribeClick(param => {
                try {
                    if (typeof Android !== 'undefined' && Android.onDataPointClicked && param.time && param.seriesData) {
                        const mainSeriesData = series.find(s => s.paneIndex === 0);
                        if (mainSeriesData) {
                            const value = param.seriesData.get(mainSeriesData.series);
                            if (value !== undefined) {
                                Android.onDataPointClicked(param.time.toString(), value, 'multi-panel');
                            }
                        }
                    }
                } catch (e) {
                    console.error('Click event error:', e);
                }
            });
        }
        
        function setupAutoResize() {
            function resizeChart() {
                chart.applyOptions({
                    width: window.innerWidth,
                    height: window.innerHeight
                });
            }
            
            window.addEventListener('resize', resizeChart);
            resizeChart(); // 초기 리사이즈
        }
        
    </script>
</body>
</html>
    """.trimIndent()
}