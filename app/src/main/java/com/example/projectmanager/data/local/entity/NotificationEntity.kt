package com.example.projectmanager.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "notifications",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("userId")
    ]
)
data class NotificationEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val title: String,
    val message: String,
    val type: String, // e.g., TASK_ASSIGNED, COMMENT_ADDED, PROJECT_DEADLINE
    val relatedItemId: String?, // Could be taskId, projectId, commentId
    val createdAt: Date,
    val isRead: Boolean = false
)