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

/**
 * Data classes for chart configuration and data
 */
@Serializable
data class ChartData(
    val time: String,
    val value: Double
)

@Serializable
data class CandlestickData(
    val time: String,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double
)

@Serializable
data class ChartOptions(
    val width: Int = 600,
    val height: Int = 300,
    val layout: LayoutOptions = LayoutOptions(),
    val grid: GridOptions = GridOptions(),
    val priceScale: PriceScaleOptions = PriceScaleOptions(),
    val timeScale: TimeScaleOptions = TimeScaleOptions()
)

@Serializable
data class LayoutOptions(
    val backgroundColor: String = "#FFFFFF",
    val textColor: String = "#333333",
    val fontSize: Int = 12,
    val fontFamily: String = "Trebuchet MS, Roboto, Ubuntu, sans-serif"
)

@Serializable
data class GridOptions(
    val vertLines: LineOptions = LineOptions(),
    val horzLines: LineOptions = LineOptions()
)

@Serializable
data class LineOptions(
    val color: String = "#E6E6E6",
    val visible: Boolean = true
)

@Serializable
data class PriceScaleOptions(
    val position: String = "right", // "left", "right", "none"
    val mode: Int = 0, // 0: Normal, 1: Logarithmic, 2: Percentage, 3: Indexed to 100
    val autoScale: Boolean = true,
    val borderVisible: Boolean = true
)

@Serializable
data class TimeScaleOptions(
    val timeVisible: Boolean = true,
    val secondsVisible: Boolean = false,
    val borderVisible: Boolean = true,
    val rightOffset: Int = 0,
    val barSpacing: Int = 6
)

enum class SeriesType {
    LINE, AREA, CANDLESTICK, BAR, HISTOGRAM, BASELINE
}

/**
 * A Jetpack Compose component that renders TradingView Lightweight Charts
 * using WebView integration.
 */
