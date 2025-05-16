package com.example.projectmanager.data.repository

import com.example.projectmanager.data.model.Chat
import com.example.projectmanager.data.model.Message
import com.example.projectmanager.util.Resource
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getChats(userId: String): Flow<Resource<List<Chat>>>
    fun getProjectChats(projectId: String): Flow<Resource<List<Chat>>>
    fun getChatMessages(chatId: String): Flow<Resource<List<Message>>>
    suspend fun createChat(chat: Chat): Resource<Chat>
    suspend fun sendMessage(message: Message): Resource<Message>
    suspend fun markMessageAsRead(messageId: String, chatId: String, userId: String): Resource<Unit>
    suspend fun deleteMessage(messageId: String, chatId: String): Resource<Unit>
    suspend fun deleteChat(chatId: String): Resource<Unit>
} 