package com.ml.doc_edge_detection_sample.sample

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.ml.doc.scanner.DocumentScannerAnalyzer
import com.ml.doc.scanner.DocumentScannerConfig
import com.ml.doc.scanner.models.DetectedDocument
import com.ml.doc.scanner.utils.DocumentScannerOverlay
import com.ml.doc_edge_detection_sample.R
import com.ml.doc_edge_detection_sample.ui.CaptureButton
import com.ml.doc_edge_detection_sample.ui.ImageSheet
import java.io.File
import java.io.FileOutputStream

/**
 * Refactored scanner screen using the new library API.
 * This demonstrates how to integrate the document scanner library.
 */
@Composable
fun ScannerScreen(
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    var detectedDocument by remember { mutableStateOf<DetectedDocument?>(null) }
    var savedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isFlashOn by remember { mutableStateOf(false) }
    var autoCaptureEnabled by remember { mutableStateOf(true) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var shouldOpenSheet by remember { mutableStateOf(false) }
    var imageList by remember { mutableStateOf<List<File>?>(null) }
    var scannerAnalyzer by remember { mutableStateOf<DocumentScannerAnalyzer?>(null) }

    val scannerConfig = remember(autoCaptureEnabled) {
        DocumentScannerConfig(
            autoCapture = autoCaptureEnabled,
            strokeColor = android.graphics.Color.BLUE,
            fillAlpha = 0.2f,
            detectionMode = 1,
            smoothingAlpha = 0.15f
        )
    }


    // Update analyzer config when it changes
    LaunchedEffect(scannerConfig) {
        scannerAnalyzer?.updateConfig(scannerConfig)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        // Camera preview with overlay using smart wrapper
        DocumentScannerOverlay(
            detectedDocument = detectedDocument,
            config = scannerConfig,
            camera = {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        previewView.scaleType = PreviewView.ScaleType.FIT_CENTER
                        val executor = ContextCompat.getMainExecutor(ctx)

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()

                            // Preview
                            val preview = androidx.camera.core.Preview.Builder()
                                .build()
                                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

                            // Image Analyzer with new library
                            val analyzer = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            val documentAnalyzer = DocumentScannerAnalyzer(
                                config = scannerConfig,
                                onDocumentDetected = { doc, _ ->
                                    detectedDocument = doc
                                },
                                onDocumentCaptured = { bitmap ->
                                    // Save captured document
                                    saveCapturedDocument(context, bitmap)
                                    savedBitmap = bitmap
                                }
                            )

                            // Set analyzer and update state reference
                            analyzer.setAnalyzer(executor, documentAnalyzer)
                            scannerAnalyzer = documentAnalyzer

                            val cameraSelector = CameraSelector.Builder()
                                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                                .build()

                            try {
                                cameraProvider.unbindAll()
                                camera = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    analyzer
                                )
                            } catch (exc: Exception) {
                                exc.printStackTrace()
                            }
                        }, executor)

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        )

        // Top bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .height(100.dp)
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Document Scanner",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp
                )

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = if (autoCaptureEnabled) "Auto" else "Manual",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    Switch(
                        checked = autoCaptureEnabled,
                        onCheckedChange = { autoCaptureEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color.Green,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color.Gray
                        ),
                        modifier = Modifier.size(width = 40.dp, height = 24.dp)
                    )
                }

                IconButton(
                    onClick = {
                        camera?.let {
                            if (it.cameraInfo.hasFlashUnit()) {
                                val newState = it.cameraInfo.torchState.value == 0
                                it.cameraControl.enableTorch(newState)
                                isFlashOn = newState
                            }
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Image(
                        painter = painterResource(
                            id = if (isFlashOn) R.drawable.flash else R.drawable.flash_off
                        ),
                        contentDescription = "Toggle Flash",
                        colorFilter = ColorFilter.tint(Color.White),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // Captured image preview
        if (savedBitmap != null) {
            Image(
                bitmap = savedBitmap!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .clickable {
                        imageList = fetchCacheFiles(context)
                        shouldOpenSheet = true
                    }
                    .sizeIn(minWidth = 150.dp, minHeight = 200.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .clickable {
                        imageList = fetchCacheFiles(context)
                        shouldOpenSheet = true
                    }
                    .sizeIn(minWidth = 150.dp, minHeight = 200.dp)
                    .background(color = Color.Gray)
                    .align(Alignment.BottomStart)
            )
        }

        // Manual capture button (shown when auto-capture is disabled)
        if (!scannerConfig.autoCapture) {
            CaptureButton(
                enabled = detectedDocument != null,
                onClick = {
                    scannerAnalyzer?.triggerManualCapture()
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            )
        }

        // Image sheet
        ImageSheet(
            imgList = imageList,
            isSheetVisible = shouldOpenSheet,
            hideSheet = { shouldOpenSheet = false }
        )
    }
}

private fun saveCapturedDocument(context: Context, bitmap: Bitmap) {
    val cacheDir = context.cacheDir
    val tempFile = File(cacheDir, "ScannerML_Image")

    if (!tempFile.exists()) {
        tempFile.mkdir()
    }

    val file = File(tempFile.absolutePath, "${System.currentTimeMillis()}.jpg")

    try {
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        outputStream.flush()
        outputStream.close()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun fetchCacheFiles(context: Context): List<File> {
    var list = emptyList<File>()
    context.cacheDir.listFiles()?.forEach {
        if (it.isDirectory) {
            list = it.listFiles()?.toList() ?: emptyList()
        }
    }
    return list
}
