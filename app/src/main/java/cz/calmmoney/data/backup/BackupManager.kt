package cz.calmmoney.data.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import cz.calmmoney.data.db.CalmMoneyDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lokální záloha/obnova celé databáze do/z uživatelského souboru (SAF).
 * Záloha = kopie SQLite souboru po checkpointu WAL. Obnova přepíše DB a restartuje appku.
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: CalmMoneyDatabase,
) {
    private val SQLITE_HEADER = "SQLite format 3"

    private fun dbFile(): File = context.getDatabasePath(CalmMoneyDatabase.NAME)

    /** Zapíše aktuální databázi do [uri]. */
    suspend fun backupTo(uri: Uri) = withContext(Dispatchers.IO) {
        // Sloučí WAL do hlavního souboru, ať je kopie konzistentní.
        database.openHelper.writableDatabase
            .query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }
        val out = context.contentResolver.openOutputStream(uri)
            ?: throw IllegalStateException("Nelze otevřít cíl zálohy.")
        out.use { output ->
            dbFile().inputStream().use { it.copyTo(output) }
        }
    }

    /** Přečte zálohu z [uri], ověří, že je to SQLite, a přepíše databázi. Vrací úspěch. */
    suspend fun restoreFrom(uri: Uri) = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("Nelze otevřít zálohu.")
        require(bytes.size >= 16 && String(bytes.copyOfRange(0, 15), Charsets.US_ASCII) == SQLITE_HEADER) {
            "Soubor není platná záloha CalmMoney."
        }
        database.close()
        val db = dbFile()
        File(db.path + "-wal").delete()
        File(db.path + "-shm").delete()
        db.outputStream().use { it.write(bytes) }
    }

    /** Restartuje aplikaci (po obnově, aby se DB znovu otevřela čistě). */
    fun restartApp() {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        Runtime.getRuntime().exit(0)
    }
}
