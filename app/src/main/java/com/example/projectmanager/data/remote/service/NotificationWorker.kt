package com.example.projectmanager.data.remote.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.projectmanager.data.model.Notification
import com.example.projectmanager.data.remote.service.NotificationService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Date
import java.util.UUID

@HiltWorker
class NotificationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val notificationService: NotificationService
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val notificationId = inputData.getString(NotificationService.KEY_NOTIFICATION_ID) ?: UUID.randomUUID().toString()
        val title = inputData.getString(NotificationService.KEY_NOTIFICATION_TITLE) ?: "Reminder"
        val content = inputData.getString(NotificationService.KEY_NOTIFICATION_CONTENT) ?: "You have a pending task"

        val notification = Notification(
            id = notificationId,
            title = title,
            content = content,
            type = "REMINDER",
            timestamp = Date(),
            isRead = false
        )

        notificationService.showNotification(notification)

        return Result.success()
    }
}