package com.example.ui.screens

import androidx.compose.animation.*
import kotlinx.coroutines.delay
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CallEntity
import com.example.data.ChatEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTabScreen(
    viewModel: ChatViewModel,
    onChatClicked: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(3) } // Default to "Chats" (index 3)
    val chats by viewModel.filteredChats.collectAsState()
    val calls by viewModel.allCalls.collectAsState()
    
    // Create Chat Dialog State
    var showCreateDialog by remember { mutableStateOf(false) }
    var newChatName by remember { mutableStateOf("") }
    var isNewGroup by remember { mutableStateOf(false) }
    var isNewAi by remember { mutableStateOf(false) }

    // Status View Dialog State
    var activeStatusUser by remember { mutableStateOf<String?>(null) }

    Scaffold(
        bottomBar = {
            Column {
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f), thickness = 0.5.dp)
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.background,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .height(60.dp)
                ) {
                    val tabs = listOf(
                        NavigationBarItemInfo("Updates", Icons.Outlined.ChangeCircle, Icons.Filled.ChangeCircle, 0),
                        NavigationBarItemInfo("Calls", Icons.Outlined.Call, Icons.Filled.Call, 1),
                        NavigationBarItemInfo("Communities", Icons.Outlined.Groups, Icons.Filled.Groups, 2),
                        NavigationBarItemInfo("Chats", Icons.Outlined.Chat, Icons.Filled.Chat, 3),
                        NavigationBarItemInfo("Settings", Icons.Outlined.Settings, Icons.Filled.Settings, 4)
                    )

                    tabs.forEach { tab ->
                        val isSelected = selectedTab == tab.index
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { selectedTab = tab.index },
                            icon = {
                                Box {
                                    Icon(
                                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label,
                                        tint = if (isSelected) WhatsAppBlue else iOSLightTextSecondary
                                    )
                                    // Add a badge on Chats tab if there are unread messages
                                    if (tab.index == 3) {
                                        val totalUnread = chats.sumOf { it.unreadCount }
                                        if (totalUnread > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .offset(x = 10.dp, y = (-5).dp)
                                                    .background(WhatsAppBlue, CircleShape)
                                                    .size(16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = totalUnread.toString(),
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                            label = {
                                Text(
                                    text = tab.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) WhatsAppBlue else iOSLightTextSecondary
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (selectedTab) {
                0 -> UpdatesTab(onStatusClicked = { activeStatusUser = it })
                1 -> CallsTab(viewModel, calls)
                2 -> CommunitiesTab()
                3 -> ChatsTab(
                    viewModel = viewModel,
                    chats = chats,
                    onChatClicked = onChatClicked,
                    onNewChatClicked = { showCreateDialog = true }
                )
                4 -> SettingsTab(viewModel)
            }

            // High-fidelity full-screen status viewer
            if (activeStatusUser != null) {
                StatusViewer(
                    contactName = activeStatusUser!!,
                    onDismiss = { activeStatusUser = null }
                )
            }

            // Create Chat Dialog
            if (showCreateDialog) {
                AlertDialog(
                    onDismissRequest = { showCreateDialog = false },
                    title = { Text("New Conversation", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = newChatName,
                                onValueChange = { newChatName = it },
                                label = { Text("Contact or Group Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isNewGroup,
                                    onCheckedChange = {
                                        isNewGroup = it
                                        if (it) isNewAi = false
                                    }
                                )
                                Text("Is this a Group?")
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isNewAi,
                                    onCheckedChange = {
                                        isNewAi = it
                                        if (it) isNewGroup = false
                                    }
                                )
                                Text("Connect to Meta AI (Gemini)")
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (newChatName.isNotBlank()) {
                                    viewModel.createNewChat(newChatName, isNewGroup, isNewAi)
                                    showCreateDialog = false
                                    newChatName = ""
                                    isNewGroup = false
                                    isNewAi = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppBlue)
                        ) {
                            Text("Create")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCreateDialog = false }) {
                            Text("Cancel", color = Color.Gray)
                        }
                    }
                )
            }
        }
    }
}

data class NavigationBarItemInfo(
    val label: String,
    val unselectedIcon: ImageVector,
    val selectedIcon: ImageVector,
    val index: Int
)

// ==================== CHATS TAB ====================
@Composable
fun ChatsTab(
    viewModel: ChatViewModel,
    chats: List<ChatEntity>,
    onChatClicked: (String) -> Unit,
    onNewChatClicked: () -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val activeFilter by viewModel.chatFilter.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // iOS Style Top Bar Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Edit",
                color = WhatsAppBlue,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { }
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(
                    imageVector = Icons.Outlined.CameraAlt,
                    contentDescription = "Camera",
                    tint = WhatsAppBlue,
                    modifier = Modifier.clickable { }
                )
                Icon(
                    imageVector = Icons.Outlined.BorderColor, // iOS Pencil in Box substitute
                    contentDescription = "New Message",
                    tint = WhatsAppBlue,
                    modifier = Modifier
                        .clickable { onNewChatClicked() }
                        .testTag("new_message_button")
                )
            }
        }

        // Title "Chats"
        Text(
            text = "Chats",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
        )

        // iOS style Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Search", fontSize = 15.sp, color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = Color.Gray,
                        modifier = Modifier.clickable { viewModel.setSearchQuery("") }
                    )
                }
            },
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.LightGray.copy(alpha = 0.15f),
                unfocusedContainerColor = Color.LightGray.copy(alpha = 0.15f),
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                disabledBorderColor = Color.Transparent
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(48.dp)
        )

        // Pill Filter Rows ("All", "Unread", "Groups")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Unread", "Groups").forEach { filter ->
                val isSelected = activeFilter == filter
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) WhatsAppBlue.copy(alpha = 0.15f) else Color.LightGray.copy(alpha = 0.15f))
                        .clickable { viewModel.setFilter(filter) }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = filter,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) WhatsAppBlue else Color.Gray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Chats List View
        if (chats.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Forum,
                        contentDescription = "No chats",
                        tint = Color.Gray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Chats Found",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                // Broadcast / New Group horizontal link buttons
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Broadcast Lists", color = WhatsAppBlue, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Text("New Group", color = WhatsAppBlue, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.clickable { onNewChatClicked() })
                    }
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f), thickness = 0.5.dp, modifier = Modifier.padding(start = 16.dp))
                }

                items(chats) { chat ->
                    ChatItemView(chat = chat, onClick = { onChatClicked(chat.id) })
                }
            }
        }
    }
}

