package com.example.projectmanager.data.repository

import com.example.projectmanager.data.model.FileAttachment
import com.example.projectmanager.data.remote.firebase.FirebaseStorageSource
import com.example.projectmanager.data.remote.firebase.FirestoreFileSource
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageRepository @Inject constructor(
    private val storageSource: FirebaseStorageSource,
    private val fileSource: FirestoreFileSource
) {
    suspend fun uploadProjectFile(file: File, projectId: String, userId: String): FileAttachment {
        val path = "projects/$projectId/files/${file.name}"
        val downloadUrl = storageSource.uploadFile(file, path)

        val fileAttachment = FileAttachment(
            name = file.name,
            url = downloadUrl,
            mimeType = getMimeType(file),
            size = file.length(),
            uploaderId = userId,
            uploadedAt = System.currentTimeMillis(),
            projectId = projectId
        )

        val fileId = fileSource.saveFileMetadata(fileAttachment)
        return fileAttachment.copy(id = fileId)
    }

    suspend fun uploadTaskFile(file: File, taskId: String, projectId: String, userId: String): FileAttachment {
        val path = "projects/$projectId/tasks/$taskId/files/${file.name}"
        val downloadUrl = storageSource.uploadFile(file, path)

        val fileAttachment = FileAttachment(
            name = file.name,
            url = downloadUrl,
            mimeType = getMimeType(file),
            size = file.length(),
            uploaderId = userId,
            uploadedAt = System.currentTimeMillis(),
            projectId = projectId,
            taskId = taskId
        )

        val fileId = fileSource.saveFileMetadata(fileAttachment)
        return fileAttachment.copy(id = fileId)
    }

    suspend fun deleteFile(fileAttachment: FileAttachment) {
        // Delete from storage
        val path = when {
            fileAttachment.commentId != null ->
                "projects/${fileAttachment.projectId}/tasks/${fileAttachment.taskId}/comments/${fileAttachment.commentId}/files/${fileAttachment.name}"
            fileAttachment.taskId != null ->
                "projects/${fileAttachment.projectId}/tasks/${fileAttachment.taskId}/files/${fileAttachment.name}"
            else ->
                "projects/${fileAttachment.projectId}/files/${fileAttachment.name}"
        }

        storageSource.deleteFile(path)

        // Delete metadata
        fileSource.deleteFileMetadata(fileAttachment.id)
    }

    private fun getMimeType(file: File): String {
        // Simple mime type detection based on file extension
        return when (file.extension.toLowerCase()) {
            "pdf" -> "application/pdf"
            "doc", "docx" -> "application/msword"
            "xls", "xlsx" -> "application/vnd.ms-excel"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "txt" -> "text/plain"
            else -> "application/octet-stream"
        }
    }
}