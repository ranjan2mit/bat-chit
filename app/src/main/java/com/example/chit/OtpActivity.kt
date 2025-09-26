package com.example.chit

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chit.model.VerifyOtpRequest
import com.example.chit.model.VerifyOtpResponse
import com.example.chit.network.ApiClient
import com.example.chit.storage.UserPreferences
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class OtpActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val phoneNumber = intent.getStringExtra("phone_number") ?: ""
        setContent {
            OtpScreen(phoneNumber)
        }
    }
}

@Composable
fun OtpScreen(phoneNumber: String) {
    var otp by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val userPrefs = UserPreferences(context)
    val coroutineScope = rememberCoroutineScope()
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
            text = "Sent to +91 $phoneNumber",
            fontSize = 16.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OtpInputField(otp = otp, onOtpChange = { if (it.length <= 6) otp = it })

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                // Make API call here
                loading = true
                val request = VerifyOtpRequest(phone = "91$phoneNumber", otp = otp)
                ApiClient.apiService.verifyOtp(request).enqueue(object : Callback<VerifyOtpResponse> {
                    override fun onResponse(call: Call<VerifyOtpResponse>, response: Response<VerifyOtpResponse>) {
                        loading = false
                        if (response.isSuccessful) {
                            val body = response.body()
                            Log.e("Otp Verification status", body?.status.toString())
                            if (body != null && body.status == "success") {
                                Toast.makeText(context, "Verification Successful", Toast.LENGTH_SHORT).show()
                                // Save to DataStore
                                coroutineScope.launch {
                                    userPrefs.saveUserData(
                                        token = body.token ?: "",
                                        phone = phoneNumber,
                                        isOtpVerified = true
                                    )
                                }
                                val intent = Intent(context, ProfileSetupActivity::class.java).apply {
                                    putExtra("token", body.token)
                                }
                                context.startActivity(intent)
                            } else {
                                Toast.makeText(context, "Invalid OTP or verification failed", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Server error: ${response.code()}", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<VerifyOtpResponse>, t: Throwable) {
                        loading = false
                        Toast.makeText(context, "API call failed: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                })
            },
            enabled = otp.length == 6 && !loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Verify")
            }
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
        visualTransformation = PasswordVisualTransformation(), // optional to hide OTP
        modifier = Modifier.fillMaxWidth()
    )
}
