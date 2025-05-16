package com.example.projectmanager.data.model

data class Notification(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val type: NotificationType = NotificationType.INFO,
    val relatedEntityId: String? = null,
    val relatedEntityType: EntityType? = null,
    val createdAt: Long = 0,
    val isRead: Boolean = false
)