@Composable
fun ChatItemView(chat: ChatEntity, onClick: () -> Unit) {
    val formatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val timeString = remember(chat.lastMessageTime) {
        if (chat.lastMessageTime > 0) formatter.format(Date(chat.lastMessageTime)) else ""
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("chat_item_${chat.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // High-fidelity Clean Minimalist Avatar with initials, gradient or color shading
            val isDark = isSystemInDarkTheme()
            val avatarBgBrush = remember(chat.isAi, chat.isGroup) {
                if (chat.isAi) {
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(Color(0xFF34C759), Color(0xFF007AFF))
                    )
                } else if (chat.isGroup) {
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(Color(0xFF60A5FA), Color(0xFF6366F1)) // blue-400 to indigo-500 from HTML
                    )
                } else {
                    null
                }
            }

            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(
                        color = if (avatarBgBrush == null) {
                            if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9) // Slate-700 or Slate-100
                        } else {
                            Color.Transparent
                        }
                    )
                    .then(
                        if (avatarBgBrush != null) Modifier.background(avatarBgBrush) else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (chat.isAi) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "Meta AI", tint = Color.White, modifier = Modifier.size(26.dp))
                } else if (chat.isGroup) {
                    Icon(Icons.Default.Groups, contentDescription = "Group", tint = Color.White, modifier = Modifier.size(26.dp))
                } else {
                    Text(
                        text = chat.name.take(2).uppercase(),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF475569) // slate-200 or slate-600
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = timeString,
                        fontSize = 13.sp,
                        color = if (chat.unreadCount > 0) WhatsAppBlue else Color.Gray,
                        fontWeight = if (chat.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.lastMessage,
                        fontSize = 14.sp,
                        color = Color.Gray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (chat.unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .background(WhatsAppBlue, CircleShape)
                                .size(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = chat.unreadCount.toString(),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f), thickness = 0.5.dp, modifier = Modifier.padding(start = 84.dp))
    }
}

