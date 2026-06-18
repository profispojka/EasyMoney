package cz.heller.core.fio

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import cz.heller.data.repo.FioRepository
import cz.heller.data.repo.FioSyncResult
import cz.heller.data.settings.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/** Automatická denní synchronizace s Fio na pozadí (90 dní, jako ruční sync). */
@HiltWorker
class FioSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val settings: SettingsRepository,
    private val fio: FioRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val connections = settings.fioConnections.first()
        if (connections.isEmpty()) return Result.success()
        var anyRetry = false
        for (c in connections) {
            if (c.token.isBlank()) continue
            when (fio.sync(c.token, c.accountId, 90)) {
                is FioSyncResult.Success -> settings.setFioLastSync(c.accountId, System.currentTimeMillis())
                FioSyncResult.RateLimited -> anyRetry = true
                is FioSyncResult.Error -> anyRetry = true
            }
        }
        return if (anyRetry) Result.retry() else Result.success()
    }

    companion object {
        const val NAME = "fio_daily_sync"
    }
}
