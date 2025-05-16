package com.example.projectmanager.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Chat(
    @DocumentId
    val id: String = "",
    val name: String = "", // For group chats
    val type: ChatType = ChatType.DIRECT,
    val participants: List<String> = emptyList(),
    @get:PropertyName("project_id")
    @set:PropertyName("project_id")
    val projectId: String? = null, // Optional, for project-specific chats
    @get:PropertyName("last_message")
    @set:PropertyName("last_message")
    val lastMessage: Message? = null,
    @get:PropertyName("unread_count")
    @set:PropertyName("unread_count")
    val unreadCount: Map<String, Int> = emptyMap(), // userId to unread count
    @ServerTimestamp
    @get:PropertyName("created_at")
    @set:PropertyName("created_at")
    val createdAt: Date? = null,
    @ServerTimestamp
    @get:PropertyName("updated_at")
    @set:PropertyName("updated_at")
    val updatedAt: Date? = null
)

data class Message(
    @DocumentId
    val id: String = "",
    @get:PropertyName("chat_id")
    @set:PropertyName("chat_id")
    val chatId: String = "",
    @get:PropertyName("sender_id")
    @set:PropertyName("sender_id")
    val senderId: String = "",
    val content: String = "",
    val type: MessageType = MessageType.TEXT,
    val attachments: List<FileAttachment> = emptyList(),
    @get:PropertyName("replied_to")
    @set:PropertyName("replied_to")
    val repliedTo: Message? = null,
    @get:PropertyName("read_by")
    @set:PropertyName("read_by")
    val readBy: List<String> = emptyList(),
    @ServerTimestamp
    @get:PropertyName("sent_at")
    @set:PropertyName("sent_at")
    val sentAt: Date? = null,
    val status: MessageStatus = MessageStatus.SENT
)

enum class ChatType {
    DIRECT,      // One-on-one chat
    GROUP,       // Group chat
    PROJECT      // Project-wide chat
}

enum class MessageType {
    TEXT,
    IMAGE,
    FILE,
    SYSTEM      // For system messages like "X joined the project"
}

enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ,
    ERROR
} 