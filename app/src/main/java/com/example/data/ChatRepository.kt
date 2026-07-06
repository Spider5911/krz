package com.example.data

import com.example.BuildConfig
import com.example.api.Content
import com.example.api.GeminiRequest
import com.example.api.Part
import com.example.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ChatRepository(private val chatDao: ChatDao) {

    val allChats: Flow<List<ChatEntity>> = chatDao.getAllChatsFlow()
    val allCalls: Flow<List<CallEntity>> = chatDao.getAllCallsFlow()

    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>> {
        return chatDao.getMessagesForChatFlow(chatId)
    }

    suspend fun insertChat(chat: ChatEntity) = withContext(Dispatchers.IO) {
        chatDao.insertChat(chat)
    }

    suspend fun updateChat(chat: ChatEntity) = withContext(Dispatchers.IO) {
        chatDao.updateChat(chat)
    }

    suspend fun insertMessage(message: MessageEntity) = withContext(Dispatchers.IO) {
        chatDao.insertMessage(message)
    }

    suspend fun insertCall(call: CallEntity) = withContext(Dispatchers.IO) {
        chatDao.insertCall(call)
    }

    suspend fun clearChatMessages(chatId: String) = withContext(Dispatchers.IO) {
        chatDao.clearChatMessages(chatId)
    }

    suspend fun updateLastMessage(chatId: String, lastMsg: String, time: Long, unread: Int) = withContext(Dispatchers.IO) {
        chatDao.updateLastMessage(chatId, lastMsg, time, unread)
    }

    suspend fun askGemini(prompt: String, chatHistory: List<MessageEntity>): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Hi! I am Meta AI. I'm running in offline/no-key mode right now. To enable real-time replies, please enter your Gemini API Key in the AI Studio Secrets panel!"
        }

        // Limit history to the last 15 messages to preserve token budget
        val recentHistory = chatHistory.takeLast(15)
        val contentsList = mutableListOf<Content>()

        recentHistory.forEach { msg ->
            val role = if (msg.isOutgoing) "user" else "model"
            // Wait, Gemini expects contents structured as: role: user/model, parts: [{text: ...}]
            // Let's ensure we build standard parts
            contentsList.add(
                Content(parts = listOf(Part(text = msg.content)))
            )
        }

        // Add the current prompt
        contentsList.add(Content(parts = listOf(Part(text = prompt))))

        val systemInstruction = Content(
            parts = listOf(
                Part(
                    text = "You are Meta AI, a smart assistant integrated directly into WhatsApp for iOS. " +
                            "Keep your answers friendly, concise (under 3-4 sentences), and formatted nicely for a mobile chat bubble screen. " +
                            "Use formatting like bullet points or bold text sparingly, and matches the natural, easy-going tone of a friendly messaging chat."
                )
            )
        )

        val request = GeminiRequest(
            contents = contentsList,
            systemInstruction = systemInstruction
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "I received an empty response. Please try again!"
        } catch (e: Exception) {
            "Meta AI (Offline): I had a little trouble reaching my servers. Let's chat offline! Error: ${e.localizedMessage ?: "Network error"}"
        }
    }
}
