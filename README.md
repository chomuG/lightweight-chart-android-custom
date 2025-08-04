# Lightweight Charts for Android Jetpack Compose

TradingView Lightweight Charts를 Android Jetpack Compose에서 사용할 수 있도록 구현한 프로젝트입니다.

## 📊 특징

- **완전한 WebView 통합**: TradingView Lightweight Charts의 모든 기능을 Android에서 사용
- **Jetpack Compose 네이티브**: Modern Android UI 툴킷과 완벽 호환
- **다양한 차트 타입**: Line, Area, Candlestick, Bar, Histogram, Baseline 지원
- **실시간 데이터**: 동적 데이터 업데이트 및 실시간 차트 구현
- **터치 상호작용**: 클릭, 크로스헤어, 줌 등 모든 상호작용 지원
- **테마 지원**: 라이트/다크 테마 자동 전환
- **고성능**: 네이티브 성능과 부드러운 애니메이션

## 🏗️ 아키텍처

### 핵심 컴포넌트

1. **LightweightChart.kt** - 메인 Compose 컴포넌트
2. **ChartViewModel.kt** - 상태 관리 및 비즈니스 로직
3. **ChartDataProvider.kt** - 샘플 데이터 및 실시간 데이터 제공
4. **MainActivity.kt** - 데모 화면 구현

### 데이터 구조

```kotlin
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
```

## 🚀 사용법

### 기본 사용법

```kotlin
@Composable
fun MyChart() {
    val sampleData = listOf(
        ChartData("2024-01-01", 100.0),
        ChartData("2024-01-02", 120.0),
        ChartData("2024-01-03", 110.0)
    )
    
    LightweightChart(
        data = sampleData,
        seriesType = SeriesType.LINE,
        modifier = Modifier.fillMaxSize(),
        onChartReady = { 
            println("Chart is ready!") 
        },
        onDataPointClick = { time, value ->
            println("Clicked: $time = $value")
        }
    )
}
```

### 고급 사용법 (ViewModel과 함께)

```kotlin
@Composable
fun AdvancedChart(
    viewModel: ChartViewModel = viewModel()
) {
    val chartData by viewModel.lineData.collectAsState()
    val chartOptions by viewModel.chartOptions.collectAsState()
    val seriesType by viewModel.seriesType.collectAsState()
    
    LightweightChart(
        data = chartData,
        seriesType = seriesType,
        options = chartOptions,
        modifier = Modifier.fillMaxSize(),
        onChartReady = { viewModel.onChartReady() },
        onDataPointClick = { time, value ->
            viewModel.onDataPointClick(time, value)
        },
        onCrosshairMove = { time, value ->
            viewModel.onCrosshairMove(time, value)
        }
    )
}
```

## 📈 지원되는 차트 타입

### SeriesType 열거형

- **LINE**: 기본 선 차트
- **AREA**: 영역 차트 (색상 채움)
- **CANDLESTICK**: 캔들스틱 차트 (OHLC 데이터)
- **BAR**: 바 차트 (OHLC 데이터)
- **HISTOGRAM**: 히스토그램
- **BASELINE**: 베이스라인 차트

### 차트 옵션 설정

```kotlin
val customOptions = ChartOptions(
    width = 800,
    height = 400,
    layout = LayoutOptions(
        backgroundColor = "#1E1E1E",
        textColor = "#FFFFFF"
    ),
    priceScale = PriceScaleOptions(
        position = "right",
        autoScale = true
    ),
    timeScale = TimeScaleOptions(
        timeVisible = true,
        borderVisible = true
    )
)
```

## 🔄 실시간 데이터

```kotlin
// ViewModel에서
fun startRealTimeUpdates() {
    viewModelScope.launch {
        ChartDataProvider.generateRealTimeData(100.0)
            .collect { newData ->
                val currentData = _lineData.value.toMutableList()
                currentData.add(newData)
                
                // 최근 50개 포인트만 유지
                if (currentData.size > 50) {
                    currentData.removeAt(0)
                }
                
                _lineData.value = currentData
            }
    }
}
```

## 🎨 테마 및 스타일링

### 다크/라이트 테마 전환

```kotlin
fun toggleTheme() {
    val currentOptions = _chartOptions.value
    val isDark = currentOptions.layout.backgroundColor == "#1E1E1E"
    
    val newLayout = if (isDark) {
        LayoutOptions(
            backgroundColor = "#FFFFFF",
            textColor = "#333333"
        )
    } else {
        LayoutOptions(
            backgroundColor = "#1E1E1E",
            textColor = "#FFFFFF"
        )
    }
    
    _chartOptions.value = currentOptions.copy(layout = newLayout)
}
```

## 📱 상호작용 처리

### 이벤트 리스너

```kotlin
sealed class ChartEvent {
    data object ChartReady : ChartEvent()
    data class DataPointClicked(val time: String, val value: Double) : ChartEvent()
    data class CrosshairMoved(val time: String?, val value: Double?) : ChartEvent()
}
```

### JavaScript 인터페이스

WebView와 Android 간의 통신을 위한 JavaScript 인터페이스:

```kotlin
class ChartJavaScriptInterface(
    private val onDataPointClick: ((String, Double) -> Unit)?,
    private val onCrosshairMove: ((String?, Double?) -> Unit)?
) {
    @JavascriptInterface
    fun onDataPointClicked(time: String, value: Double) {
        onDataPointClick?.invoke(time, value)
    }
    
    @JavascriptInterface
    fun onCrosshairMoved(time: String?, value: Double?) {
        onCrosshairMove?.invoke(time, value)
    }
}
```

## 📦 의존성

### build.gradle.kts (Module: app)

```kotlin
dependencies {
    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    
    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
}
```

### libs.versions.toml

```toml
[versions]
compose-bom = "2024.12.01"
compose-activity = "1.10.1"
lifecycle-viewmodel-compose = "2.8.7"

[libraries]
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "compose-activity" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle-viewmodel-compose" }
```

## 🛠️ 빌드 및 실행

1. **Android Studio에서 프로젝트 열기**
2. **Gradle 동기화 대기**
3. **에뮬레이터 또는 실제 기기에서 실행**

### 최소 요구사항

- Android API 24+ (Android 7.0)
- Kotlin 2.0.21+
- Jetpack Compose BOM 2024.12.01+

## 🌟 주요 기능 데모

앱을 실행하면 다음 기능들을 체험할 수 있습니다:

1. **데이터 소스 전환**: Sample, Crypto, Stock, Volume, Real-time
2. **차트 타입 변경**: 6가지 차트 타입 실시간 전환
3. **테마 토글**: 다크/라이트 모드 전환
4. **상호작용**: 데이터 포인트 클릭, 크로스헤어 이동
5. **동적 데이터**: 실시간 데이터 추가 및 업데이트

## 📄 라이선스

이 프로젝트는 Apache 2.0 라이선스를 따릅니다. TradingView Lightweight Charts는 TradingView, Inc.의 제품이며 해당 라이선스를 준수합니다.

## 🤝 기여

버그 리포트, 기능 요청, 풀 리퀘스트를 환영합니다!

## 📞 지원

문제가 있으시면 GitHub Issues를 통해 문의해 주세요.