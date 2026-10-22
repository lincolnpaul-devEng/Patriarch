package com.campus.network.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.campus.network.core.data.DeltaSyncManager
import com.campus.network.core.network.SupabaseHttpException
import kotlinx.coroutines.CancellationException
import java.io.IOException

/**
 * WorkManager worker that uploads pending local writes and user-requested failed writes.
 *
 * Runs periodically on a validated network connection. Delegates to
 * [DeltaSyncManager.retryFailedSyncs] only runs for an explicit retry action; background
 * work processes PENDING rows and leaves fatal FAILED rows for the user to retry.
 */
class SyncRetryWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val manager = DeltaSyncManager.get(applicationContext)
            val shouldRetry = if (inputData.getBoolean(KEY_RETRY_FAILED, false)) {
                manager.retryFailedSyncs()
            } else {
                manager.syncPendingWrites()
            }
            if (shouldRetry) {
                Result.retry()
            } else {
                Result.success()
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: SupabaseHttpException) {
            if (error.isRetryable) Result.retry() else Result.failure()
        } catch (error: IOException) {
            Result.retry()
        } catch (error: Exception) {
            Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "campus_background_sync_retry_work"
        private const val IMMEDIATE_WORK_NAME = "campus_immediate_sync_retry_work"
        private const val KEY_RETRY_FAILED = "retry_failed"

        fun runNow(context: Context, retryFailed: Boolean = false) {
            val request = OneTimeWorkRequestBuilder<SyncRetryWorker>()
                .setInputData(Data.Builder().putBoolean(KEY_RETRY_FAILED, retryFailed).build())
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(
                    androidx.work.BackoffPolicy.EXPONENTIAL,
                    30,
                    java.util.concurrent.TimeUnit.SECONDS
                )
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
        }

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicWork = PeriodicWorkRequestBuilder<SyncRetryWorker>(
                15, java.util.concurrent.TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWork
            )
        }
    }
}
