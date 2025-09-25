package com.example.chit

import android.Manifest
import android.content.Intent
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.emoji2.emojipicker.EmojiPickerView
import kotlinx.coroutines.delay
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ChatDetailActivity : ComponentActivity() {
    private lateinit var imageUri: Uri
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var videoUri: Uri
    private lateinit var videoLauncher: ActivityResultLauncher<Uri>
    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<String>
    private var cameraPermissionGranted = false
    private lateinit var audioPermissionLauncher: ActivityResultLauncher<String>
    private var isAudioPermissionGranted = false
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var isRecording = false
    // Add this for CustomCameraActivity result
    private lateinit var customCameraLauncher: ActivityResultLauncher<Intent>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChatDetailScreen(contactName = "Rahul")
        }
// Register for CustomCameraActivity result
        customCameraLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val imageUri = result.data?.data
                if (imageUri != null) {
                    Toast.makeText(this, "Photo captured successfully!", Toast.LENGTH_SHORT).show()
                    // Handle the captured photo here (upload, display, etc.)
                }
            }
        }
        cameraPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            cameraPermissionGranted = granted
            if (granted) {
                // Now you can safely open camera/video
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
            }
        }
        audioPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            isAudioPermissionGranted = granted
            if (!granted) {
                Toast.makeText(this, "Microphone permission is required", Toast.LENGTH_SHORT).show()
            }
        }
        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                // Handle imageUri (upload, show preview, etc.)
            }
        }
        videoLauncher = registerForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
            if (success) {
                // Handle videoUri
            }
        }
    }

    fun openCamera() {
        if (!cameraPermissionGranted) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            return
        }
        val photoFile = File(
            getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "IMG_${System.currentTimeMillis()}.jpg"
        )
        imageUri = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            photoFile
        )
        cameraLauncher.launch(imageUri)
    }

    fun openVideoRecorder() {
        if (!cameraPermissionGranted) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            return
        }
        val videoFile = File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), "VID_${System.currentTimeMillis()}.mp4")
        videoUri = FileProvider.getUriForFile(this, "${packageName}.provider", videoFile)
        videoLauncher.launch(videoUri)
    }
    fun openCustomCamera() {
        if (!cameraPermissionGranted) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            return
        }
        val intent = Intent(this, CustomCameraActivity::class.java)
        customCameraLauncher.launch(intent)
    }

    fun startRecording() {
        if (!isAudioPermissionGranted) {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        try {
            val fileName = "AUD_${System.currentTimeMillis()}.m4a"
            audioFile = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), fileName)

            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile?.absolutePath)
                prepare()
                start()
                isRecording = true
            }
            Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Recording failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopRecording() {
        if (!isRecording) return

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            isRecording = false
            Toast.makeText(this, "Audio saved: ${audioFile?.name}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Stop recording failed", Toast.LENGTH_SHORT).show()
        } finally {
            mediaRecorder = null
        }
    }

    fun cancelRecording() {
        if (!isRecording) return

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            isRecording = false
            audioFile?.delete()
            Toast.makeText(this, "Recording canceled", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder = null
        }
    }
}