@Composable
fun LightweightChart(
    data: List<ChartData> = emptyList(),
    candlestickData: List<CandlestickData> = emptyList(),
    seriesType: SeriesType = SeriesType.LINE,
    options: ChartOptions = ChartOptions(),
    modifier: Modifier = Modifier,
    drawingMode: Boolean = false,
    onChartReady: (() -> Unit)? = null,
    onDataPointClick: ((String, Double) -> Unit)? = null,
    onCrosshairMove: ((String?, Double?) -> Unit)? = null,
    onLineToolCreated: ((String, String) -> Unit)? = null
) {
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    
    // Generate HTML content with embedded JavaScript
    val htmlContent = remember(data, candlestickData, seriesType, options, drawingMode) {
        generateChartHtml(data, candlestickData, seriesType, options, drawingMode)
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
            
            // Add JavaScript interface for callbacks using reflection (safer approach)
            try {
                val method = webView.javaClass.getMethod(
                    "addJavascriptInterface", 
                    Any::class.java, 
                    String::class.java
                )
                method.invoke(
                    webView,
                    ChartJavaScriptInterface(
                        onDataPointClick = onDataPointClick,
                        onCrosshairMove = onCrosshairMove,
                        onLineToolCreated = onLineToolCreated
                    ),
                    "Android"
                )
            } catch (e: Exception) {
                // Fallback: WebView without JavaScript interface
                // Chart will still work but without click events
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
 * JavaScript interface for handling chart events
 */
class ChartJavaScriptInterface(
    private val onDataPointClick: ((String, Double) -> Unit)?,
    private val onCrosshairMove: ((String?, Double?) -> Unit)?,
    private val onLineToolCreated: ((String, String) -> Unit)?
) {
    @JavascriptInterface
    fun onDataPointClicked(time: String, value: Double) {
        onDataPointClick?.invoke(time, value)
    }
    
    @JavascriptInterface
    fun onCrosshairMoved(time: String?, value: Double?) {
        onCrosshairMove?.invoke(time, value)
    }
    
    @JavascriptInterface
    fun onLineToolCreated(toolType: String, points: String) {
        onLineToolCreated?.invoke(toolType, points)
    }
    
    @JavascriptInterface
    fun onError(error: String) {
        println("Chart error: $error")
    }
}

/**
 * Generates HTML content with TradingView Lightweight Charts
 */
private fun generateChartHtml(
    data: List<ChartData>,
    candlestickData: List<CandlestickData>,
    seriesType: SeriesType,
    options: ChartOptions,
    drawingMode: Boolean = false
): String {
    val json = Json { ignoreUnknownKeys = true }
    
    val dataJson = when (seriesType) {
        SeriesType.CANDLESTICK -> json.encodeToString(candlestickData)
        else -> json.encodeToString(data)
    }
    
    val optionsJson = json.encodeToString(options)
    val seriesTypeString = seriesType.name.lowercase()
    
    // Base64로 인코딩하여 안전하게 전달
    val optionsBase64 = android.util.Base64.encodeToString(optionsJson.toByteArray(), android.util.Base64.NO_WRAP)
    val dataBase64 = android.util.Base64.encodeToString(dataJson.toByteArray(), android.util.Base64.NO_WRAP)
    
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
            position: relative;
            transition: all 0.2s ease;
        }
        ${if (drawingMode) """
        #chart-container.drawing-mode {
            cursor: crosshair;
            background: rgba(74, 144, 226, 0.02);
            border: 2px dashed rgba(74, 144, 226, 0.3);
            border-radius: 4px;
        }
        
        /* Custom crosshair cursor overlay */
        .crosshair-overlay {
            position: absolute;
            width: 21px;
            height: 21px;
            pointer-events: none;
            z-index: 1000;
            opacity: 0;
            transition: opacity 0.2s ease;
        }
        .crosshair-overlay.active {
            opacity: 1;
        }
        .crosshair-overlay::before,
        .crosshair-overlay::after {
            content: '';
            position: absolute;
            background: #4a90e2;
            box-shadow: 0 0 3px rgba(0,0,0,0.5);
        }
        .crosshair-overlay::before {
            width: 21px;
            height: 1px;
            top: 10px;
            left: 0;
        }
        .crosshair-overlay::after {
            width: 1px;
            height: 21px;
            top: 0;
            left: 10px;
        }
        .crosshair-center {
            position: absolute;
            width: 3px;
            height: 3px;
            background: #4a90e2;
            border: 1px solid white;
            border-radius: 50%;
            top: 9px;
            left: 9px;
            box-shadow: 0 0 3px rgba(0,0,0,0.3);
        }
        
        .drawing-indicator {
            position: absolute;
            top: 10px;
            right: 10px;
            background: rgba(74, 144, 226, 0.9);
            color: white;
            padding: 6px 12px;
            border-radius: 4px;
            font-size: 12px;
            font-weight: bold;
            z-index: 1000;
            opacity: 0;
            transform: translateY(-10px);
            transition: all 0.3s ease;
        }
        .drawing-indicator.show {
            opacity: 1;
            transform: translateY(0);
        }
        """ else ""}
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
    <div id="loading">Loading chart...</div>
    <div id="chart-container" style="display: none;">
        ${if (drawingMode) """
        <div id="drawingIndicator" class="drawing-indicator">터치하여 추세선 그리기</div>
        <div id="crosshairOverlay" class="crosshair-overlay">
            <div class="crosshair-center"></div>
        </div>
        """ else ""}
    </div>
    
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
        
        // TradingView Lightweight Charts 라이브러리 로드 (CDN)
        loadScript('https://unpkg.com/lightweight-charts/dist/lightweight-charts.standalone.production.js', function() {
            initChart();
        });
        
        function initChart() {
            try {
                // 로딩 화면 숨기고 차트 컨테이너 표시
                document.getElementById('loading').style.display = 'none';
                document.getElementById('chart-container').style.display = 'block';
                
                // Base64에서 디코딩 후 JSON 파싱
                const optionsJson = decodeBase64('$optionsBase64');
                const dataJson = decodeBase64('$dataBase64');
                
                console.log('Decoded optionsJson:', optionsJson);
                console.log('Decoded dataJson:', dataJson);
                
                const chartOptions = JSON.parse(optionsJson);
                const chartData = JSON.parse(dataJson);
                
                console.log('Parsed chartOptions:', chartOptions);
                console.log('Parsed chartData:', chartData);
                
                // Create chart with safe property access
                const chart = LightweightCharts.createChart(
                    document.getElementById('chart-container'),
                    {
                        width: chartOptions.width || 600,
                        height: chartOptions.height || 300,
                        layout: {
                            backgroundColor: (chartOptions.layout && chartOptions.layout.backgroundColor) || '#FFFFFF',
                            textColor: (chartOptions.layout && chartOptions.layout.textColor) || '#333333',
                            fontSize: (chartOptions.layout && chartOptions.layout.fontSize) || 12,
                            fontFamily: (chartOptions.layout && chartOptions.layout.fontFamily) || 'Trebuchet MS, Roboto, Ubuntu, sans-serif'
                        },
                        grid: {
                            vertLines: {
                                color: (chartOptions.grid && chartOptions.grid.vertLines && chartOptions.grid.vertLines.color) || '#E6E6E6',
                                visible: (chartOptions.grid && chartOptions.grid.vertLines && chartOptions.grid.vertLines.visible !== undefined) ? chartOptions.grid.vertLines.visible : true
                            },
                            horzLines: {
                                color: (chartOptions.grid && chartOptions.grid.horzLines && chartOptions.grid.horzLines.color) || '#E6E6E6',
                                visible: (chartOptions.grid && chartOptions.grid.horzLines && chartOptions.grid.horzLines.visible !== undefined) ? chartOptions.grid.horzLines.visible : true
                            }
                        },
                        priceScale: {
                            position: (chartOptions.priceScale && chartOptions.priceScale.position) || 'right',
                            mode: (chartOptions.priceScale && chartOptions.priceScale.mode !== undefined) ? chartOptions.priceScale.mode : 0,
                            autoScale: (chartOptions.priceScale && chartOptions.priceScale.autoScale !== undefined) ? chartOptions.priceScale.autoScale : true,
                            borderVisible: (chartOptions.priceScale && chartOptions.priceScale.borderVisible !== undefined) ? chartOptions.priceScale.borderVisible : true
                        },
                        timeScale: {
                            timeVisible: (chartOptions.timeScale && chartOptions.timeScale.timeVisible !== undefined) ? chartOptions.timeScale.timeVisible : true,
                            secondsVisible: (chartOptions.timeScale && chartOptions.timeScale.secondsVisible !== undefined) ? chartOptions.timeScale.secondsVisible : false,
                            borderVisible: (chartOptions.timeScale && chartOptions.timeScale.borderVisible !== undefined) ? chartOptions.timeScale.borderVisible : true,
                            rightOffset: (chartOptions.timeScale && chartOptions.timeScale.rightOffset !== undefined) ? chartOptions.timeScale.rightOffset : 0,
                            barSpacing: (chartOptions.timeScale && chartOptions.timeScale.barSpacing !== undefined) ? chartOptions.timeScale.barSpacing : 6
                        },
                        handleScroll: {
                            mouseWheel: ${!drawingMode},
                            pressedMouseMove: ${!drawingMode},
                            horzTouchDrag: ${!drawingMode},
                            vertTouchDrag: ${!drawingMode}
                        },
                        handleScale: {
                            mouseWheel: ${!drawingMode},
                            pinch: ${!drawingMode},
                            axisPressedMouseMove: ${!drawingMode},
                            axisDoubleClickReset: ${!drawingMode}
                        }
            }
        );
        
        // Create appropriate series based on type
        let series;
        switch ('$seriesTypeString') {
            case 'line':
                series = chart.addSeries(LightweightCharts.LineSeries, {
                    color: '#2962FF',
                    lineWidth: 2
                });
                break;
            case 'area':
                series = chart.addSeries(LightweightCharts.AreaSeries, {
                    lineColor: '#2962FF',
                    topColor: '#2962FF',
                    bottomColor: 'rgba(41, 98, 255, 0.28)'
                });
                break;
            case 'candlestick':
                series = chart.addSeries(LightweightCharts.CandlestickSeries, {
                    upColor: '#26a69a',
                    downColor: '#ef5350',
                    borderVisible: false,
                    wickUpColor: '#26a69a',
                    wickDownColor: '#ef5350'
                });
                break;
            case 'bar':
                series = chart.addSeries(LightweightCharts.BarSeries, {
                    upColor: '#26a69a',
                    downColor: '#ef5350'
                });
                break;
            case 'histogram':
                series = chart.addSeries(LightweightCharts.HistogramSeries, {
                    color: '#26a69a'
                });
                break;
            case 'baseline':
                series = chart.addSeries(LightweightCharts.BaselineSeries, {
                    baseValue: { type: 'price', price: 0 },
                    topLineColor: 'rgba( 38, 166, 154, 1)',
                    topFillColor1: 'rgba( 38, 166, 154, 0.28)',
                    topFillColor2: 'rgba( 38, 166, 154, 0.05)',
                    bottomLineColor: 'rgba( 239, 83, 80, 1)',
                    bottomFillColor1: 'rgba( 239, 83, 80, 0.28)',
                    bottomFillColor2: 'rgba( 239, 83, 80, 0.05)'
                });
                break;
            default:
                series = chart.addSeries(LightweightCharts.LineSeries);
        }
        
        // Set data
        series.setData(chartData);
        
        // Handle chart events using v4+ API
        chart.subscribeCrosshairMove(param => {
            try {
                // Interface 동적 감지
                let androidInterface = null;
                for (let prop in window) {
                    if (prop.startsWith('ChartBridge_') && window[prop].getInterfaceName) {
                        androidInterface = window[prop];
                        break;
                    }
                }
                
                if (!androidInterface && typeof Android !== 'undefined') {
                    androidInterface = Android;
                }
                
                if (androidInterface && androidInterface.onCrosshairMoved) {
                    const time = param.time ? param.time.toString() : null;
                    const value = param.seriesData && param.seriesData.get(series) 
                        ? param.seriesData.get(series) 
                        : null;
                    androidInterface.onCrosshairMoved(time, value);
                }
            } catch (e) {
                console.error('Crosshair event error:', e);
            }
        });
        
        chart.subscribeClick(param => {
            try {
                // Interface 동적 감지
                let androidInterface = null;
                for (let prop in window) {
                    if (prop.startsWith('ChartBridge_') && window[prop].getInterfaceName) {
                        androidInterface = window[prop];
                        break;
                    }
                }
                
                if (!androidInterface && typeof Android !== 'undefined') {
                    androidInterface = Android;
                }
                
                if (androidInterface && androidInterface.onDataPointClicked && param.time && param.seriesData) {
                    const value = param.seriesData.get(series);
                    if (value !== undefined) {
                        androidInterface.onDataPointClicked(param.time.toString(), value);
                    }
                }
            } catch (e) {
                console.error('Click event error:', e);
            }
        });
        
        // Auto-resize chart
        function resizeChart() {
            chart.applyOptions({
                width: window.innerWidth,
                height: window.innerHeight
            });
        }
        
        window.addEventListener('resize', resizeChart);
        
        // Initial resize
        resizeChart();
        
        ${if (drawingMode) """
        // Crosshair drawing system for trend lines
        let crosshairState = {
            visible: false,
            firstPoint: null,
            mode: 'waiting' // 'waiting', 'placing-first', 'placing-second'
        };
        
        const container = document.getElementById('chart-container');
        const crosshair = document.getElementById('crosshairOverlay');
        const indicator = document.getElementById('drawingIndicator');
        
        // Enable drawing mode
        container.classList.add('drawing-mode');
        indicator.classList.add('show');
        
        // Handle chart interactions
        container.addEventListener('click', (event) => {
            const rect = container.getBoundingClientRect();
            const point = {
                x: event.clientX - rect.left,
                y: event.clientY - rect.top
            };
            
            handleChartInteraction(point);
            event.preventDefault();
        });
        
        // Prevent default touch behaviors
        container.addEventListener('touchstart', (event) => {
            event.preventDefault();
            event.stopPropagation();
        });
        
        // Handle touch interactions
        container.addEventListener('touchend', (event) => {
            if (event.touches.length > 0) return;
            
            const touch = event.changedTouches[0];
            const rect = container.getBoundingClientRect();
            const point = {
                x: touch.clientX - rect.left,
                y: touch.clientY - rect.top
            };
            
            handleChartInteraction(point);
            event.preventDefault();
            event.stopPropagation();
        });
        
        // Handle mouse movement for crosshair positioning
        container.addEventListener('mousemove', (event) => {
            if (crosshairState.visible) {
                const rect = container.getBoundingClientRect();
                const point = {
                    x: event.clientX - rect.left,
                    y: event.clientY - rect.top
                };
                
                updateCrosshairPosition(point);
            }
        });
        
        // Handle touch movement for crosshair positioning
        container.addEventListener('touchmove', (event) => {
            if (crosshairState.visible && event.touches.length === 1) {
                const touch = event.touches[0];
                const rect = container.getBoundingClientRect();
                const point = {
                    x: touch.clientX - rect.left,
                    y: touch.clientY - rect.top
                };
                
                updateCrosshairPosition(point);
                event.preventDefault();
            }
        });
        
        function handleChartInteraction(point) {
            const timeScale = chart.timeScale();
            
            // Get coordinates with validation
            const time = timeScale.coordinateToTime(point.x);
            const price = series.coordinateToPrice(point.y);
            
            console.log('Chart interaction:', { point, time, price });
            
            // Validate coordinates
            if (time === null || price === null || 
                typeof time === 'undefined' || typeof price === 'undefined' ||
                isNaN(price)) {
                console.warn('Invalid coordinates:', { time, price });
                return;
            }
            
            if (crosshairState.mode === 'waiting') {
                // First touch - show crosshair immediately and set first point
                crosshairState.firstPoint = { time: time, price: price };
                crosshairState.mode = 'placing-second';
                crosshairState.visible = true;
                showCrosshair(point);
                indicator.textContent = '두 번째 점으로 이동하고 터치';
                indicator.style.background = 'rgba(33, 150, 243, 0.9)';
                
                console.log('First point set:', crosshairState.firstPoint);
                
            } else if (crosshairState.mode === 'placing-second') {
                // Second touch - create trend line
                const secondPoint = { time: time, price: price };
                
                console.log('Second point set:', secondPoint);
                
                // Validate both points before creating line
                if (crosshairState.firstPoint && 
                    crosshairState.firstPoint.time !== null && 
                    crosshairState.firstPoint.price !== null &&
                    secondPoint.time !== null && 
                    secondPoint.price !== null) {
                    
                    createTrendLine(crosshairState.firstPoint, secondPoint);
                } else {
                    console.error('Invalid points for trend line creation');
                }
                
                // Reset state regardless
                crosshairState.mode = 'waiting';
                crosshairState.firstPoint = null;
                hideCrosshair();
                
                indicator.textContent = '추세선 완료! ✓';
                indicator.style.background = 'rgba(76, 175, 80, 0.9)';
                
                setTimeout(() => {
                    indicator.textContent = '터치하여 추세선 그리기';
                    indicator.style.background = 'rgba(74, 144, 226, 0.9)';
                }, 1500);
            }
        }
        
        function showCrosshair(point) {
            crosshair.style.left = (point.x - 10) + 'px';
            crosshair.style.top = (point.y - 10) + 'px';
            crosshair.classList.add('active');
            crosshairState.visible = true;
        }
        
        function hideCrosshair() {
            crosshair.classList.remove('active');
            crosshairState.visible = false;
        }
        
        function updateCrosshairPosition(point) {
            if (!crosshairState.visible) return;
            
            crosshair.style.left = (point.x - 10) + 'px';
            crosshair.style.top = (point.y - 10) + 'px';
        }
        
        function createTrendLine(firstPoint, secondPoint) {
            try {
                // Validate points
                if (!firstPoint || !secondPoint || 
                    firstPoint.time === null || firstPoint.price === null ||
                    secondPoint.time === null || secondPoint.price === null) {
                    console.error('Invalid points for trend line:', firstPoint, secondPoint);
                    return;
                }
                
                // Convert times to proper format
                const time1 = typeof firstPoint.time === 'number' ? firstPoint.time : Math.floor(firstPoint.time);
                const time2 = typeof secondPoint.time === 'number' ? secondPoint.time : Math.floor(secondPoint.time);
                
                // Validate converted times
                if (!time1 || !time2 || time1 === time2) {
                    console.error('Invalid time values:', time1, time2);
                    return;
                }
                
                // Add a simple line series to represent the trend line
                const trendLineSeries = chart.addLineSeries({
                    color: '#4a90e2',
                    lineWidth: 2,
                    lineStyle: 0 // solid line
                });
                
                const lineData = [
                    { time: time1, value: firstPoint.price },
                    { time: time2, value: secondPoint.price }
                ];
                
                trendLineSeries.setData(lineData);
                
                console.log('Trend line created:', lineData);
                
                // Notify Android
                let androidInterface = null;
                for (let prop in window) {
                    if (prop.startsWith('ChartBridge_') && window[prop].getInterfaceName) {
                        androidInterface = window[prop];
                        break;
                    }
                }
                
                if (!androidInterface && typeof Android !== 'undefined') {
                    androidInterface = Android;
                }
                
                if (androidInterface && androidInterface.onLineToolCreated) {
                    androidInterface.onLineToolCreated('TrendLine', JSON.stringify(lineData));
                }
            } catch (error) {
                console.error('Error creating trend line:', error);
                let androidInterface = null;
                for (let prop in window) {
                    if (prop.startsWith('ChartBridge_') && window[prop].getInterfaceName) {
                        androidInterface = window[prop];
                        break;
                    }
                }
                
                if (!androidInterface && typeof Android !== 'undefined') {
                    androidInterface = Android;
                }
                
                if (androidInterface && androidInterface.onError) {
                    androidInterface.onError('Error creating trend line: ' + error.message);
                }
            }
        }
        
        // ESC key to cancel crosshair
        document.addEventListener('keydown', (event) => {
            if (event.key === 'Escape') {
                if (crosshairState.visible) {
                    hideCrosshair();
                    crosshairState.mode = 'waiting';
                    crosshairState.firstPoint = null;
                    
                    indicator.textContent = '터치하여 추세선 그리기';
                    indicator.style.background = 'rgba(74, 144, 226, 0.9)';
                }
                event.preventDefault();
            }
        });
        """ else ""}
        
                // Expose chart globally for potential future interactions
                window.lightweightChart = chart;
                window.chartSeries = series;
                
            } catch (error) {
                console.error('Chart initialization error:', error);
                document.getElementById('loading').innerHTML = 'Chart initialization failed: ' + error.message;
                document.getElementById('loading').style.display = 'block';
                document.getElementById('chart-container').style.display = 'none';
            }
        }
    </script>
</body>
</html>
    """.trimIndent()
}