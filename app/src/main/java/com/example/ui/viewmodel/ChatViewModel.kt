package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CallEntity
import com.example.data.ChatEntity
import com.example.data.ChatRepository
import com.example.data.MessageEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(private val repository: ChatRepository) : ViewModel() {

    private val _selectedChatId = MutableStateFlow<String?>(null)
    val selectedChatId: StateFlow<String?> = _selectedChatId.asStateFlow()

    private val _chatFilter = MutableStateFlow("All") // "All", "Unread", "Groups"
    val chatFilter: StateFlow<String> = _chatFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    // Filtered chats flow based on search query and category filters
    val filteredChats: StateFlow<List<ChatEntity>> = combine(
        repository.allChats,
        _chatFilter,
        _searchQuery
    ) { chats, filter, query ->
        var list = chats

        // Apply filters
        list = when (filter) {
            "Unread" -> list.filter { it.unreadCount > 0 }
            "Groups" -> list.filter { it.isGroup }
            else -> list
        }

        // Apply search query
        if (query.isNotBlank()) {
            list = list.filter { it.name.contains(query, ignoreCase = true) || it.lastMessage.contains(query, ignoreCase = true) }
        }

        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCalls: StateFlow<List<CallEntity>> = repository.allCalls
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active messages flow bound to the selected chat ID
    val activeMessages: StateFlow<List<MessageEntity>> = _selectedChatId
        .flatMapLatest { id ->
            if (id.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                repository.getMessagesForChat(id)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        initializeMockData()
    }

    fun setFilter(filter: String) {
        _chatFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectChat(chatId: String?) {
        _selectedChatId.value = chatId
        if (chatId != null) {
            // Mark chat messages as read
            viewModelScope.launch {
                val chat = repository.allChats.firstOrNull()?.find { it.id == chatId }
                if (chat != null && chat.unreadCount > 0) {
                    repository.updateChat(chat.copy(unreadCount = 0))
                }
            }
        }
    }

    fun sendMessage(chatId: String, content: String) {
        if (content.isBlank()) return

        viewModelScope.launch {
            val timestamp = System.currentTimeMillis()
            val userMsg = MessageEntity(
                chatId = chatId,
                content = content,
                timestamp = timestamp,
                isOutgoing = true,
                status = "SENT"
            )

            // 1. Insert message
            repository.insertMessage(userMsg)

            // 2. Update chat info
            val chat = repository.allChats.firstOrNull()?.find { it.id == chatId }
            if (chat != null) {
                repository.updateChat(
                    chat.copy(
                        lastMessage = content,
                        lastMessageTime = timestamp,
                        unreadCount = 0
                    )
                )
            }

            // Simulate blue ticks transition
            delay(400)
            repository.insertMessage(userMsg.copy(status = "DELIVERED"))
            delay(400)
            repository.insertMessage(userMsg.copy(status = "READ"))

            // 3. Trigger Auto Response (Gemini or Mock Contacts)
            triggerResponse(chatId, content)
        }
    }

    private fun triggerResponse(chatId: String, userContent: String) {
        viewModelScope.launch {
            val chat = repository.allChats.firstOrNull()?.find { it.id == chatId } ?: return@launch
            _isTyping.value = true
            delay(1200) // Realistic delay

            val replyText: String
            if (chat.isAi) {
                // Fetch response from Gemini API
                replyText = repository.askGemini(userContent, activeMessages.value)
            } else {
                // Mock contact replies
                replyText = when {
                    chat.id == "sarah" -> {
                        if (userContent.contains("coffee", ignoreCase = true) || userContent.contains("free", ignoreCase = true)) {
                            "Awesome, coffee sounds perfect! See you at 4:00 PM ☕️"
                        } else {
                            "Sounds good! Let me know if anything changes."
                        }
                    }
                    chat.isGroup -> {
                        val responders = listOf("Alex", "Sarah", "Emily")
                        val responder = responders.random()
                        "$responder: That sounds like an awesome plan! Let's do it."
                    }
                    else -> {
                        "Hey there! Thanks for reaching out. I am currently away from my phone, but I'll reply as soon as I can!"
                    }
                }
            }

            val timestamp = System.currentTimeMillis()
            val replyMsg = MessageEntity(
                chatId = chatId,
                content = replyText,
                timestamp = timestamp,
                isOutgoing = false,
                status = "READ"
            )

            _isTyping.value = false
            repository.insertMessage(replyMsg)
            repository.updateChat(
                chat.copy(
                    lastMessage = replyText,
                    lastMessageTime = timestamp,
                    unreadCount = if (_selectedChatId.value == chatId) 0 else 1
                )
            )
        }
    }

    fun makeCall(callerName: String, isAudio: Boolean) {
        viewModelScope.launch {
            repository.insertCall(
                CallEntity(
                    callerName = callerName,
                    timestamp = System.currentTimeMillis(),
                    isIncoming = false,
                    isMissed = false,
                    isAudio = isAudio
                )
            )
        }
    }

    fun addMockCall(callerName: String, isAudio: Boolean, isIncoming: Boolean, isMissed: Boolean) {
        viewModelScope.launch {
            repository.insertCall(
                CallEntity(
                    callerName = callerName,
                    timestamp = System.currentTimeMillis() - 1200000,
                    isIncoming = isIncoming,
                    isMissed = isMissed,
                    isAudio = isAudio
                )
            )
        }
    }

    fun clearChat(chatId: String) {
        viewModelScope.launch {
            repository.clearChatMessages(chatId)
            val chat = repository.allChats.firstOrNull()?.find { it.id == chatId }
            if (chat != null) {
                repository.updateChat(
                    chat.copy(
                        lastMessage = "No messages",
                        lastMessageTime = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun createNewChat(name: String, isGroup: Boolean, isAi: Boolean) {
        viewModelScope.launch {
            val id = name.lowercase().replace(" ", "_")
            val chat = ChatEntity(
                id = id,
                name = name,
                avatarUrl = if (isAi) "meta_ai" else id,
                isGroup = isGroup,
                isAi = isAi,
                lastMessage = if (isAi) "Ask me anything!" else "New chat started",
                lastMessageTime = System.currentTimeMillis()
            )
            repository.insertChat(chat)
            
            // Insert initial greeting message
            repository.insertMessage(
                MessageEntity(
                    chatId = id,
                    content = if (isAi) "Hi! I am Meta AI. I am powered by Gemini. Ask me any question, and I'll do my best to help you!" else "Hey! I just joined WhatsApp. Let's chat!",
                    timestamp = System.currentTimeMillis(),
                    isOutgoing = false,
                    status = "READ"
                )
            )
        }
    }

    private fun initializeMockData() {
        viewModelScope.launch {
            val existingChats = repository.allChats.firstOrNull() ?: emptyList()
            if (existingChats.isNotEmpty()) return@launch

            val now = System.currentTimeMillis()

            // 1. Insert Chats
            val metaAi = ChatEntity("meta_ai", "Meta AI", "meta_ai", false, true, "Hi! Ask me anything about travel, coding, cooking or general facts.", now - 300000)
            val sarah = ChatEntity("sarah", "Sarah Jenkins", "sarah", false, false, "How about 4:00 PM at Blue Bottle?", now - 600000, 1)
            val designGroup = ChatEntity("ios_design", "iOS Developers Group", "design", true, false, "Sarah: Love it, looks super clean!", now - 1800000)
            val john = ChatEntity("john", "John Doe", "john", false, false, "Thanks! Highly appreciate it.", now - 7200000)

            repository.insertChat(metaAi)
            repository.insertChat(sarah)
            repository.insertChat(designGroup)
            repository.insertChat(john)

            // 2. Insert Messages
            // Sarah messages
            repository.insertMessage(MessageEntity(chatId = "sarah", content = "Hey! Are you free for coffee later today?", timestamp = now - 3600000, isOutgoing = false, status = "READ"))
            repository.insertMessage(MessageEntity(chatId = "sarah", content = "Yeah sure! What time?", timestamp = now - 3300000, isOutgoing = true, status = "READ"))
            repository.insertMessage(MessageEntity(chatId = "sarah", content = "How about 4:00 PM at Blue Bottle?", timestamp = now - 3000000, isOutgoing = false, status = "READ"))

            // Meta AI messages
            repository.insertMessage(MessageEntity(chatId = "meta_ai", content = "Hi! I'm Meta AI, your assistant powered by Gemini. Ask me anything!", timestamp = now - 4000000, isOutgoing = false, status = "READ"))

            // iOS Design Group messages
            repository.insertMessage(MessageEntity(chatId = "ios_design", content = "Welcome to the iOS design system group.", timestamp = now - 86400000, isOutgoing = false, status = "READ"))
            repository.insertMessage(MessageEntity(chatId = "ios_design", content = "Alex: Let's make sure the bottom navigation tabs exactly match iOS.", timestamp = now - 80000000, isOutgoing = false, status = "READ"))
            repository.insertMessage(MessageEntity(chatId = "ios_design", content = "You: Definitely, rounded lists, blurred tab bars and big headers!", timestamp = now - 75000000, isOutgoing = true, status = "READ"))
            repository.insertMessage(MessageEntity(chatId = "ios_design", content = "Sarah: Love it, looks super clean!", timestamp = now - 70000000, isOutgoing = false, status = "READ"))

            // John Doe messages
            repository.insertMessage(MessageEntity(chatId = "john", content = "Can you send me the source code link?", timestamp = now - 172800000, isOutgoing = false, status = "READ"))
            repository.insertMessage(MessageEntity(chatId = "john", content = "Sure, here's the link!", timestamp = now - 160000000, isOutgoing = true, status = "READ"))
            repository.insertMessage(MessageEntity(chatId = "john", content = "Thanks! Highly appreciate it.", timestamp = now - 150000000, isOutgoing = false, status = "READ"))

            // 3. Insert Calls
            repository.insertCall(CallEntity(callerName = "Sarah Jenkins", timestamp = now - 7200000, isIncoming = true, isMissed = false, isAudio = true))
            repository.insertCall(CallEntity(callerName = "John Doe", timestamp = now - 86400000, isIncoming = true, isMissed = true, isAudio = true))
            repository.insertCall(CallEntity(callerName = "iOS Developers Group", timestamp = now - 172800000, isIncoming = false, isMissed = false, isAudio = false))
        }
    }
}

class ChatViewModelFactory(private val repository: ChatRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
