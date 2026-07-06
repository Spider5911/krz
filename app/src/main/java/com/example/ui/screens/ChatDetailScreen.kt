package com.example.ui.screens

import androidx.compose.animation.*
import kotlinx.coroutines.delay
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ChatEntity
import com.example.data.MessageEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    viewModel: ChatViewModel,
    chatId: String,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chats by viewModel.filteredChats.collectAsState()
    val chat = remember(chats, chatId) { chats.find { it.id == chatId } }
    val messages by viewModel.activeMessages.collectAsState()
    val isTyping by viewModel.isTyping.collectAsState()

    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll to the bottom of the list when new messages arrive
    LaunchedEffect(messages.size, isTyping) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (chat == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Chat not found", color = Color.Gray)
        }
        return
    }

    val isDark = isSystemInDarkTheme()
    // Beautiful minimalist wallpaper background colors
    val wallpaperColor = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF)

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { }
                        ) {
                            // High-fidelity Clean Minimalist Avatar with initials, gradient or color shading
                            val avatarBgBrush = remember(chat.isAi, chat.isGroup) {
                                if (chat.isAi) {
                                    androidx.compose.ui.graphics.Brush.linearGradient(
                                        colors = listOf(Color(0xFF34C759), Color(0xFF007AFF))
                                    )
                                } else if (chat.isGroup) {
                                    androidx.compose.ui.graphics.Brush.linearGradient(
                                        colors = listOf(Color(0xFF60A5FA), Color(0xFF6366F1))
                                    )
                                } else {
                                    null
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        color = if (avatarBgBrush == null) {
                                            if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9)
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
                                    Icon(Icons.Default.AutoAwesome, contentDescription = "AI", tint = Color.White, modifier = Modifier.size(20.dp))
                                } else if (chat.isGroup) {
                                    Icon(Icons.Default.Groups, contentDescription = "Group", tint = Color.White, modifier = Modifier.size(20.dp))
                                } else {
                                    Text(
                                        text = chat.name.take(2).uppercase(),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF475569)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = chat.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                )
                                Text(
                                    text = if (isTyping) "typing..." else if (chat.isAi) "online (Meta AI)" else "online",
                                    fontSize = 12.sp,
                                    color = if (isTyping) WhatsAppBlue else Color.Gray,
                                    fontWeight = if (isTyping) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBackClicked,
                            modifier = Modifier.testTag("chat_back_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = WhatsAppBlue)
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.makeCall(chat.name, false) }) {
                            Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = WhatsAppBlue)
                        }
                        IconButton(onClick = { viewModel.makeCall(chat.name, true) }) {
                            Icon(Icons.Default.Call, contentDescription = "Voice Call", tint = WhatsAppBlue)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f), thickness = 0.5.dp)
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(wallpaperColor)
                // Draw elegant subtle WhatsApp iOS wallpaper grid background
                .drawBehind {
                    if (!isDark) {
                        // Soft elegant beige background pattern lines
                        val lineColor = Color.Gray.copy(alpha = 0.04f)
                        val step = 40.dp.toPx()
                        var x = 0f
                        while (x < size.width) {
                            drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                            x += step
                        }
                        var y = 0f
                        while (y < size.height) {
                            drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                            y += step
                        }
                    }
                }
        ) {
            // Message Bubbles View Area
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Safety information / End-to-End Encryption header row
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDark) Color(0xFF182229) else Color(0xFFFFF9C4))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .widthIn(max = 280.dp)
                        ) {
                            Text(
                                text = "🔒 Messages and calls are end-to-end encrypted. No one outside of this chat, not even WhatsApp, can read or listen to them.",
                                fontSize = 11.sp,
                                color = if (isDark) iOSDarkTextSecondary else Color(0xFF5D4037),
                                textAlign = TextAlign.Center,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                items(messages) { message ->
                    MessageBubble(message = message, isDark = isDark)
                }

                // AI is Typing Indicator Row
                if (isTyping) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isDark) iOSDarkBubbleReceiver else iOSLightBubbleReceiver)
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${chat.name} is typing",
                                        fontSize = 13.sp,
                                        color = if (isDark) Color.White else Color.Black
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    TypingDotsAnimation()
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Text Bar area
            Column {
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f), thickness = 0.5.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // "+" Attachment action button
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Attachments",
                        tint = WhatsAppBlue,
                        modifier = Modifier
                            .size(28.dp)
                            .clickable { }
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    // Text Input box (iOS style grey background rounded field)
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("Text Message", fontSize = 15.sp, color = Color.Gray) },
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = if (isDark) Color(0xFF1F2C34) else Color(0xFFF0F2F5),
                            unfocusedContainerColor = if (isDark) Color(0xFF1F2C34) else Color(0xFFF0F2F5),
                            focusedBorderColor = Color.LightGray.copy(alpha = 0.3f),
                            unfocusedBorderColor = Color.LightGray.copy(alpha = 0.3f)
                        ),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            capitalization = KeyboardCapitalization.Sentences
                        ),
                        singleLine = false,
                        maxLines = 4,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("message_input_field")
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    // Action transitions (Camera + Mic when empty, Send button when text is populated)
                    AnimatedContent(
                        targetState = textInput.isNotBlank(),
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "ActionTransition"
                    ) { hasText ->
                        if (hasText) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(WhatsAppBlue)
                                    .clickable {
                                        viewModel.sendMessage(chatId, textInput)
                                        textInput = ""
                                    }
                                    .testTag("send_message_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Icon(
                                    imageVector = Icons.Outlined.CameraAlt,
                                    contentDescription = "Camera",
                                    tint = WhatsAppBlue,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clickable { }
                                )
                                Icon(
                                    imageVector = Icons.Outlined.Mic,
                                    contentDescription = "Voice note",
                                    tint = WhatsAppBlue,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clickable { }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: MessageEntity, isDark: Boolean) {
    val formatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val timeString = remember(message.timestamp) { formatter.format(Date(message.timestamp)) }

    val bubbleBg = if (message.isOutgoing) {
        if (isDark) iOSDarkBubbleSender else iOSLightBubbleSender
    } else {
        if (isDark) iOSDarkBubbleReceiver else iOSLightBubbleReceiver
    }

    val textColor = if (isDark) Color.White else Color.Black

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = if (message.isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 14.dp,
                        topEnd = 14.dp,
                        bottomStart = if (message.isOutgoing) 14.dp else 2.dp,
                        bottomEnd = if (message.isOutgoing) 2.dp else 14.dp
                    )
                )
                .background(bubbleBg)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Column {
                Text(
                    text = message.content,
                    fontSize = 15.sp,
                    color = textColor,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeString,
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    if (message.isOutgoing) {
                        Spacer(modifier = Modifier.width(4.dp))
                        // iOS style Delivery Ticks
                        val tickColor = if (message.status == "READ") WhatsAppBlue else Color.Gray
                        val tickIcon = if (message.status == "SENT") {
                            Icons.Default.Check
                        } else {
                            Icons.Default.DoneAll
                        }
                        Icon(
                            imageVector = tickIcon,
                            contentDescription = message.status,
                            tint = tickColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TypingDotsAnimation() {
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(400)
            tick = (tick + 1) % 4
        }
    }
    Text(
        text = ".".repeat(tick),
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = WhatsAppBlue,
        modifier = Modifier.width(20.dp)
    )
}
