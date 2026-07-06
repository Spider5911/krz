package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY lastMessageTime DESC")
    fun getAllChatsFlow(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE id = :id LIMIT 1")
    suspend fun getChatById(id: String): ChatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)

    @Update
    suspend fun updateChat(chat: ChatEntity)

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesForChatFlow(chatId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("SELECT * FROM calls ORDER BY timestamp DESC")
    fun getAllCallsFlow(): Flow<List<CallEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCall(call: CallEntity)

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun clearChatMessages(chatId: String)

    @Query("UPDATE chats SET lastMessage = :lastMsg, lastMessageTime = :time, unreadCount = :unread WHERE id = :chatId")
    suspend fun updateLastMessage(chatId: String, lastMsg: String, time: Long, unread: Int)
}
