package com.example.chit

import android.Manifest
import android.content.Context
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
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CustomCameraActivity : ComponentActivity() {
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)

        setContent {
            CustomCameraScreen(
                onBack = { finish() },
                onPhotoCaptured = { uri ->
                    Toast.makeText(this, "Photo captured!", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK, Intent().apply { data = uri })
                    finish()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomCameraScreen(
    onBack: () -> Unit,
    onPhotoCaptured: (Uri) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by remember { mutableStateOf(ImageCapture.FLASH_MODE_OFF) }
    var showCustomization by remember { mutableStateOf(false) }
    var isCameraReady by remember { mutableStateOf(false) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        SimpleCameraPreview(
            lensFacing = lensFacing,
            flashMode = flashMode,
            onCameraReady = { isCameraReady = true },
            onPhotoCaptured = onPhotoCaptured,
            executor = cameraExecutor,
            modifier = Modifier.fillMaxSize()
        )

        // Top Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.3f))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            Row {
                IconButton(
                    onClick = {
                        flashMode = when (flashMode) {
                            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                            else -> ImageCapture.FLASH_MODE_OFF
                        }
                    }
                ) {
                    Icon(
                        when (flashMode) {
                            ImageCapture.FLASH_MODE_OFF -> Icons.Default.FlashOff
                            ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                            else -> Icons.Default.FlashAuto
                        },
                        contentDescription = "Flash",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
                            CameraSelector.LENS_FACING_FRONT
                        else
                            CameraSelector.LENS_FACING_BACK
                    }
                ) {
                    Icon(Icons.Default.Cameraswitch, contentDescription = "Switch Camera", tint = Color.White)
                }
            }
        }

        // Bottom Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.3f))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Magic Button
            FloatingActionButton(
                onClick = { showCustomization = true },
                containerColor = Color.Magenta,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "Magic", tint = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Capture Button
            FloatingActionButton(
                onClick = {
                    if (isCameraReady) {
                        coroutineScope.launch {
                            // Trigger capture through the preview
                            // This will be handled by the SimpleCameraPreview's click handler
                        }
                    }
                },
                containerColor = Color.White,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(Icons.Default.Camera, contentDescription = "Capture", tint = Color.Black)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Tap to capture", color = Color.White, style = MaterialTheme.typography.bodySmall)
        }

        // Real-time Customization Panel
        if (showCustomization) {
            RealTimeCustomizationPanel(
                onDismiss = { showCustomization = false },
                onFilterSelected = { filter ->
                    Toast.makeText(context, "Applied filter: $filter", Toast.LENGTH_SHORT).show()
                },
                onEffectSelected = { effect ->
                    Toast.makeText(context, "Applied effect: $effect", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
fun SimpleCameraPreview(
    lensFacing: Int,
    flashMode: Int,
    onCameraReady: () -> Unit,
    onPhotoCaptured: (Uri) -> Unit,
    executor: ExecutorService,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER

                setOnClickListener {
                    imageCapture?.let { capture ->
                        capturePhoto(capture, context, executor, onPhotoCaptured)
                    }
                }
            }
        },
        modifier = modifier.clickable {
            imageCapture?.let { capture ->
                capturePhoto(capture, context, executor, onPhotoCaptured)
            }
        },
        update = { previewView ->
            // Initialize camera in a coroutine or background thread
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()

                    // Build preview
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    // Build image capture
                    imageCapture = ImageCapture.Builder()
                        .setFlashMode(flashMode)
                        .build()

                    // Select camera lens
                    val cameraSelector = CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build()

                    // Unbind all use cases first
                    cameraProvider.unbindAll()

                    // Bind use cases to camera
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )

                    onCameraReady()

                } catch (exc: Exception) {
                    Log.e("SimpleCameraPreview", "Camera initialization failed", exc)
                    Toast.makeText(context, "Camera failed to start", Toast.LENGTH_SHORT).show()
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
}

fun capturePhoto(
    imageCapture: ImageCapture,
    context: Context,
    executor: ExecutorService,
    onPhotoCaptured: (Uri) -> Unit
) {
    // Create time-stamped output file
    val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
        .format(System.currentTimeMillis())

    val contentValues = android.content.ContentValues().apply {
        put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, name)
        put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
        }
    }

    val outputOptions = ImageCapture.OutputFileOptions.Builder(
        context.contentResolver,
        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    ).build()

    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onError(exc: ImageCaptureException) {
                Log.e("Camera", "Photo capture failed: ${exc.message}", exc)
                Toast.makeText(context, "Photo capture failed: ${exc.message}", Toast.LENGTH_SHORT).show()
            }

            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                output.savedUri?.let { uri ->
                    onPhotoCaptured(uri)
                }
            }
        }
    )
}

@Composable
fun RealTimeCustomizationPanel(
    onDismiss: () -> Unit,
    onFilterSelected: (String) -> Unit,
    onEffectSelected: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable(onClick = onDismiss)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .width(300.dp)
                .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium)
                .padding(16.dp)
                .clickable(enabled = false) { },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Real-time Customization", style = MaterialTheme.typography.headlineSmall)

            Spacer(modifier = Modifier.height(16.dp))

            Text("Filters", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Normal", "Vintage", "B&W", "Warm", "Cool").forEach { filter ->
                    FilterChip(
                        selected = false,
                        onClick = { onFilterSelected(filter) },
                        label = { Text(filter) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Effects", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Blur", "Brighten", "Contrast", "Sharpen", "Saturate").forEach { effect ->
                    FilterChip(
                        selected = false,
                        onClick = { onEffectSelected(effect) },
                        label = { Text(effect) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = onDismiss) {
                Text("Apply Changes")
            }
        }
    }
}