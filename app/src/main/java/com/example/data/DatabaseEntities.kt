package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: String,
    val name: String,
    val avatarUrl: String,
    val isGroup: Boolean,
    val isAi: Boolean,
    val lastMessage: String,
    val lastMessageTime: Long,
    val unreadCount: Int = 0
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val chatId: String,
    val content: String,
    val timestamp: Long,
    val isOutgoing: Boolean,
    val status: String // "SENT", "DELIVERED", "READ"
)

@Entity(tableName = "calls")
data class CallEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val callerName: String,
    val timestamp: Long,
    val isIncoming: Boolean,
    val isMissed: Boolean,
    val isAudio: Boolean
)
