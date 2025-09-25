package com.example.chit

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.filter.*
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
                // After capture, you get URI of the filtered image
                setResult(RESULT_OK, Intent().apply { data = filteredUri })
                finish()
            }
        }
    }
}

@Composable
fun CameraWithFilterScreen(onImageCaptured: (Uri) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // GPUImage instance
    val gpuImage = remember { GPUImage(context) }

    // List of filters to swipe through
    val filters = remember {
        listOf<GPUImageFilter>(
            GPUImageFilter(),  // no filter
            GPUImageSepiaToneFilter(),
            GPUImageGrayscaleFilter(),
            GPUImageBrightnessFilter(0.5f),
            GPUImageContrastFilter(2.0f)
        )
    }
    var currentFilterIndex by remember { mutableStateOf(0) }
    val currentFilter = filters[currentFilterIndex]

    // CameraX objects
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    Box(modifier = Modifier.fillMaxSize()) {
        // The preview, with gesture to swipe filters
        AndroidView(
            factory = { ctx ->
                val previewView = androidx.camera.view.PreviewView(ctx)
                startCamera(
                    previewView = previewView,
                    lifecycleOwner = lifecycleOwner,
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
                        if (dragAmount > 0) {
                            // swipe right
                            currentFilterIndex = (currentFilterIndex - 1 + filters.size) % filters.size
                        } else {
                            // swipe left
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
                    captureWithFilter(ic, context, currentFilter, gpuImage, onImageCaptured)
                }
            },
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.BottomCenter)
                .padding(24.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            shape = CircleShape
        ) {
            Icon(imageVector = Icons.Default.Camera, contentDescription = "Capture")
        }
    }
}

/** Starts CameraX preview with filter applied in real time */
private fun startCamera(
    previewView: androidx.camera.view.PreviewView,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onImageCaptureReady: (ImageCapture) -> Unit,
    filter: GPUImageFilter,
    gpuImage: GPUImage
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(previewView.context)
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().build()
        val imageCapture = ImageCapture.Builder().build()

        // We use an ImageAnalysis to feed frames into GPUImage for live filter preview
        val analyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        analyzer.setAnalyzer(ContextCompat.getMainExecutor(previewView.context)) { imageProxy ->
            val bitmap = imageProxyToBitmap(imageProxy)
            imageProxy.close()
            if (bitmap != null) {
                gpuImage.setImage(bitmap)
                gpuImage.setFilter(filter)
                // draws filtered image into the GLSurface or internal texture in GPUImage,
                // so preview will show filtered result
            }
        }

        preview.setSurfaceProvider(previewView.surfaceProvider)

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageCapture,
            analyzer
        )

        onImageCaptureReady(imageCapture)
    }, ContextCompat.getMainExecutor(previewView.context))
}

/** Capture a photo with the current filter applied */
private fun captureWithFilter(
    imageCapture: ImageCapture,
    context: android.content.Context,
    filter: GPUImageFilter,
    gpuImage: GPUImage,
    onImageCaptured: (Uri) -> Unit
) {
    // First take a picture, then apply filter and save
    val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    val name = "IMG_${sdf.format(Date())}.jpg"

    val contentValues = android.content.ContentValues().apply {
        put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, name)
        put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/FilteredCamera")
        }
    }

    val outputOptions = ImageCapture.OutputFileOptions.Builder(
        context.contentResolver,
        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
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
                    // Now apply the filter to the saved image
                    applyFilterToUri(savedUri, filter, context, onImageCaptured)
                }
            }
        }
    )
}

/** Load the image at uri, apply filter, save it, and return new Uri */
private fun applyFilterToUri(
    uri: Uri,
    filter: GPUImageFilter,
    context: android.content.Context,
    onImageSaved: (Uri) -> Unit
) {
    try {
        val input = context.contentResolver.openInputStream(uri)
        val bmp = android.graphics.BitmapFactory.decodeStream(input)
        input?.close()
        val gpu = GPUImage(context)
        gpu.setImage(bmp)
        gpu.setFilter(filter)
        val filteredBmp = gpu.bitmapWithFilterApplied

        // Save filteredBmp to a new URI
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val name = "FILT_${sdf.format(Date())}.jpg"
        val cv = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, name)
            put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/FilteredCamera")
            }
        }
        val newUri = context.contentResolver.insert(
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv
        )

        newUri?.let {
            val os = context.contentResolver.openOutputStream(it)
            os?.use { stream ->
                filteredBmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, stream)
            }
            onImageSaved(it)
        } ?: run {
            // fallback: return original
            onImageSaved(uri)
        }
    } catch (e: Exception) {
        Log.e("CameraFilter", "Filter or save failed: ${e.message}", e)
        onImageSaved(uri)
    }
}

/** Convert ImageProxy to Bitmap (simple version) */
private fun imageProxyToBitmap(imageProxy: ImageProxy): android.graphics.Bitmap? {
    val plane = imageProxy.planes.firstOrNull() ?: return null
    val buffer = plane.buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}
