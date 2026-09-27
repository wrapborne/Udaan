package com.viplove.licadvisornative.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.viplove.licadvisornative.network.TokenManager
import com.viplove.licadvisornative.util.NotificationHelper
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class GmailImportReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "GmailImportReminder"
        private const val WORK_NAME = "gmail_import_reminder_work"
        private const val PREF_NAME = "gmail_import_reminders"
        private const val KEY_LAST_REMINDER_AT = "last_reminder_at"
        private const val KEY_LAST_SEEN_IMPORT_AT = "last_seen_import_at"
        private const val FIFTEEN_DAYS_MS = 15L * 24 * 60 * 60 * 1000
        private const val TWO_DAYS_MS = 2L * 24 * 60 * 60 * 1000
        private const val NOTIFICATION_ID = 88015

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<GmailImportReminderWorker>(
                repeatInterval = 12,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setInitialDelay(30, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }

    override suspend fun doWork(): Result {
        if (!TokenManager.isLoggedIn()) return Result.success()
        val uid = TokenManager.getUserId() ?: return Result.success()
        val role = TokenManager.getUserRole().orEmpty()
        if (role !in listOf("advisor", "admin")) return Result.success()

        return try {
            val now = System.currentTimeMillis()
            val imports = Firebase.firestore.collection("gmail_imports")
                .whereEqualTo("uid", uid)
                .get()
                .await()
                .documents
                .filter { it.getString("status") == "APPLIED" }

            val latestDue = imports
                .filter { it.getString("pdfType") == "PREMIUM_DUE_LIST" }
                .maxOfOrNull { it.getLong("updatedAt") ?: 0L } ?: 0L
            val latestCommission = imports
                .filter { it.getString("pdfType") == "COMMISSION_BILL" }
                .maxOfOrNull { it.getLong("updatedAt") ?: 0L } ?: 0L
            val latestImport = maxOf(latestDue, latestCommission)

            val prefs = applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val lastSeenImport = prefs.getLong(KEY_LAST_SEEN_IMPORT_AT, 0L)
            if (latestImport > lastSeenImport) {
                prefs.edit()
                    .putLong(KEY_LAST_SEEN_IMPORT_AT, latestImport)
                    .remove(KEY_LAST_REMINDER_AT)
                    .apply()
                NotificationHelper.cancelNotification(applicationContext, NOTIFICATION_ID)
            }

            val missing = mutableListOf<String>()
            if (latestDue == 0L || now - latestDue >= FIFTEEN_DAYS_MS) missing += "premium due list"
            if (latestCommission == 0L || now - latestCommission >= FIFTEEN_DAYS_MS) missing += "commission bill"
            if (missing.isEmpty()) {
                NotificationHelper.cancelNotification(applicationContext, NOTIFICATION_ID)
                return Result.success()
            }

            val lastReminderAt = prefs.getLong(KEY_LAST_REMINDER_AT, 0L)
            if (lastReminderAt == 0L || now - lastReminderAt >= TWO_DAYS_MS) {
                NotificationHelper.showGmailImportReminder(
                    context = applicationContext,
                    notificationId = NOTIFICATION_ID,
                    title = "Update LIC Gmail imports",
                    message = "Please open Gmail Import and update ${missing.joinToString(" and ")}."
                )
                prefs.edit().putLong(KEY_LAST_REMINDER_AT, now).apply()
            }

            Result.success()
        } catch (error: Exception) {
            Log.e(TAG, "Could not check Gmail import freshness", error)
            Result.retry()
        }
    }
}
