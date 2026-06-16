package cz.calmmoney.core.fio

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import cz.calmmoney.data.repo.FioRepository
import cz.calmmoney.data.repo.FioSyncResult
import cz.calmmoney.data.settings.SettingsRepository
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
        val token = settings.fioToken.first()
        val accountId = settings.fioAccountId.first()
        if (token.isBlank() || accountId == null) return Result.success()
        return when (fio.sync(token, accountId, 90)) {
            is FioSyncResult.Success -> {
                settings.setFioLastSync(System.currentTimeMillis())
                Result.success()
            }
            FioSyncResult.RateLimited -> Result.retry()
            is FioSyncResult.Error -> Result.retry()
        }
    }

    companion object {
        const val NAME = "fio_daily_sync"
    }
}
