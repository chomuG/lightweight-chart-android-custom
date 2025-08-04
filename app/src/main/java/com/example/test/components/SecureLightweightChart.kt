package com.example.test.components

import android.os.Build
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.JavascriptInterface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 보안이 강화된 Lightweight Chart 컴포넌트
 * 
 * 보안 개선사항:
 * 1. 파일 접근 권한 제한
 * 2. JavaScript Interface 범위 제한
 * 3. 안전한 WebView 설정
 * 4. 로컬 라이브러리 사용 (선택사항)
 */
@Composable
fun SecureLightweightChart(
    data: List<ChartData> = emptyList(),
    candlestickData: List<CandlestickData> = emptyList(),
    seriesType: SeriesType = SeriesType.LINE,
    options: ChartOptions = ChartOptions(),
    modifier: Modifier = Modifier,
    onChartReady: (() -> Unit)? = null,
    onDataPointClick: ((String, Double) -> Unit)? = null,
    onCrosshairMove: ((String?, Double?) -> Unit)? = null,
    useLocalLibrary: Boolean = false // 로컬 라이브러리 사용 여부
) {
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    
    // 보안이 강화된 HTML 콘텐츠 생성
    val htmlContent = remember(data, candlestickData, seriesType, options, useLocalLibrary) {
        generateSecureChartHtml(data, candlestickData, seriesType, options, useLocalLibrary)
    }
    
    AndroidView(
        factory = { context ->
            val webView = WebView(context)
            
            // 보안 강화된 WebView 설정
            webView.configureSecureWebView()
            
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    onChartReady?.invoke()
                }
                
                // URL 로딩 제한 (보안 강화)
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    // data: URL과 about:blank만 허용
                    return when {
                        url?.startsWith("data:") == true -> false
                        url?.startsWith("about:blank") == true -> false
                        else -> true // 다른 URL은 차단
                    }
                }
            }
            
            // 보안이 강화된 JavaScript Interface 추가
            webView.addSecureJavaScriptInterface(
                onDataPointClick = onDataPointClick,
                onCrosshairMove = onCrosshairMove
            )
            
            webView
        },
        update = { view ->
            // data: URL 사용으로 보안 강화
            val dataUrl = "data:text/html;charset=utf-8,${android.util.Base64.encodeToString(
                htmlContent.toByteArray(), 
                android.util.Base64.NO_PADDING
            )}"
            view.loadUrl(dataUrl)
        },
        modifier = modifier.fillMaxSize()
    )
}

/**
 * 보안이 강화된 WebView 설정
 */
private fun WebView.configureSecureWebView() {
    settings.apply {
        // 필수 설정
        javaScriptEnabled = true
        
        // 보안 강화 설정
        allowFileAccess = false              // 파일 접근 차단
        allowContentAccess = false           // Content Provider 접근 차단
        allowFileAccessFromFileURLs = false  // 파일 URL에서 파일 접근 차단
        allowUniversalAccessFromFileURLs = false // 파일 URL에서 범용 접근 차단
        
        // DOM Storage는 차트 기능에 필요하므로 유지
        domStorageEnabled = true
        
        // 추가 보안 설정
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }
        
        // 개인정보 보호 설정
        setGeolocationEnabled(false)
        databaseEnabled = false
        
        // 캐시 설정 (보안상 비활성화)
        cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
        // setAppCacheEnabled는 API 33+에서 deprecated되어 제거
    }
    
    // WebView 디버깅 비활성화 (프로덕션 환경)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        WebView.setWebContentsDebuggingEnabled(false)
    }
}

/**
 * 보안이 강화된 JavaScript Interface 추가
 */
private fun WebView.addSecureJavaScriptInterface(
    onDataPointClick: ((String, Double) -> Unit)?,
    onCrosshairMove: ((String?, Double?) -> Unit)?
) {
    // Interface 이름을 예측하기 어렵게 변경
    val interfaceName = "ChartBridge_${System.currentTimeMillis()}"
    
    // 반사를 사용한 안전한 JavaScript Interface 추가
    try {
        val method = this.javaClass.getMethod(
            "addJavascriptInterface",
            Any::class.java,
            String::class.java
        )
        method.invoke(
            this,
            SecureChartJavaScriptInterface(
                onDataPointClick = onDataPointClick,
                onCrosshairMove = onCrosshairMove,
                interfaceName = interfaceName
            ),
            interfaceName
        )
    } catch (e: Exception) {
        // JavaScript Interface를 사용할 수 없음 - 차트는 동작하지만 상호작용 제한
        e.printStackTrace()
    }
}

/**
 * 보안이 강화된 JavaScript Interface
 */
class SecureChartJavaScriptInterface(
    private val onDataPointClick: ((String, Double) -> Unit)?,
    private val onCrosshairMove: ((String?, Double?) -> Unit)?,
    private val interfaceName: String
) {
    companion object {
        private const val MAX_STRING_LENGTH = 100
        private const val MAX_VALUE = 1_000_000.0
        private const val MIN_VALUE = -1_000_000.0
    }
    
    @JavascriptInterface
    fun onDataPointClicked(time: String, value: Double) {
        // 입력 검증
        if (!isValidInput(time, value)) return
        
        onDataPointClick?.invoke(time, value)
    }
    
    @JavascriptInterface
    fun onCrosshairMoved(time: String?, value: Double?) {
        // 입력 검증
        if (time != null && !isValidTimeString(time)) return
        if (value != null && !isValidValue(value)) return
        
        onCrosshairMove?.invoke(time, value)
    }
    
    @JavascriptInterface
    fun getInterfaceName(): String = interfaceName
    
    /**
     * 입력값 검증
     */
    private fun isValidInput(time: String, value: Double): Boolean {
        return isValidTimeString(time) && isValidValue(value)
    }
    
    private fun isValidTimeString(time: String): Boolean {
        return time.length <= MAX_STRING_LENGTH && 
               time.matches(Regex("^[\\d\\-T:\\.Z\\s]+$")) // 날짜/시간 형식만 허용
    }
    
    private fun isValidValue(value: Double): Boolean {
        return !value.isNaN() && 
               !value.isInfinite() && 
               value in MIN_VALUE..MAX_VALUE
    }
}

