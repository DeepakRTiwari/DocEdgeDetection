# Document Scanner Library

A clean, modular, camera-agnostic document scanning library for Android.

## Demo

<img src="https://github.com/user-attachments/assets/b0c2b910-a083-4fac-a5c7-ae6f9ea8e020" alt="Scanner Demo" width="20%" height="20%" />

## Features

- 📸 **Camera-Agnostic** - Works with CameraX, Camera2, or any image source
- 🎨 **Customizable** - Configure colors, thresholds, and behavior
- 🔄 **Auto-Capture** - Intelligent auto-capture with stability detection
- ✋ **Manual Capture** - Optional manual capture button
- 🎯 **Smart Overlay** - Automatic sizing and coordinate transformation
- 📦 **Modular** - Clean separation between core logic and UI

## Modules

- `:DocEdgeDetection` – reusable SDK (scanner + overlay).
- `:app` – sample app showing Compose integration and permission flow.

## Setup

1) Add the module dependency

```kotlin
// settings.gradle.kts already includes :DocEdgeDetection
dependencies {
    implementation(project(":DocEdgeDetection"))
    // CameraX + OpenCV already declared in the library; add any app-specific deps here
}
```

2) Ensure minimum SDK 24+ and Camera permission in `AndroidManifest.xml`:

```xml

<uses-permission android:name="android.permission.CAMERA" />
```

Add `WRITE_EXTERNAL_STORAGE` only if targeting API 28 or lower and you persist captures.

3) Initialize OpenCV once (see `MainActivity` in the sample).

## Compose Integration (CameraX)

Drop the scanner directly into a Compose screen. The overlay sizes itself and handles coordinate
mapping.

```kotlin
@Composable
fun ComposeScanner() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    var detectedDoc by remember { mutableStateOf<DetectedDocument?>(null) }
    val config = remember {
        DocumentScannerConfig(
            autoCapture = true,
            strokeColor = android.graphics.Color.BLUE,
            fillAlpha = 0.2f,
            detectionMode = 1
        )
    }

    DocumentScannerOverlay(
        detectedDocument = detectedDoc,
        config = config,
        camera = {
            AndroidView(factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FIT_CENTER
                }
                val executor = ContextCompat.getMainExecutor(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build()
                        .also { it.setSurfaceProvider(previewView.surfaceProvider) }

                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    val analyzer = DocumentScannerAnalyzer(
                        config = config,
                        onDocumentDetected = { doc, _ -> detectedDoc = doc },
                        onDocumentCaptured = { bitmap -> /* save bitmap */ }
                    )
                    analysis.setAnalyzer(executor, analyzer)

                    val selector = CameraSelector.DEFAULT_BACK_CAMERA
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner, selector, preview, analysis
                    )
                }, executor)

                previewView
            }, modifier = Modifier.fillMaxSize())
        }
    )
}
```

Enable manual capture by setting `autoCapture = false` in the config and invoking
`scannerAnalyzer.triggerManualCapture()` (see `ScannerScreen` in the sample).

## Native XML Integration

You can keep your existing XML layouts and wire the scanner in your Activity/Fragment.

`layout/scanner_activity.xml`

```xml

<androidx.camera.view.PreviewView android:id="@+id/previewView" android:layout_width="match_parent"
    android:layout_height="match_parent" />

    <!-- Optional: a custom overlay view where you draw detected polygon -->
<com.yourapp.OverlayView android:id="@+id/overlay" android:layout_width="match_parent"
android:layout_height="match_parent" />
```

`ScannerActivity.kt`

```kotlin
class ScannerActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var overlay: OverlayView
    private var analyzer: DocumentScannerAnalyzer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.scanner_activity)

        previewView = findViewById(R.id.previewView)
        overlay = findViewById(R.id.overlay)

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        val config = DocumentScannerConfig(autoCapture = true)
        val executor = ContextCompat.getMainExecutor(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analyzer = DocumentScannerAnalyzer(
                config = config,
                onDocumentDetected = { doc, _ -> overlay.updatePolygon(doc?.points) },
                onDocumentCaptured = { bitmap -> /* persist bitmap */ }
            )
            analysis.setAnalyzer(executor, analyzer!!)

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis
            )
        }, executor)
    }
}
```

Inside your `OverlayView`, draw the polygon using the detected points. If you prefer to post-process
a captured bitmap instead of drawing live, use `DocumentScanner.drawPolygonOverlay(...)`.

## Quick Start APIs

### 1) Basic CameraX Pipeline

