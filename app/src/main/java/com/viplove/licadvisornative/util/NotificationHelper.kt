// File: app/src/main/java/com/viplove/licadvisornative/util/NotificationHelper.kt
package com.viplove.licadvisornative.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.viplove.licadvisornative.MainActivity
import com.viplove.licadvisornative.R

object NotificationHelper {
    
    private const val CHANNEL_ID_DUE_DATE = "policy_due_dates"
    private const val CHANNEL_NAME_DUE_DATE = "Policy Due Date Alerts"
    private const val CHANNEL_DESC_DUE_DATE = "Notifications for upcoming policy payment due dates"
    
    /**
     * Creates notification channels for the app.
     * Should be called once at app startup (e.g., in Application class or MainActivity).
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val dueDateChannel = NotificationChannel(
                CHANNEL_ID_DUE_DATE,
                CHANNEL_NAME_DUE_DATE,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC_DUE_DATE
                enableVibration(true)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(dueDateChannel)
        }
    }
    
    /**
     * Shows a notification for an upcoming policy due date.
     */
    fun showDueDateNotification(
        context: Context,
        notificationId: Int,
        policyNumber: String,
        holderName: String,
        dueDate: String,
        daysUntilDue: Int
    ) {
        // Check notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }
        
        val title = when {
            daysUntilDue <= 0 -> "⚠️ Policy Payment Overdue!"
            daysUntilDue == 1 -> "🔔 Policy Payment Due Tomorrow"
            daysUntilDue <= 7 -> "📅 Policy Payment Due Soon"
            else -> "📋 Upcoming Policy Payment"
        }
        
        val message = buildString {
            append("$holderName - Policy #$policyNumber\n")
            when {
                daysUntilDue < 0 -> append("Payment is ${-daysUntilDue} days overdue!")
                daysUntilDue == 0 -> append("Payment is due today!")
                daysUntilDue == 1 -> append("Payment is due tomorrow")
                else -> append("Due on $dueDate ($daysUntilDue days)")
            }
        }
        
        // Intent to open app when notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 
            notificationId, 
            intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_DUE_DATE)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
    
    /**
     * Cancels a specific notification by ID.
     */
    fun cancelNotification(context: Context, notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }
    
    /**
     * Cancels all notifications.
     */
    fun cancelAllNotifications(context: Context) {
        NotificationManagerCompat.from(context).cancelAll()
    }
}