/**
 * 보안이 강화된 HTML 생성
 */
private fun generateSecureChartHtml(
    data: List<ChartData>,
    candlestickData: List<CandlestickData>,
    seriesType: SeriesType,
    options: ChartOptions,
    useLocalLibrary: Boolean
): String {
    val json = Json { ignoreUnknownKeys = true }
    
    val dataJson = when (seriesType) {
        SeriesType.CANDLESTICK -> json.encodeToString(candlestickData)
        else -> json.encodeToString(data)
    }
    
    val optionsJson = json.encodeToString(options)
    val seriesTypeString = seriesType.name.lowercase()
    
    // 라이브러리 소스 선택
    val libraryScript = if (useLocalLibrary) {
        // 로컬 라이브러리 사용 (앱 assets에 포함된 파일)
        """<script src="file:///android_asset/lightweight-charts.js"></script>"""
    } else {
        // CDN 사용 (기본값, 하지만 보안상 권장하지 않음)
        """<script src="https://unpkg.com/lightweight-charts/dist/lightweight-charts.standalone.production.js"></script>"""
    }
    
    return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta http-equiv="Content-Security-Policy" content="default-src 'self' 'unsafe-inline' unpkg.com; script-src 'self' 'unsafe-inline' unpkg.com;">
    $libraryScript
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
    </style>
</head>
<body>
    <div id="chart-container"></div>
    
    <script>
        // 전역 변수 오염 방지
        (function() {
            'use strict';
            
            const chartOptions = JSON.parse('$optionsJson');
            const chartData = JSON.parse('$dataJson');
            
            // Interface 동적 감지
            let androidInterface = null;
            for (let prop in window) {
                if (prop.startsWith('ChartBridge_') && window[prop].getInterfaceName) {
                    androidInterface = window[prop];
                    break;
                }
            }
            
            // 안전한 Interface 호출 함수
            function safeCall(methodName, ...args) {
                try {
                    if (androidInterface && typeof androidInterface[methodName] === 'function') {
                        androidInterface[methodName](...args);
                    }
                } catch (e) {
                    console.error('Interface call failed:', e);
                }
            }
            
            // 입력값 검증 함수
            function isValidNumber(value) {
                return typeof value === 'number' && 
                       !isNaN(value) && 
                       isFinite(value) && 
                       value >= -1000000 && 
                       value <= 1000000;
            }
            
            function isValidTimeString(value) {
                return typeof value === 'string' && 
                       value.length <= 100 && 
                       /^[\d\-T:\.\sZ]+$/.test(value);
            }
            
            // 차트 생성 (기존 로직과 동일하지만 보안 강화)
            const chart = LightweightCharts.createChart(
                document.getElementById('chart-container'),
                chartOptions
            );
            
            let series;
            switch ('$seriesTypeString') {
                case 'line':
                    series = chart.addLineSeries({ color: '#2962FF', lineWidth: 2 });
                    break;
                case 'area':
                    series = chart.addAreaSeries({
                        lineColor: '#2962FF',
                        topColor: '#2962FF',
                        bottomColor: 'rgba(41, 98, 255, 0.28)'
                    });
                    break;
                case 'candlestick':
                    series = chart.addCandlestickSeries({
                        upColor: '#26a69a',
                        downColor: '#ef5350',
                        borderVisible: false,
                        wickUpColor: '#26a69a',
                        wickDownColor: '#ef5350'
                    });
                    break;
                case 'bar':
                    series = chart.addBarSeries({
                        upColor: '#26a69a',
                        downColor: '#ef5350'
                    });
                    break;
                case 'histogram':
                    series = chart.addHistogramSeries({ color: '#26a69a' });
                    break;
                case 'baseline':
                    series = chart.addBaselineSeries({
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
                    series = chart.addLineSeries();
            }
            
            series.setData(chartData);
            
            // 보안이 강화된 이벤트 핸들러
            chart.subscribeCrosshairMove(param => {
                const time = param.time ? param.time.toString() : null;
                const value = param.seriesPrices && param.seriesPrices.get(series) 
                    ? param.seriesPrices.get(series) 
                    : null;
                
                // 입력값 검증 후 호출
                if ((time === null || isValidTimeString(time)) && 
                    (value === null || isValidNumber(value))) {
                    safeCall('onCrosshairMoved', time, value);
                }
            });
            
            chart.subscribeClick(param => {
                if (param.time && param.seriesPrices) {
                    const time = param.time.toString();
                    const value = param.seriesPrices.get(series);
                    
                    // 입력값 검증 후 호출
                    if (isValidTimeString(time) && isValidNumber(value)) {
                        safeCall('onDataPointClicked', time, value);
                    }
                }
            });
            
            // 자동 리사이즈
            function resizeChart() {
                chart.applyOptions({
                    width: window.innerWidth,
                    height: window.innerHeight
                });
            }
            
            window.addEventListener('resize', resizeChart);
            resizeChart();
            
        })(); // IIFE 종료
    </script>
</body>
</html>
    """.trimIndent()
}