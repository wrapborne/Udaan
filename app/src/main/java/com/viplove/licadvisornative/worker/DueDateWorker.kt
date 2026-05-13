package com.viplove.licadvisornative.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.viplove.licadvisornative.network.ApiClient
import com.viplove.licadvisornative.network.TokenManager
import com.viplove.licadvisornative.network.toPolicy
import com.viplove.licadvisornative.util.NotificationHelper
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class DueDateWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "DueDateWorker"
        private const val WORK_NAME = "due_date_check_work"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<DueDateWorker>(
                repeatInterval = 12,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setInitialDelay(1, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )

            Log.d(TAG, "DueDateWorker scheduled for periodic execution")
        }

        fun runNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<DueDateWorker>().build()
            WorkManager.getInstance(context).enqueue(request)
            Log.d(TAG, "DueDateWorker triggered for immediate execution")
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "DueDateWorker: Starting due date check...")

        if (!TokenManager.isLoggedIn()) {
            Log.d(TAG, "DueDateWorker: No user logged in, skipping")
            return Result.success()
        }

        return try {
            val role = TokenManager.getUserRole() ?: "advisor"

            // Only advisors get policy due date notifications
            if (role != "advisor" && role != "agent") {
                return Result.success()
            }

            val response = ApiClient.api.getPolicies()
            if (!response.isSuccessful) {
                Log.w(TAG, "DueDateWorker: Failed to fetch policies")
                return Result.retry()
            }

            val policies = response.body()?.map { it.toPolicy() } ?: emptyList()
            var notificationCount = 0

            policies.forEach { policy ->
                val daysUntilDue = calculateDaysUntilDue(policy.doc, policy.mode, policy.enachDate)

                if (daysUntilDue != null && daysUntilDue <= 7) {
                    val dueDate = calculateDueDate(policy.doc, policy.mode, policy.enachDate)
                    NotificationHelper.showDueDateNotification(
                        context = applicationContext,
                        notificationId = policy.policyNumber.hashCode(),
                        policyNumber = policy.policyNumber,
                        holderName = policy.shortName,
                        dueDate = dueDate,
                        daysUntilDue = daysUntilDue
                    )
                    notificationCount++
                }
            }

            Log.d(TAG, "DueDateWorker: Sent $notificationCount notifications")
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "DueDateWorker: Error checking due dates", e)
            Result.retry()
        }
    }

    private fun calculateDaysUntilDue(docTimestamp: Long, mode: String, enachDate: String): Int? {
        val dueDate = try {
            val baseTimestamp = if (enachDate.isNotEmpty()) {
                SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                    .parse(enachDate)?.time ?: docTimestamp
            } else {
                docTimestamp
            }

            val calendar = Calendar.getInstance().apply { timeInMillis = baseTimestamp }
            val today = Calendar.getInstance()

            val monthsToAdd = when (mode.uppercase()) {
                "YEARLY" -> 12
                "HALF YEARLY", "HLY" -> 6
                "QUARTERLY", "QTLY" -> 3
                "MONTHLY", "MNLY" -> 1
                else -> 12
            }

            while (calendar.before(today)) {
                calendar.add(Calendar.MONTH, monthsToAdd)
            }

            calendar.time
        } catch (e: Exception) {
            return null
        }

        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }

        val dueDateCal = Calendar.getInstance().apply {
            time = dueDate
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }

        val diffInMillis = dueDateCal.timeInMillis - today.timeInMillis
        return (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
    }

    private fun calculateDueDate(docTimestamp: Long, mode: String, enachDate: String): String {
        val baseTimestamp = if (enachDate.isNotEmpty()) {
            try {
                SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                    .parse(enachDate)?.time ?: docTimestamp
            } catch (e: Exception) {
                docTimestamp
            }
        } else {
            docTimestamp
        }

        val calendar = Calendar.getInstance().apply { timeInMillis = baseTimestamp }
        val today = Calendar.getInstance()

        val monthsToAdd = when (mode.uppercase()) {
            "YEARLY" -> 12
            "HALF YEARLY", "HLY" -> 6
            "QUARTERLY", "QTLY" -> 3
            "MONTHLY", "MNLY" -> 1
            else -> 12
        }

        while (calendar.before(today)) {
            calendar.add(Calendar.MONTH, monthsToAdd)
        }

        return SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(calendar.time)
    }
}
