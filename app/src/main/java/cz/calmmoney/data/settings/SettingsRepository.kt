package cz.calmmoney.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/** Uživatelská nastavení v DataStore (nikoli v Room). */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val FIO_TOKEN = stringPreferencesKey("fio_token")
        val FIO_ACCOUNT_ID = stringPreferencesKey("fio_account_id")
        val FIO_LAST_SYNC = longPreferencesKey("fio_last_sync")
    }

    val onboardingDone: Flow<Boolean> =
        context.settingsDataStore.data.map { it[Keys.ONBOARDING_DONE] ?: false }

    suspend fun setOnboardingDone(value: Boolean) {
        context.settingsDataStore.edit { it[Keys.ONBOARDING_DONE] = value }
    }

    /** Fio read-only token (jen pro export dat). Prázdné = nenastaveno. */
    val fioToken: Flow<String> =
        context.settingsDataStore.data.map { it[Keys.FIO_TOKEN] ?: "" }

    /** ID účtu CalmMoney, do kterého se Fio pohyby importují. */
    val fioAccountId: Flow<String?> =
        context.settingsDataStore.data.map { it[Keys.FIO_ACCOUNT_ID] }

    /** Čas poslední úspěšné synchronizace (epoch millis), 0 = nikdy. */
    val fioLastSync: Flow<Long> =
        context.settingsDataStore.data.map { it[Keys.FIO_LAST_SYNC] ?: 0L }

    suspend fun setFioConnection(token: String, accountId: String) {
        context.settingsDataStore.edit {
            it[Keys.FIO_TOKEN] = token.trim()
            it[Keys.FIO_ACCOUNT_ID] = accountId
        }
    }

    suspend fun setFioLastSync(epochMillis: Long) {
        context.settingsDataStore.edit { it[Keys.FIO_LAST_SYNC] = epochMillis }
    }

    suspend fun clearFio() {
        context.settingsDataStore.edit {
            it.remove(Keys.FIO_TOKEN)
            it.remove(Keys.FIO_ACCOUNT_ID)
            it.remove(Keys.FIO_LAST_SYNC)
        }
    }
}
