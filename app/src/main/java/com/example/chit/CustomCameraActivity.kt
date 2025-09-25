package com.example.chit

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.YuvImage
import android.media.Image
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.filter.*
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

class CustomCameraActivity : ComponentActivity() {
    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)

        setContent {
            CameraWithFilterScreen { filteredUri ->
                setResult(RESULT_OK, Intent().apply { data = filteredUri })
                finish()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraWithFilterScreen(onImageCaptured: (Uri) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // GPUImage instance for preview
    val gpuImage = remember { GPUImage(context) }

    // List of filters with names
    val filters = remember {
        listOf(
            FilterInfo("Normal", GPUImageFilter()),
            FilterInfo("Sepia", GPUImageSepiaToneFilter()),
            FilterInfo("Grayscale", GPUImageGrayscaleFilter()),
            FilterInfo("Bright", GPUImageBrightnessFilter(0.3f)),
            FilterInfo("Contrast", GPUImageContrastFilter(1.5f)),
            FilterInfo("Saturation", GPUImageSaturationFilter(2.0f)),
            FilterInfo("Vignette", GPUImageVignetteFilter()),
            FilterInfo("Pixelated", GPUImagePixelationFilter()),
        )
    }

    var currentFilterIndex by remember { mutableStateOf(0) }
    val currentFilterInfo = filters[currentFilterIndex]
    val currentFilter = currentFilterInfo.filter

    // CameraX objects
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var preview by remember { mutableStateOf<Preview?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    Column(modifier = Modifier.fillMaxSize()) {
        // Filter name display
        Text(
            text = currentFilterInfo.name,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(16.dp),
            color = Color.White,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium
        )

        // Camera preview
        Box(modifier = Modifier.weight(1f)) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    startCamera(
                        previewView = previewView,
                        lifecycleOwner = lifecycleOwner,
                        onPreviewReady = { p -> preview = p },
                        onImageCaptureReady = { ic -> imageCapture = ic },
                        filter = currentFilter,
                        gpuImage = gpuImage
                    )
                    previewView
                },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            if (dragAmount > 20) { // Swipe right threshold
                                currentFilterIndex = (currentFilterIndex - 1 + filters.size) % filters.size
                            } else if (dragAmount < -20) { // Swipe left threshold
                                currentFilterIndex = (currentFilterIndex + 1) % filters.size
                            }
                            change.consume()
                        }
                    }
            )

            // Capture button
            FloatingActionButton(
                onClick = {
                    imageCapture?.let { ic ->
                        captureWithFilter(ic, context, currentFilter, onImageCaptured)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Default.Camera, contentDescription = "Capture")
            }
        }
    }
}

data class FilterInfo(val name: String, val filter: GPUImageFilter)

/** Starts CameraX preview with filter applied in real time */
private fun startCamera(
    previewView: PreviewView,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onPreviewReady: (Preview) -> Unit,
    onImageCaptureReady: (ImageCapture) -> Unit,
    filter: GPUImageFilter,
    gpuImage: GPUImage
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(previewView.context)
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().build()
        val imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        // ImageAnalysis for real-time filtering
        val analyzer = ImageAnalysis.Builder()
            .setTargetResolution(Size(640, 480)) // Lower resolution for better performance
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        analyzer.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
            try {
                val bitmap = imageProxyToBitmap(imageProxy)
                if (bitmap != null) {
                    gpuImage.setImage(bitmap)
                    gpuImage.setFilter(filter)
                    // Note: GPUImage doesn't directly modify PreviewView
                    // This analyzer is mainly for processing, not live preview update
                }
            } catch (e: Exception) {
                Log.e("CameraFilter", "Analysis failed: ${e.message}")
            } finally {
                imageProxy.close()
            }
        }

        preview.setSurfaceProvider(previewView.surfaceProvider)

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture,
                analyzer
            )

            onPreviewReady(preview)
            onImageCaptureReady(imageCapture)
        } catch (e: Exception) {
            Log.e("CameraFilter", "Camera binding failed: ${e.message}")
        }
    }, ContextCompat.getMainExecutor(previewView.context))
}

/** Improved ImageProxy to Bitmap conversion */
@androidx.annotation.OptIn(ExperimentalGetImage::class)
private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
    val image = imageProxy.image ?: return null

    when (image.format) {
        ImageFormat.YUV_420_888 -> {
            return yuv420ToBitmap(image)
        }
        ImageFormat.JPEG -> {
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
        else -> return null
    }
}

private fun yuv420ToBitmap(image: Image): Bitmap? {
    val planes = image.planes
    val yPlane = planes[0]
    val uPlane = planes[1]
    val vPlane = planes[2]

    val yBuffer = yPlane.buffer
    val uBuffer = uPlane.buffer
    val vBuffer = vPlane.buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)

    // Copy Y plane
    yBuffer.get(nv21, 0, ySize)

    // Copy UV planes
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)

    val yuvImage = YuvImage(
        nv21,
        ImageFormat.NV21,
        image.width,
        image.height,
        null
    )

    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(android.graphics.Rect(0, 0, image.width, image.height), 100, out)
    val imageBytes = out.toByteArray()
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}

/** Capture a photo with the current filter applied */
private fun captureWithFilter(
    imageCapture: ImageCapture,
    context: Context,
    filter: GPUImageFilter,
    onImageCaptured: (Uri) -> Unit
) {
    val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    val name = "IMG_${sdf.format(Date())}.jpg"

    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, name)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/FilteredCamera")
        }
    }

    val outputOptions = ImageCapture.OutputFileOptions.Builder(
        context.contentResolver,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    ).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onError(exc: ImageCaptureException) {
                Log.e("Camera", "Capture failed: ${exc.message}", exc)
                Toast.makeText(context, "Capture failed", Toast.LENGTH_SHORT).show()
            }

            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val savedUri = output.savedUri
                if (savedUri != null) {
                    applyFilterToUri(savedUri, filter, context, onImageCaptured)
                }
            }
        }
    )
}

/** Apply filter to captured image and save */
private fun applyFilterToUri(
    uri: Uri,
    filter: GPUImageFilter,
    context: Context,
    onImageSaved: (Uri) -> Unit
) {
    try {
        val input: InputStream? = context.contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(input)
        input?.close()

        if (originalBitmap == null) {
            onImageSaved(uri) // Fallback to original
            return
        }

        val gpu = GPUImage(context)
        gpu.setImage(originalBitmap)
        gpu.setFilter(filter)
        val filteredBitmap = gpu.bitmapWithFilterApplied

        // Save filtered bitmap
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val name = "FILTERED_${sdf.format(Date())}.jpg"

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/FilteredCamera")
            }
        }

        val newUri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )

        newUri?.let { uri ->
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                filteredBitmap?.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            }
            Toast.makeText(context, "Photo saved with filter!", Toast.LENGTH_SHORT).show()
            onImageSaved(uri)
        } ?: run {
            onImageSaved(uri) // Fallback to original
        }

        // Recycle bitmaps
        originalBitmap.recycle()
        filteredBitmap?.recycle()

    } catch (e: Exception) {
        Log.e("CameraFilter", "Filter application failed: ${e.message}", e)
        onImageSaved(uri) // Fallback to original
    }
}