package com.example.projectmanager.data.remote.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.example.projectmanager.R
import com.example.projectmanager.data.model.Notification
import com.example.projectmanager.data.repository.UserRepository
import com.example.projectmanager.ui.main.MainActivity
import com.example.projectmanager.util.Constants
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userRepository: UserRepository,
    private val workManager: WorkManager
) {
    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: Flow<List<Notification>> = _notifications

    init {
        createNotificationChannel()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Register the current user for FCM notifications
                val currentUser = userRepository.getCurrentUser().first()
                currentUser?.let { user ->
                    val token = FirebaseMessaging.getInstance().token.await()
                    userRepository.updateFcmToken(user.userId, token)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to register for FCM notifications")
            }
        }
    }

    fun showNotification(notification: Notification) {
        // Add to the internal list
        val currentList = _notifications.value.toMutableList()
        currentList.add(0, notification)
        _notifications.value = currentList

        // Show system notification
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(Constants.EXTRA_NOTIFICATION_ID, notification.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notification.id.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(notification.title)
            .setContentText(notification.content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(notification.id.hashCode(), builder.build())
            } catch (e: SecurityException) {
                Timber.e(e, "Notification permission not granted")
            }
        }
    }

    fun scheduleReminder(notificationId: String, title: String, content: String, delayInMinutes: Long) {
        val data = workDataOf(
            KEY_NOTIFICATION_ID to notificationId,
            KEY_NOTIFICATION_TITLE to title,
            KEY_NOTIFICATION_CONTENT to content
        )

        val reminderRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInputData(data)
            .setInitialDelay(delayInMinutes, TimeUnit.MINUTES)
            .build()

        workManager.enqueue(reminderRequest)
    }

    fun cancelScheduledReminder(notificationId: String) {
        workManager.cancelAllWorkByTag(notificationId)
    }

    fun clearNotifications() {
        _notifications.value = emptyList()
    }

    fun markNotificationAsRead(notificationId: String) {
        val currentList = _notifications.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == notificationId }
        if (index != -1) {
            val updatedNotification = currentList[index].copy(isRead = true)
            currentList[index] = updatedNotification
            _notifications.value = currentList
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Project Manager Notifications"
            val descriptionText = "Notification channel for Project Manager app"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "project_manager_channel"
        const val KEY_NOTIFICATION_ID = "notification_id"
        const val KEY_NOTIFICATION_TITLE = "notification_title"
        const val KEY_NOTIFICATION_CONTENT = "notification_content"
    }
}