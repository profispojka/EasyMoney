package cz.heller.core.fio

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Plánuje / ruší denní automatickou synchronizaci s Fio (WorkManager). */
@Singleton
class FioSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun scheduleDaily() {
        val request = PeriodicWorkRequestBuilder<FioSyncWorker>(1, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
            )
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(FioSyncWorker.NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(FioSyncWorker.NAME)
    }
}
