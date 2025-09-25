package com.example.chit

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext

class ChatHomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChatHomeScreen()
        }
    }
}

// ----------------- Main Screen -----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatHomeScreen() {
    val tabs = listOf(
        BottomNavItem("Chats", Icons.Default.Chat),
        BottomNavItem("Updates", Icons.Default.History),
        BottomNavItem("Communities", Icons.Default.Groups),
        BottomNavItem("Calls", Icons.Default.Call)
    )

    var selectedIndex by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WhatsApp") },
                actions = {
                    IconButton(onClick = { /* Camera */ }) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Camera")
                    }
                    IconButton(onClick = { /* More Options */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedIndex == index,
                        onClick = { selectedIndex = index },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedIndex == 0) {
                FloatingActionButton(onClick = { /* New Chat */ }) {
                    Icon(Icons.Default.Add, contentDescription = "New Chat")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (selectedIndex) {
                0 -> ChatsTab()
                1 -> UpdatesTab()
                2 -> CommunitiesTab()
                3 -> CallsTab()
            }
        }
    }
}

// ----------------- Bottom Navigation Item Model -----------------

data class BottomNavItem(val label: String, val icon: ImageVector)

// ----------------- Chat Item Model -----------------

data class ChatItem(val nameOrNumber: String, val time: String)

// ----------------- Chat Tab -----------------

@Composable
fun ChatsTab() {
    val chatList = listOf(
        ChatItem("Rahul", "10:20 AM"),
        ChatItem("Anjali", "Yesterday"),
        ChatItem("+91 9876543210", "Monday"),
        ChatItem("Office Group", "Sun"),
        ChatItem("Family", "Sat")
    )
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }

    Column(modifier = Modifier.fillMaxSize()) {
        // 🔍 Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search chats") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )

        // 💬 Chat list
        LazyColumn {
            items(chatList.filter {
                it.nameOrNumber.contains(searchQuery.text, ignoreCase = true)
            }) { chat ->
                ChatListItem(chat = chat, onClick = {
                    val intent = Intent(context, ChatDetailActivity::class.java)
                    context.startActivity(intent)
                })
                Divider()
            }
        }
    }
}

@Composable
fun ChatListItem(chat: ChatItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ✅ This is the required content block

        // Profile placeholder
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = chat.nameOrNumber.take(1).uppercase(),
                fontSize = 20.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = chat.nameOrNumber, style = MaterialTheme.typography.titleMedium)
        }

        Text(
            text = chat.time,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

// ----------------- Other Tabs (Placeholders) -----------------

@Composable
fun UpdatesTab() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Updates Screen")
    }
}

@Composable
fun CommunitiesTab() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Communities Screen")
    }
}

@Composable
fun CallsTab() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Calls Screen")
    }
}