// ==================== CALLS TAB ====================
@Composable
fun CallsTab(viewModel: ChatViewModel, calls: List<CallEntity>) {
    var selectedFilter by remember { mutableStateOf(0) } // 0 = All, 1 = Missed

    val displayedCalls = remember(calls, selectedFilter) {
        if (selectedFilter == 1) calls.filter { it.isMissed } else calls
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // iOS Style Calls Header with center segment buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Edit",
                color = WhatsAppBlue,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { }
            )

            // Segmented Control ("All", "Missed")
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.LightGray.copy(alpha = 0.2f))
                    .padding(2.dp)
            ) {
                listOf("All", "Missed").forEachIndexed { index, label ->
                    val isSelected = selectedFilter == index
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) Color.White else Color.Transparent)
                            .clickable { selectedFilter = index }
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.Call,
                contentDescription = "New Call",
                tint = WhatsAppBlue,
                modifier = Modifier.clickable {
                    // Quick add a random outgoing call
                    viewModel.makeCall("Sarah Jenkins", true)
                }
            )
        }

        Text(
            text = "Calls",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                // Create Call Link Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.LightGray.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Link, contentDescription = "Link", tint = WhatsAppBlue)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text("Create Call Link", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = WhatsAppBlue)
                        Text("Share a link for your WhatsApp call", fontSize = 13.sp, color = Color.Gray)
                    }
                }
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f), thickness = 0.5.dp, modifier = Modifier.padding(start = 74.dp))
            }

            item {
                Text(
                    text = "Recent",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            if (displayedCalls.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No Calls", color = Color.Gray)
                    }
                }
            } else {
                items(displayedCalls) { call ->
                    CallHistoryItem(call)
                }
            }
        }
    }
}

@Composable
fun CallHistoryItem(call: CallEntity) {
    val formatter = remember { SimpleDateFormat("EEEE, h:mm a", Locale.getDefault()) }
    val timeString = remember(call.timestamp) { formatter.format(Date(call.timestamp)) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color.LightGray.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(call.callerName.take(1).uppercase(), fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = call.callerName,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (call.isMissed) Color.Red else Color.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (call.isIncoming) Icons.Default.CallReceived else Icons.Default.CallMade,
                    contentDescription = "Direction",
                    tint = Color.Gray,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(timeString, fontSize = 13.sp, color = Color.Gray)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(
                imageVector = if (call.isAudio) Icons.Default.Call else Icons.Default.Videocam,
                contentDescription = "Call Type",
                tint = WhatsAppBlue
            )
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Details",
                tint = WhatsAppBlue
            )
        }
    }
    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f), thickness = 0.5.dp, modifier = Modifier.padding(start = 70.dp))
}

// ==================== UPDATES (STATUS) TAB ====================
@Composable
fun UpdatesTab(onStatusClicked: (String) -> Unit) {
    val mockStatuses = listOf(
        StatusInfo("Sarah Jenkins", "sarah", true),
        StatusInfo("Alex Rivera", "alex", true),
        StatusInfo("John Doe", "john", false)
    )

    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // iOS Style Updates Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Privacy", color = WhatsAppBlue, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Icon(Icons.Default.Search, contentDescription = "Search", tint = WhatsAppBlue)
        }

        Text(
            text = "Updates",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Status section inside a clean rounded iOS container card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(cardColor)
                .padding(14.dp)
        ) {
            Text("Status", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // My Status add icon
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onStatusClicked("My Status") }
                    ) {
                        Box(modifier = Modifier.size(60.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(Color.LightGray.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = "Me", tint = Color.Gray, modifier = Modifier.size(32.dp))
                            }
                            // Small blue add badge (Minimalism)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .background(WhatsAppBlue, CircleShape)
                                    .size(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("My Status", fontSize = 12.sp, color = textColor)
                    }
                }

                items(mockStatuses) { status ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onStatusClicked(status.name) }
                    ) {
                        // Ringed status wrapper (WhatsApp iOS unread blue status ring)
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(if (status.isUnread) WhatsAppBlue else Color.LightGray)
                                .padding(2.5.dp) // Ring border thickness
                                .clip(CircleShape)
                                .background(cardColor)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(Color.LightGray.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(status.name.take(1), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = textColor)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(status.name.split(" ").first(), fontSize = 12.sp, color = textColor)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Channels Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(cardColor)
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Channels", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
                Text("Explore >", color = WhatsAppBlue, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Stay updated on your favorite topics. Find channels to follow below.", fontSize = 13.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(14.dp))

            // Channels List
            val mockChannels = listOf("WhatsApp", "Meta AI News", "Real Madrid C.F.")
            mockChannels.forEachIndexed { index, name ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(WhatsAppBlue.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Verified, contentDescription = "Verified", tint = WhatsAppBlue)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                        Text("32.5M followers", fontSize = 12.sp, color = Color.Gray)
                    }
                    Button(
                        onClick = { },
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppBlue.copy(alpha = 0.1f)),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Follow", color = WhatsAppBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (index < mockChannels.size - 1) {
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f), thickness = 0.5.dp)
                }
            }
        }
    }
}

