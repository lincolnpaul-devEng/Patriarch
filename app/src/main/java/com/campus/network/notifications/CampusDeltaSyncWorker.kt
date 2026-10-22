package com.campus.network.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.campus.network.core.data.DeltaSyncManager
import com.campus.network.core.network.SupabaseHttpException
import kotlinx.coroutines.CancellationException
import java.io.IOException

/**
 * WorkManager worker that performs a full delta sync of all tables in the background.
 * Runs only when the device has a validated network connection.
 */
class CampusDeltaSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            DeltaSyncManager.get(applicationContext).syncAll()
            Result.success()
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
        private const val WORK_NAME = "campus_background_delta_sync_work"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicWork = PeriodicWorkRequestBuilder<CampusDeltaSyncWorker>(
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
