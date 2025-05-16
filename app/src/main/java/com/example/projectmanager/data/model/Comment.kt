package com.example.projectmanager.data.model

data class Comment(
    val id: String = "",
    val taskId: String = "",
    val userId: String = "",
    val content: String = "",
    val createdAt: Long = 0,
    val updatedAt: Long? = null,
    val attachmentIds: List<String> = emptyList()
)