data class StatusInfo(val name: String, val id: String, val isUnread: Boolean)

// ==================== COMMUNITIES TAB ====================
@Composable
fun CommunitiesTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F7)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Groups,
            contentDescription = "Communities",
            tint = WhatsAppBlue,
            modifier = Modifier.size(96.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Introducing Communities",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Easily organize your related groups and send announcements. Now your neighborhoods or schools can have their own space.",
            fontSize = 15.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { },
            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppBlue),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth()
        ) {
            Text("Start a Community", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

// ==================== SETTINGS TAB ====================
@Composable
fun SettingsTab(viewModel: ChatViewModel) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val cardColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Settings",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Card (Grouped Card style)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(cardColor)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(WhatsAppBlue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("ME", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("User Pro", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
                        Text("iOS style design rocks! 🚀", fontSize = 13.sp, color = Color.Gray)
                    }
                    Icon(Icons.Default.QrCode, contentDescription = "QR Code", tint = WhatsAppBlue)
                }
            }

            // Group 1
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(cardColor)
                ) {
                    SettingsRow(Icons.Default.Star, "Starred Messages", Color(0xFFFFCC00), textColor)
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f), thickness = 0.5.dp, modifier = Modifier.padding(start = 54.dp))
                    SettingsRow(Icons.Default.Laptop, "Linked Devices", Color(0xFF00C7BE), textColor)
                }
            }

            // Group 2
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(cardColor)
                ) {
                    SettingsRow(Icons.Default.Key, "Account", WhatsAppBlue, textColor)
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f), thickness = 0.5.dp, modifier = Modifier.padding(start = 54.dp))
                    SettingsRow(Icons.Default.Lock, "Privacy", Color(0xFF34C759), textColor)
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f), thickness = 0.5.dp, modifier = Modifier.padding(start = 54.dp))
                    SettingsRow(Icons.Default.Chat, "Chats", WhatsAppBlue, textColor) // WhatsAppBlue for Minimalism
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f), thickness = 0.5.dp, modifier = Modifier.padding(start = 54.dp))
                    SettingsRow(Icons.Default.Notifications, "Notifications", Color(0xFFFF3B30), textColor)
                }
            }

            // Clear database row (convenience feature)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(cardColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Add a mock incoming call
                                viewModel.addMockCall("Meta AI", true, true, false)
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(WhatsAppBlue, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "Call", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Add Mock Call Log", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun SettingsRow(icon: ImageVector, title: String, color: Color, textColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(color, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = textColor, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Go", tint = Color.LightGray)
    }
}

// ==================== STATUS LIGHTBOX VIEWER ====================
@Composable
fun StatusViewer(contactName: String, onDismiss: () -> Unit) {
    var progress by remember { mutableStateOf(0f) }

    // Auto dismiss after 5 seconds
    LaunchedEffect(Unit) {
        val duration = 5000L
        val interval = 50L
        val steps = duration / interval
        for (i in 0..steps) {
            delay(interval)
            progress = i.toFloat() / steps
        }
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { onDismiss() }
    ) {
        // Status Progress Bars
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            LinearProgressIndicator(
                progress = { progress },
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f),
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(CircleShape)
            )
        }

        // Contact Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(WhatsAppGreenLight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(contactName.take(1), color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(contactName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("Just now", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            }
        }

        // Status Content Display
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.ChatBubble,
                    contentDescription = "Status Text",
                    tint = WhatsAppGreenLight,
                    modifier = Modifier.size(84.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = when (contactName) {
                        "Sarah Jenkins" -> "Enjoying a lovely espresso in San Francisco! ☕✨"
                        "Alex Rivera" -> "Coding up some beautiful Jetpack Compose screens. iOS theme matches perfectly! 📱💻"
                        "My Status" -> "Sharing my status with friends on WhatsApp iOS clone!"
                        else -> "Living life to the fullest! 🌟"
                    },
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}
