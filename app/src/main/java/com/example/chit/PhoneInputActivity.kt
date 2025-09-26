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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.example.chit.model.SendOtpRequest
import com.example.chit.model.SendOtpResponse
import com.example.chit.network.ApiClient
import retrofit2.Call
import retrofit2.Response

import retrofit2.Callback

class PhoneInputActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PhoneNumberScreen()
        }
    }
}

@Composable
fun PhoneNumberScreen() {
    var phoneNumber by remember { mutableStateOf("") }
    val isValid = phoneNumber.length == 10 && phoneNumber.all { it.isDigit() }
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Enter your phone number",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("+91", fontSize = 18.sp, modifier = Modifier.padding(end = 8.dp))

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                label = { Text("Phone Number") }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                isLoading = true
                val fullPhone = "91$phoneNumber" // Or however your backend expects it
                val request = SendOtpRequest(fullPhone)
                ApiClient.apiService.sendOtp(request).enqueue(object : Callback<SendOtpResponse> {
                    override fun onResponse(call: Call<SendOtpResponse>, response: Response<SendOtpResponse>) {
                        isLoading = false
                        Log.e("OTP message", response.body()?.message.toString() )
                        if (response.body()?.status.equals("success")) {
                            // Go to OTP screen
                            val intent = Intent(context, OtpActivity::class.java).apply {
                                putExtra("phone_number", phoneNumber)
                            }
                            context.startActivity(intent)
                        } else {
                            Toast.makeText(context, "Failed to send OTP", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<SendOtpResponse>, t: Throwable) {
                        isLoading = false
                        Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show()
                        Log.e("OTP", "API call failed", t)
                    }
                })
            },
            enabled = isValid && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(if (isLoading) "Sending..." else "Next")
        }

        if (!isValid && phoneNumber.isNotEmpty()) {
            Text(
                text = "Enter a valid 10-digit number",
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
