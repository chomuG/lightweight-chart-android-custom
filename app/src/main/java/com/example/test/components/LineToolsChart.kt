package com.example.test.components

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.test.R

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LineToolsChart(
    modifier: Modifier = Modifier,
    data: List<Map<String, Any>> = emptyList()
) {
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    
    // Sample data for demonstration
    val sampleData = remember {
        listOf(
            mapOf("time" to "2023-12-01", "open" to 100.0, "high" to 105.0, "low" to 95.0, "close" to 102.0),
            mapOf("time" to "2023-12-02", "open" to 102.0, "high" to 108.0, "low" to 98.0, "close" to 106.0),
            mapOf("time" to "2023-12-03", "open" to 106.0, "high" to 110.0, "low" to 104.0, "close" to 108.0),
            mapOf("time" to "2023-12-04", "open" to 108.0, "high" to 112.0, "low" to 105.0, "close" to 109.0),
            mapOf("time" to "2023-12-05", "open" to 109.0, "high" to 115.0, "low" to 107.0, "close" to 113.0)
        )
    }
    
    val backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.surface.toArgb()
    val backgroundColorHex = String.format("#%06X", 0xFFFFFF and backgroundColor)

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                webView = this
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                addJavascriptInterface(LineToolsJavaScriptInterface(), "Android")
                
                val htmlContent = """
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>TradingView Line Tools Chart</title>
    <style>
        body { 
            margin: 0; 
            padding: 10px;
            background-color: $backgroundColorHex;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
        }
        #container { 
            width: 100%; 
            height: 400px; 
            position: relative;
            transition: all 0.2s ease;
        }
        #container.drawing-mode {
            cursor: crosshair;
            background: rgba(74, 144, 226, 0.02);
            border: 2px dashed rgba(74, 144, 226, 0.3);
            border-radius: 4px;
        }
        .drawing-indicator {
            position: absolute;
            top: 10px;
            right: 10px;
            background: rgba(74, 144, 226, 0.9);
            color: white;
            padding: 4px 8px;
            border-radius: 4px;
            font-size: 10px;
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
        .drawing-indicator.dragging {
            background: rgba(255, 152, 0, 0.9) !important;
            animation: pulse 1s infinite;
        }
        @keyframes pulse {
            0% { opacity: 0.9; }
            50% { opacity: 0.7; }
            100% { opacity: 0.9; }
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
        .toolbar {
            display: flex;
            gap: 4px;
            margin-bottom: 10px;
            padding: 8px;
            background: rgba(40, 44, 52, 0.95);
            border-radius: 6px;
            flex-wrap: wrap;
            box-shadow: 0 2px 8px rgba(0,0,0,0.2);
        }
        .tool-btn {
            padding: 8px 12px;
            border: 1px solid #3e4147;
            background: #2a2e39;
            color: #d1d4dc;
            border-radius: 4px;
            cursor: pointer;
            font-size: 11px;
            font-weight: 500;
            transition: all 0.15s ease;
            min-width: 28px;
            text-align: center;
            position: relative;
        }
        .tool-btn:hover {
            background: #363a45;
            border-color: #4a90e2;
            transform: translateY(-1px);
        }
        .tool-btn.active {
            background: #4a90e2;
            color: white;
            border-color: #4a90e2;
            box-shadow: 0 0 8px rgba(74, 144, 226, 0.4);
        }
        .tool-btn.active::after {
            content: '';
            position: absolute;
            bottom: -2px;
            left: 50%;
            transform: translateX(-50%);
            width: 6px;
            height: 2px;
            background: #4a90e2;
            border-radius: 1px;
        }
        .tool-group {
            display: flex;
            gap: 4px;
            align-items: center;
        }
        .tool-group::after {
            content: "";
            width: 1px;
            height: 20px;
            background: #ddd;
            margin-left: 4px;
        }
        .tool-group:last-child::after {
            display: none;
        }
    </style>
</head>
<body>
    <div class="toolbar">
        <div class="tool-group">
            <button class="tool-btn" onclick="setActiveTool('')">✋</button>
        </div>
        <div class="tool-group">
            <button class="tool-btn" onclick="setActiveTool('TrendLine')" title="추세선">📈</button>
            <button class="tool-btn" onclick="setActiveTool('HorizontalLine')" title="수평선">➖</button>
            <button class="tool-btn" onclick="setActiveTool('VerticalLine')" title="수직선">|</button>
            <button class="tool-btn" onclick="setActiveTool('Ray')" title="레이">↗️</button>
        </div>
        <div class="tool-group">
            <button class="tool-btn" onclick="setActiveTool('Rectangle')" title="사각형">⬜</button>
            <button class="tool-btn" onclick="setActiveTool('Circle')" title="원">⭕</button>
            <button class="tool-btn" onclick="setActiveTool('Triangle')" title="삼각형">🔺</button>
        </div>
        <div class="tool-group">
            <button class="tool-btn" onclick="setActiveTool('Text')" title="텍스트">📝</button>
            <button class="tool-btn" onclick="setActiveTool('Callout')" title="콜아웃">💬</button>
        </div>
        <div class="tool-group">
            <button class="tool-btn" onclick="setActiveTool('FibRetracement')" title="피보나치">🌀</button>
            <button class="tool-btn" onclick="setActiveTool('ParallelChannel')" title="채널">📊</button>
        </div>
        <div class="tool-group">
            <button class="tool-btn" onclick="clearAllLineTools()" title="모두 지우기">🗑️</button>
            <button class="tool-btn" onclick="exportLineTools()" title="내보내기">💾</button>
        </div>
    </div>
    <div id="container">
        <div id="drawingIndicator" class="drawing-indicator">Drawing Mode</div>
        <div id="crosshairOverlay" class="crosshair-overlay">
            <div class="crosshair-center"></div>
        </div>
    </div>

    <script src="file:///android_asset/lightweight-charts-line-tools.js"></script>
    <script>
        let chart;
        let series;
        let activeToolType = '';
        
        // Crosshair state management
        let crosshairState = {
            visible: false,
            firstPoint: null,
            mode: 'waiting' // 'waiting', 'placing-first', 'placing-second'
        };
        
        // Initialize chart
        function initChart() {
            try {
                // Use the line tools enhanced chart with better interaction settings
                chart = LightweightCharts.createChart(document.getElementById('container'), {
                    width: document.getElementById('container').clientWidth,
                    height: 400,
                    layout: {
                        background: { type: 'solid', color: '$backgroundColorHex' },
                        textColor: '#333',
                    },
                    grid: {
                        vertLines: { color: '#e1e1e1' },
                        horzLines: { color: '#e1e1e1' },
                    },
                    crosshair: {
                        mode: LightweightCharts.CrosshairMode.Normal,
                    },
                    timeScale: {
                        borderColor: '#D1D4DC',
                        timeVisible: true,
                        secondsVisible: false,
                    },
                    rightPriceScale: {
                        borderColor: '#D1D4DC',
                    },
                    handleScroll: {
                        mouseWheel: true,
                        pressedMouseMove: true,
                        horzTouchDrag: true,
                        vertTouchDrag: true,
                    },
                    handleScale: {
                        axisPressedMouseMove: true,
                        mouseWheel: true,
                        pinch: true,
                    },
                    trackingMode: {
                        exitMode: LightweightCharts.TrackingModeExitMode.OnNextTap
                    }
                });

                // Add candlestick series
                series = chart.addCandlestickSeries({
                    upColor: '#26a69a',
                    downColor: '#ef5350',
                    borderVisible: false,
                    wickUpColor: '#26a69a',
                    wickDownColor: '#ef5350',
                });

                // Set sample data
                const sampleData = ${sampleData.map { 
                    """{ time: '${it["time"]}', open: ${it["open"]}, high: ${it["high"]}, low: ${it["low"]}, close: ${it["close"]} }"""
                }.joinToString(", ", "[", "]")}

                series.setData(sampleData);

                // Handle window resize
                window.addEventListener('resize', () => {
                    chart.resize(document.getElementById('container').clientWidth, 400);
                });

                // Line tools event handlers
                if (chart.subscribeLineToolsDoubleClick) {
                    chart.subscribeLineToolsDoubleClick((params) => {
                        console.log('Line tool double clicked:', params);
                        if (Android && Android.onLineToolDoubleClick) {
                            Android.onLineToolDoubleClick(JSON.stringify(params));
                        }
                    });
                }

                if (chart.subscribeLineToolsAfterEdit) {
                    chart.subscribeLineToolsAfterEdit((params) => {
                        console.log('Line tool after edit:', params);
                        
                        // Show completion feedback
                        const indicator = document.getElementById('drawingIndicator');
                        const originalText = indicator.textContent;
                        indicator.textContent = '완료! ✓';
                        indicator.style.background = 'rgba(76, 175, 80, 0.9)';
                        
                        setTimeout(() => {
                            if (activeToolType) {
                                indicator.textContent = originalText;
                                indicator.style.background = 'rgba(74, 144, 226, 0.9)';
                            }
                        }, 1500);
                        
                        if (Android && Android.onLineToolAfterEdit) {
                            Android.onLineToolAfterEdit(JSON.stringify(params));
                        }
                    });
                }
                
                // Crosshair-based line tool creation system
                const container = document.getElementById('container');
                const crosshair = document.getElementById('crosshairOverlay');
                
                // Handle chart interactions
                container.addEventListener('click', (event) => {
                    if (!activeToolType) return;
                    
                    const rect = container.getBoundingClientRect();
                    const point = {
                        x: event.clientX - rect.left,
                        y: event.clientY - rect.top
                    };
                    
                    handleChartInteraction(point);
                    event.preventDefault();
                });
                
                // Handle touch interactions
                container.addEventListener('touchend', (event) => {
                    if (!activeToolType || event.touches.length > 0) return;
                    
                    const touch = event.changedTouches[0];
                    const rect = container.getBoundingClientRect();
                    const point = {
                        x: touch.clientX - rect.left,
                        y: touch.clientY - rect.top
                    };
                    
                    handleChartInteraction(point);
                    event.preventDefault();
                });
                
                // Handle mouse movement for crosshair positioning
                container.addEventListener('mousemove', (event) => {
                    if (crosshairState.visible && activeToolType) {
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
                    if (crosshairState.visible && activeToolType && event.touches.length === 1) {
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

                console.log('Chart initialized successfully with line tools support');
                if (Android && Android.onChartReady) {
                    Android.onChartReady();
                }
            } catch (error) {
                console.error('Chart initialization failed:', error);
                if (Android && Android.onError) {
                    Android.onError('Chart initialization failed: ' + error.message);
                }
            }
        }

        function setActiveTool(toolType) {
            try {
                // Clear active states
                document.querySelectorAll('.tool-btn').forEach(btn => btn.classList.remove('active'));
                
                const container = document.getElementById('container');
                const indicator = document.getElementById('drawingIndicator');
                
                if (toolType === '') {
                    activeToolType = '';
                    // Disable drawing mode
                    container.classList.remove('drawing-mode');
                    indicator.classList.remove('show');
                    indicator.textContent = 'Drawing Mode';
                    
                    // Hide crosshair and reset state
                    hideCrosshair();
                    
                    // Set select mode cursor
                    container.style.cursor = 'default';
                    event.target.classList.add('active');
                    return;
                }

                // Set active tool
                activeToolType = toolType;
                event.target.classList.add('active');
                
                // Enable drawing mode visual feedback
                container.classList.add('drawing-mode');
                indicator.classList.add('show');
                indicator.textContent = '터치하여 시작: ' + getToolDisplayName(toolType);

                // Reset crosshair state
                crosshairState.mode = 'waiting';
                crosshairState.firstPoint = null;

                console.log('Active tool set to:', toolType);
            } catch (error) {
                console.error('Error setting active tool:', error);
                if (Android && Android.onError) {
                    Android.onError('Error setting active tool: ' + error.message);
                }
            }
        }
        
        function handleChartInteraction(point) {
            const timeScale = chart.timeScale();
            const priceScale = chart.priceScale();
            
            const time = timeScale.coordinateToTime(point.x);
            const price = priceScale.coordinateToPrice(point.y);
            
            if (time === null || price === null) return;
            
            const indicator = document.getElementById('drawingIndicator');
            
            if (crosshairState.mode === 'waiting') {
                // First touch - show crosshair immediately and set first point
                crosshairState.firstPoint = { time: time, price: price };
                crosshairState.mode = 'placing-second';
                crosshairState.visible = true;
                showCrosshair(point);
                indicator.textContent = '두 번째 점으로 크로스헤어를 이동하고 터치';
                indicator.style.background = 'rgba(33, 150, 243, 0.9)';
                
            } else if (crosshairState.mode === 'placing-second') {
                // Third touch - create line tool and finish
                const secondPoint = { time: time, price: price };
                createLineTool(crosshairState.firstPoint, secondPoint);
                
                // Reset state
                crosshairState.mode = 'waiting';
                crosshairState.firstPoint = null;
                hideCrosshair();
                
                indicator.textContent = '완료! ✓';
                indicator.style.background = 'rgba(76, 175, 80, 0.9)';
                
                setTimeout(() => {
                    if (activeToolType) {
                        indicator.textContent = '터치하여 시작: ' + getToolDisplayName(activeToolType);
                        indicator.style.background = 'rgba(74, 144, 226, 0.9)';
                    }
                }, 1500);
            }
        }
        
        function showCrosshair(point) {
            const crosshair = document.getElementById('crosshairOverlay');
            crosshair.style.left = (point.x - 10) + 'px';
            crosshair.style.top = (point.y - 10) + 'px';
            crosshair.classList.add('active');
            crosshairState.visible = true;
        }
        
        function hideCrosshair() {
            const crosshair = document.getElementById('crosshairOverlay');
            crosshair.classList.remove('active');
            crosshairState.visible = false;
        }
        
        function updateCrosshairPosition(point) {
            if (!crosshairState.visible) return;
            
            const crosshair = document.getElementById('crosshairOverlay');
            crosshair.style.left = (point.x - 10) + 'px';
            crosshair.style.top = (point.y - 10) + 'px';
        }
        
        function createLineTool(firstPoint, secondPoint) {
            try {
                const points = [firstPoint, secondPoint];
                const defaultOptions = getDefaultOptionsForTool(activeToolType);
                
                if (chart.addLineTool) {
                    const lineTool = chart.addLineTool(activeToolType, points, defaultOptions);
                    console.log('Line tool created with crosshair:', activeToolType, points);
                } else {
                    console.error('addLineTool method not available');
                }
            } catch (error) {
                console.error('Error creating line tool:', error);
                if (Android && Android.onError) {
                    Android.onError('Error creating line tool: ' + error.message);
                }
            }
        }
        
        function getToolDisplayName(toolType) {
            const names = {
                'TrendLine': '추세선',
                'HorizontalLine': '수평선',
                'VerticalLine': '수직선',
                'Ray': '레이',
                'Rectangle': '사각형',
                'Circle': '원',
                'Triangle': '삼각형',
                'Text': '텍스트',
                'Callout': '콜아웃',
                'FibRetracement': '피보나치',
                'ParallelChannel': '채널'
            };
            return names[toolType] || toolType;
        }

        function getDefaultOptionsForTool(toolType) {
            const commonOptions = {
                visible: true,
                editable: true
            };

            switch (toolType) {
                case 'TrendLine':
                case 'Ray':
                case 'Arrow':
                case 'ExtendedLine':
                    return {
                        ...commonOptions,
                        text: {
                            value: '',
                            font: { color: '#333', size: 12, family: 'Arial', bold: false, italic: false },
                            alignment: 'left',
                            box: { alignment: { vertical: 'top', horizontal: 'left' }, angle: 0, scale: 1 },
                            padding: 4,
                            wordWrapWidth: 200,
                            forceTextAlign: false,
                            forceCalculateMaxLineWidth: false
                        },
                        line: {
                            color: '#2196F3',
                            width: 2,
                            style: 0, // solid
                            extend: { left: false, right: false },
                            end: { left: 0, right: 0 } // normal ends
                        }
                    };

                case 'HorizontalLine':
                case 'HorizontalRay':
                    return {
                        ...commonOptions,
                        text: {
                            value: '',
                            font: { color: '#333', size: 12, family: 'Arial', bold: false, italic: false },
                            alignment: 'left',
                            box: { alignment: { vertical: 'top', horizontal: 'left' }, angle: 0, scale: 1 },
                            padding: 4,
                            wordWrapWidth: 200,
                            forceTextAlign: false,
                            forceCalculateMaxLineWidth: false
                        },
                        line: {
                            color: '#FF9800',
                            width: 1,
                            style: 0,
                            extend: { left: true, right: true }
                        }
                    };

                case 'VerticalLine':
                    return {
                        ...commonOptions,
                        text: {
                            value: '',
                            font: { color: '#333', size: 12, family: 'Arial', bold: false, italic: false },
                            alignment: 'left',
                            box: { alignment: { vertical: 'top', horizontal: 'left' }, angle: 0, scale: 1 },
                            padding: 4,
                            wordWrapWidth: 200,
                            forceTextAlign: false,
                            forceCalculateMaxLineWidth: false
                        },
                        line: {
                            color: '#4CAF50',
                            width: 1,
                            style: 0
                        }
                    };

                case 'Rectangle':
                    return {
                        ...commonOptions,
                        text: {
                            value: '',
                            font: { color: '#333', size: 12, family: 'Arial', bold: false, italic: false },
                            alignment: 'center',
                            box: { alignment: { vertical: 'middle', horizontal: 'center' }, angle: 0, scale: 1 },
                            padding: 4,
                            wordWrapWidth: 200,
                            forceTextAlign: false,
                            forceCalculateMaxLineWidth: false
                        },
                        rectangle: {
                            background: { color: 'rgba(33, 150, 243, 0.1)' },
                            border: { color: '#2196F3', width: 1, style: 0 },
                            extend: { left: false, right: false }
                        }
                    };

                case 'Circle':
                    return {
                        ...commonOptions,
                        text: {
                            value: '',
                            font: { color: '#333', size: 12, family: 'Arial', bold: false, italic: false },
                            alignment: 'center',
                            box: { alignment: { vertical: 'middle', horizontal: 'center' }, angle: 0, scale: 1 },
                            padding: 4,
                            wordWrapWidth: 200,
                            forceTextAlign: false,
                            forceCalculateMaxLineWidth: false
                        },
                        circle: {
                            background: { color: 'rgba(156, 39, 176, 0.1)' },
                            border: { color: '#9C27B0', width: 1, style: 0 },
                            extend: { left: false, right: false }
                        }
                    };

                case 'Triangle':
                    return {
                        ...commonOptions,
                        triangle: {
                            background: { color: 'rgba(255, 152, 0, 0.1)' },
                            border: { color: '#FF9800', width: 1, style: 0 }
                        }
                    };

                case 'Text':
                    return {
                        ...commonOptions,
                        text: {
                            value: 'Text',
                            font: { color: '#333', size: 14, family: 'Arial', bold: false, italic: false },
                            box: { 
                                alignment: { vertical: 'top', horizontal: 'left' }, 
                                angle: 0, 
                                scale: 1,
                                background: { color: 'rgba(255, 255, 255, 0.8)', inflation: { x: 4, y: 2 } },
                                border: { color: '#ddd', width: 1, radius: 2, highlight: false, style: 0 }
                            },
                            padding: 8,
                            wordWrapWidth: 200,
                            forceTextAlign: false,
                            forceCalculateMaxLineWidth: false
                        }
                    };

                case 'Callout':
                    return {
                        ...commonOptions,
                        text: {
                            value: 'Callout',
                            font: { color: '#333', size: 12, family: 'Arial', bold: false, italic: false },
                            alignment: 'left',
                            box: { 
                                alignment: { vertical: 'top', horizontal: 'left' }, 
                                angle: 0, 
                                scale: 1,
                                background: { color: 'rgba(255, 235, 59, 0.9)', inflation: { x: 6, y: 4 } },
                                border: { color: '#FFC107', width: 1, radius: 4, highlight: false, style: 0 }
                            },
                            padding: 8,
                            wordWrapWidth: 150,
                            forceTextAlign: false,
                            forceCalculateMaxLineWidth: false
                        },
                        line: {
                            color: '#FFC107',
                            width: 2,
                            style: 0
                        }
                    };

                case 'FibRetracement':
                    return {
                        ...commonOptions,
                        line: {
                            width: 1,
                            style: 0
                        },
                        extend: { left: false, right: false },
                        levels: [
                            { coeff: 0, color: '#787B86', opacity: 0.25, distanceFromCoeffEnabled: false, distanceFromCoeff: 0 },
                            { coeff: 0.236, color: '#F23645', opacity: 0.25, distanceFromCoeffEnabled: false, distanceFromCoeff: 0.236 },
                            { coeff: 0.382, color: '#FF9800', opacity: 0.25, distanceFromCoeffEnabled: false, distanceFromCoeff: 0.382 },
                            { coeff: 0.5, color: '#4CAF50', opacity: 0.25, distanceFromCoeffEnabled: false, distanceFromCoeff: 0.5 },
                            { coeff: 0.618, color: '#2196F3', opacity: 0.25, distanceFromCoeffEnabled: false, distanceFromCoeff: 0.618 },
                            { coeff: 1, color: '#787B86', opacity: 0.25, distanceFromCoeffEnabled: false, distanceFromCoeff: 1 }
                        ]
                    };

                case 'ParallelChannel':
                    return {
                        ...commonOptions,
                        channelLine: {
                            color: '#9C27B0',
                            width: 1,
                            style: 0
                        },
                        middleLine: {
                            color: '#9C27B0',
                            width: 1,
                            style: 1 // dashed
                        },
                        showMiddleLine: true,
                        extend: { left: false, right: false },
                        background: { color: 'rgba(156, 39, 176, 0.05)' }
                    };

                default:
                    return commonOptions;
            }
        }

        function clearAllLineTools() {
            try {
                if (chart && chart.removeAllLineTools) {
                    chart.removeAllLineTools();
                    console.log('All line tools cleared');
                }
            } catch (error) {
                console.error('Error clearing line tools:', error);
                if (Android && Android.onError) {
                    Android.onError('Error clearing line tools: ' + error.message);
                }
            }
        }

        function exportLineTools() {
            try {
                if (chart && chart.exportLineTools) {
                    const exported = chart.exportLineTools();
                    console.log('Line tools exported:', exported);
                    if (Android && Android.onLineToolsExported) {
                        Android.onLineToolsExported(exported);
                    }
                }
            } catch (error) {
                console.error('Error exporting line tools:', error);
                if (Android && Android.onError) {
                    Android.onError('Error exporting line tools: ' + error.message);
                }
            }
        }

        // Keyboard shortcuts
        document.addEventListener('keydown', (event) => {
            // ESC to return to select mode or cancel crosshair
            if (event.key === 'Escape') {
                if (crosshairState.visible) {
                    // Cancel current crosshair operation
                    hideCrosshair();
                    crosshairState.mode = 'waiting';
                    crosshairState.firstPoint = null;
                    
                    const indicator = document.getElementById('drawingIndicator');
                    if (activeToolType) {
                        indicator.textContent = '터치하여 시작: ' + getToolDisplayName(activeToolType);
                        indicator.style.background = 'rgba(74, 144, 226, 0.9)';
                    }
                } else {
                    // Return to select mode
                    setActiveTool('');
                }
                event.preventDefault();
            }
            
            // Delete key to clear selected line tools
            if (event.key === 'Delete' && chart && chart.removeSelectedLineTools) {
                chart.removeSelectedLineTools();
                event.preventDefault();
            }
            
            // Ctrl+A to clear all line tools
            if (event.ctrlKey && event.key === 'a' && chart && chart.removeAllLineTools) {
                chart.removeAllLineTools();
                event.preventDefault();
            }
        });

        // Initialize when DOM is loaded
        document.addEventListener('DOMContentLoaded', initChart);
        
        // Also initialize immediately if DOM is already loaded
        if (document.readyState !== 'loading') {
            initChart();
        }
    </script>
</body>
</html>
                """.trimIndent()
                
                loadDataWithBaseURL("file:///android_asset/", htmlContent, "text/html", "utf-8", null)
            }
        }
    )
}

class LineToolsJavaScriptInterface {
    @JavascriptInterface
    fun onChartReady() {
        println("Chart is ready")
    }
    
    @JavascriptInterface
    fun onError(error: String) {
        println("Chart error: $error")
    }
    
    @JavascriptInterface
    fun onLineToolDoubleClick(params: String) {
        println("Line tool double clicked: $params")
    }
    
    @JavascriptInterface
    fun onLineToolAfterEdit(params: String) {
        println("Line tool after edit: $params")
    }
    
    @JavascriptInterface
    fun onLineToolsExported(data: String) {
        println("Line tools exported: $data")
    }
}