data class ChatMessage(
    val message: String,
    val isSentByUser: Boolean,
    val timestamp: String,
    val date: LocalDate
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(contactName: String) {
    var messageInput by remember { mutableStateOf(TextFieldValue("")) }
    var showAttachmentOptions by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    // Remove this line: var showCameraDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = context as? ChatDetailActivity
    val messages = remember {
        listOf(
            ChatMessage("Hey!", false, "10:00 AM", LocalDate.now().minusDays(1)),
            ChatMessage("Hello!", true, "10:01 AM", LocalDate.now().minusDays(1)),
            ChatMessage("How are you?", false, "10:02 AM", LocalDate.now()),
            ChatMessage("I'm good, thanks!", true, "10:03 AM", LocalDate.now())
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(contactName) },
                navigationIcon = {
                    IconButton(onClick = { /* Handle back */ }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Video Call */ }) {
                        Icon(Icons.Default.Videocam, contentDescription = "Video Call")
                    }
                    IconButton(onClick = { /* Call */ }) {
                        Icon(Icons.Default.Call, contentDescription = "Call")
                    }
                    IconButton(onClick = { /* Info */ }) {
                        Icon(Icons.Default.Info, contentDescription = "Info")
                    }
                }
            )
        },
        bottomBar = {
            ChatInputBar(
                messageInput = messageInput,
                onMessageChange = { messageInput = it },
                onSend = {
                    messageInput = TextFieldValue("")
                },
                onAttachmentClick = {
                    showAttachmentOptions = true
                },
                onEmojiClick = {
                    showEmojiPicker = true
                },
                onCameraClick = {
                    // Directly open custom camera without dialog
                    activity?.openCustomCamera()
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        ) {
            ChatMessageList(messages)

            if (showAttachmentOptions) {
                AttachmentOptionsDialog(onDismiss = {
                    showAttachmentOptions = false
                })
            }

            if (showEmojiPicker) {
                EmojiPickerDialog(
                    onDismiss = { showEmojiPicker = false },
                    onEmojiSelected = { emoji ->
                        messageInput = TextFieldValue(messageInput.text + emoji)
                        showEmojiPicker = false
                    }
                )
            }

            // Remove the showCameraDialog condition and ShowDialog call
        }
    }
}
@Composable
fun ChatInputBar(
    messageInput: TextFieldValue,
    onMessageChange: (TextFieldValue) -> Unit,
    onSend: () -> Unit,
    onAttachmentClick: () -> Unit,
    onEmojiClick: () -> Unit,
    onCameraClick: () -> Unit  // This now directly opens the camera
) {
    val context = LocalContext.current
    val activity = context as? ChatDetailActivity
    var isRecording by remember { mutableStateOf(false) }
    var recordingTime by remember { mutableStateOf(0L) }
    var showRecordingUI by remember { mutableStateOf(false) }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingTime = 0L
            while (isRecording) {
                delay(1000L)
                recordingTime++
            }
        }
    }

    if (showRecordingUI) {
        RecordingOverlay(
            onStopRecording = {
                isRecording = false
                showRecordingUI = false
                activity?.stopRecording()
            },
            onCancelRecording = {
                isRecording = false
                showRecordingUI = false
                activity?.cancelRecording()
            },
            recordingTime = recordingTime
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        IconButton(onClick = onEmojiClick) {
            Icon(Icons.Default.EmojiEmotions, contentDescription = "Emoji")
        }

        IconButton(onClick = onAttachmentClick) {
            Icon(Icons.Default.AttachFile, contentDescription = "Attachment")
        }

        // Camera icon now directly triggers the camera without dialog
        IconButton(onClick = onCameraClick) {
            Icon(Icons.Default.PhotoCamera, contentDescription = "Camera")
        }

        TextField(
            value = messageInput,
            onValueChange = onMessageChange,
            placeholder = { Text("Message") },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        Box(
            modifier = Modifier
                .size(48.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            if (messageInput.text.isBlank()) {
                                isRecording = true
                                showRecordingUI = true
                                activity?.startRecording()

                                tryAwaitRelease()

                                isRecording = false
                                showRecordingUI = false
                                activity?.stopRecording()
                            } else {
                                onSend()
                            }
                        }
                    )
                }
                .background(
                    color = if (messageInput.text.isNotBlank()) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (messageInput.text.isNotBlank()) Icons.Default.Send else Icons.Default.Mic,
                contentDescription = if (messageInput.text.isNotBlank()) "Send" else "Record Audio",
                tint = if (messageInput.text.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
@Composable
fun RecordingOverlay(
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    recordingTime: Long
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Recording",
                tint = Color.Red,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = formatRecordingTime(recordingTime),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Release to send • Swipe to cancel",
                color = Color.White,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onCancelRecording,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("Cancel")
            }
        }

        Button(
            onClick = onStopRecording,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
        ) {
            Icon(Icons.Default.Check, contentDescription = "Stop Recording")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Send Recording")
        }
    }
}

fun formatRecordingTime(seconds: Long): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}

@Composable
fun ShowDialog(title: String, options: List<String>, onOptionSelected: (String) -> Unit) {
    var showDialog by remember { mutableStateOf(true) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {},
            title = { Text(title) },
            text = {
                Column {
                    options.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showDialog = false
                                    onOptionSelected(option)
                                }
                                .padding(12.dp)
                        ) {
                            Text(option)
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun ChatMessageList(messages: List<ChatMessage>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 64.dp),
        contentPadding = PaddingValues(8.dp)
    ) {
        var lastDate: LocalDate? = null

        itemsIndexed(messages) { _, message ->
            if (message.date != lastDate) {
                DateSeparator(message.date)
                lastDate = message.date
            }
            ChatBubble(message)
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
fun DateSeparator(date: LocalDate) {
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = date.format(formatter),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val bubbleColor = if (message.isSentByUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (message.isSentByUser) Color.White else Color.Black

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isSentByUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .background(bubbleColor, RoundedCornerShape(8.dp))
                .padding(10.dp)
                .widthIn(max = 280.dp)
        ) {
            Text(message.message, color = textColor)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                message.timestamp,
                color = textColor.copy(alpha = 0.6f),
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
fun AttachmentOptionsDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = {
            Text("Select attachment")
        },
        text = {
            Column {
                AttachmentOption("Gallery", Icons.Default.Image)
                AttachmentOption("Document", Icons.Default.Description)
                AttachmentOption("Camera", Icons.Default.PhotoCamera)
                AttachmentOption("Location", Icons.Default.Place)
                AttachmentOption("Contact", Icons.Default.Person)
                AttachmentOption("Audio", Icons.Default.Audiotrack)
            }
        }
    )
}

@Composable
fun AttachmentOption(label: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Handle action */ }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.padding(end = 8.dp))
        Text(label)
    }
}

@Composable
fun EmojiPickerDialog(onDismiss: () -> Unit, onEmojiSelected: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { Text("Choose Emoji") },
        text = {
            AndroidView(
                factory = { context ->
                    EmojiPickerView(context).apply {
                        setOnEmojiPickedListener { emoji ->
                            onEmojiSelected(emoji.emoji)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            )
        }
    )
}