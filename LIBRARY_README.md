# Document Scanner Library

A clean, modular, camera-agnostic document scanning library for Android.

## Features

- 📸 **Camera-Agnostic** - Works with CameraX, Camera2, or any image source
- 🎨 **Customizable** - Configure colors, thresholds, and behavior
- 🔄 **Auto-Capture** - Intelligent auto-capture with stability detection
- ✋ **Manual Capture** - Optional manual capture button
- 🎯 **Smart Overlay** - Automatic sizing and coordinate transformation
- 📦 **Modular** - Clean separation between core logic and UI

## Quick Start

### 1. Basic Usage with CameraX

```kotlin
// Configure scanner
val config = DocumentScannerConfig(
    autoCapture = true,
    strokeColor = android.graphics.Color.BLUE,
    fillAlpha = 0.3f
)

// Use smart overlay wrapper
DocumentScannerOverlay(
    detectedDocument = detectedDoc,
    config = config,
    camera = {
        AndroidView(factory = { PreviewView(it) })
    }
)

// Set up analyzer
val analyzer = DocumentScannerAnalyzer(
    config = config,
    onDocumentDetected = { doc, preview ->
        // Update UI with detected document
        detectedDoc = doc
    },
    onDocumentCaptured = { bitmap ->
        // Save captured document
        saveBitmap(bitmap)
    }
)
```

### 2. Standalone Document Scanner

```kotlin
val scanner = DocumentScanner(config)

// Process frame
val detected = scanner.processFrame(imageMat)

// Crop document
val cropped = scanner.cropDocument(imageMat, detected.points)

// Draw overlay on bitmap (GraphicsOverlay style)
val withOverlay = scanner.drawPolygonOverlay(bitmap, detected.points)
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

## Architecture

```
scanner/
├── DocumentScanner.kt              # Main API
├── DocumentScannerAnalyzer.kt      # CameraX analyzer
├── DocumentScannerConfig.kt        # Configuration
├── models/
│   └── DetectedDocument.kt         # Data model
└── utils/
    ├── DocumentScannerOverlay.kt   # Smart wrapper
    ├── GeometryUtils.kt            # Geometry calculations
    └── ImageUtils.kt               # Image conversions

ui/
└── CaptureButton.kt                # Manual capture button

sample/
└── RefactoredScannerScreen.kt      # Usage example
```

## Benefits

✅ **No Size Management** - BoxWithConstraints handled internally  
✅ **No Coordinate Math** - Automatic transformations  
✅ **Camera-Agnostic** - Works with any image source  
✅ **Clean API** - Intuitive and well-documented  
✅ **Reusable** - Drop into any project  

## See Also

- [RefactoredScannerScreen.kt](sample/RefactoredScannerScreen.kt) - Complete integration example
- [ImageProcessor.kt](processor/ImageProcessor.kt) - Core edge detection algorithm
