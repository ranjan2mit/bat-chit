package com.example.chit
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.*

class OtpActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val phoneNumber = intent.getStringExtra("phone_number")
        setContent {
            OtpScreen(phoneNumber)
        }
    }
}

@Composable
fun OtpScreen(phoneNumber: String?) {
    var otp by remember { mutableStateOf("") }
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Enter the 6-digit code",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Sent to +91 ${phoneNumber ?: ""}",
            fontSize = 16.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OtpInputField(otp = otp, onOtpChange = { if (it.length <= 6) otp = it })

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val intent = Intent(context, ProfileSetupActivity::class.java)
                context.startActivity(intent)
            },
            enabled = otp.length == 6,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Verify")
        }
    }
}

@Composable
fun OtpInputField(otp: String, onOtpChange: (String) -> Unit) {
    OutlinedTextField(
        value = otp,
        onValueChange = onOtpChange,
        label = { Text("OTP") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        visualTransformation = PasswordVisualTransformation(), // Optional
        modifier = Modifier.fillMaxWidth()
    )
}
