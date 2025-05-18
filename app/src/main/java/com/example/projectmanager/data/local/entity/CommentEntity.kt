package com.example.projectmanager.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.projectmanager.data.local.Converters
import com.example.projectmanager.data.model.Comment
import java.util.Date

@Entity(
    tableName = "comments",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("taskId"),
        Index("userId")
    ]
)
@TypeConverters(Converters::class)
data class CommentEntity(
    @PrimaryKey
    val id: String,
    val taskId: String,
    val userId: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long?,
    val attachmentIds: List<String> = emptyList()
) {
    fun toDomain() = Comment(
        id = id,
        taskId = taskId,
        userId = userId,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt,
        attachmentIds = attachmentIds
    )

    companion object {
        fun fromDomain(comment: Comment) = CommentEntity(
            id = comment.id,
            taskId = comment.taskId,
            userId = comment.userId,
            content = comment.content,
            createdAt = comment.createdAt,
            updatedAt = comment.updatedAt,
            attachmentIds = comment.attachmentIds
        )
    }
}