```kotlin
val config = DocumentScannerConfig(
    autoCapture = true,
    strokeColor = android.graphics.Color.BLUE,
    fillAlpha = 0.3f
)

val analyzer = DocumentScannerAnalyzer(
    config = config,
    onDocumentDetected = { doc, _ -> detectedDoc = doc },
    onDocumentCaptured = { bitmap -> saveBitmap(bitmap) }
)
```

Wrap your camera preview with `DocumentScannerOverlay` in Compose, or draw polygons on a custom view
in XML.

### 2) Standalone (bitmap/matrix only)

```kotlin
val scanner = DocumentScanner(config)

val detected = scanner.processFrame(imageMat)
val cropped = scanner.cropDocument(imageMat, detected?.points ?: emptyList())
val withOverlay = scanner.drawPolygonOverlay(bitmap, detected?.points ?: emptyList())
```

## API Reference

### DocumentScannerConfig

Configuration for scanner behavior and appearance.

```kotlin
data class DocumentScannerConfig(
    val minContourArea: Double = 3000.0,
    val minFrameAreaPercent: Double = 0.12,
    val smoothingAlpha: Float = 0.15f,
    val requiredStableFrames: Int = 20,
    val postCaptureCooldownMs: Long = 2500L,
    val minPolygonDistance: Float = 50f,
    val autoCapture: Boolean = true,
    val strokeColor: Int = Color.Blue.toArgb(),
    val fillAlpha: Float = 0.3f
)
```

### DetectedDocument

Data model representing a detected document.

```kotlin
data class DetectedDocument(
    val points: List<Point>,      // 4 corner points
    val frameWidth: Int,
    val frameHeight: Int,
    val confidence: Float = 1.0f,
    val timestamp: Long
)
```

### DocumentScanner

Main API class for document scanning.

```kotlin
class DocumentScanner(config: DocumentScannerConfig) {
    fun processFrame(image: Mat): DetectedDocument?
    fun processFrameSmooth(image: Mat): DetectedDocument?
    fun cropDocument(image: Mat, points: List<Point>): Bitmap?
    fun drawPolygonOverlay(bitmap: Bitmap, points: List<Point>): Bitmap
}
```

### DocumentScannerAnalyzer

CameraX ImageAnalysis.Analyzer implementation.

```kotlin
class DocumentScannerAnalyzer(
    config: DocumentScannerConfig,
    onDocumentDetected: (DetectedDocument?, Bitmap) -> Unit,
    onDocumentCaptured: ((Bitmap) -> Unit)? = null
) : ImageAnalysis.Analyzer
```

### DocumentScannerOverlay

Smart composable wrapper with automatic sizing.

```kotlin
@Composable
fun DocumentScannerOverlay(
    detectedDocument: DetectedDocument?,
    modifier: Modifier = Modifier,
    config: DocumentScannerConfig = DocumentScannerConfig(),
    camera: @Composable () -> Unit,
    customOverlay: (@Composable (DetectedDocument?, Float, Float) -> Unit)? = null
)
```

## Customization

### Custom Colors

```kotlin
val config = DocumentScannerConfig(
    strokeColor = android.graphics.Color.GREEN,
    fillAlpha = 0.2f  // Fill color auto-generated from stroke
)
```

### Manual Capture Mode

```kotlin
val config = DocumentScannerConfig(autoCapture = false)

// Show manual capture button
if (!config.autoCapture) {
    CaptureButton(
        enabled = detectedDocument != null,
        onClick = { /* trigger capture */ }
    )
}
```

### Custom Overlay Rendering

```kotlin
DocumentScannerOverlay(
    detectedDocument = doc,
    camera = { /* camera preview */ },
    customOverlay = { doc, width, height ->
        // Your custom rendering logic
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw custom overlay
        }
    }
)
```

## Benefits

✅ **No Size Management** - BoxWithConstraints handled internally  
✅ **No Coordinate Math** - Automatic transformations  
✅ **Camera-Agnostic** - Works with any image source  
✅ **Clean API** - Intuitive and well-documented  
✅ **Reusable** - Drop into any project

## See Also

- `app/src/main/java/com/ml/doc_edge_detection_sample/sample/ScannerScreen.kt` – Compose integration
  with permissions and manual capture
- `DocEdgeDetection/src/main/java/com/ml/doc/scanner/DocumentScanner.kt` – Core scanner
- `DocEdgeDetection/src/main/java/com/ml/doc/scanner/DocumentScannerAnalyzer.kt` – CameraX analyzer
  wiring
- `DocEdgeDetection/src/main/java/com/ml/doc/scanner/utils/DocumentScannerOverlay.kt` – Compose
  overlay helper
