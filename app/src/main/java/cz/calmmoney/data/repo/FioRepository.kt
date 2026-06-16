package cz.calmmoney.data.repo

import cz.calmmoney.core.categorize.Categorizer
import cz.calmmoney.core.categorize.MerchantText
import cz.calmmoney.core.fio.FioApiClient
import cz.calmmoney.core.fio.FioFetchResult
import cz.calmmoney.core.fio.FioParser
import cz.calmmoney.core.fio.FioTx
import cz.calmmoney.data.db.RecordDao
import cz.calmmoney.data.db.RecordEntity
import cz.calmmoney.data.db.RecordSource
import cz.calmmoney.data.db.RecordType
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/** Výsledek synchronizace s Fio. */
sealed interface FioSyncResult {
    data class Success(val added: Int, val total: Int, val categorized: Int) : FioSyncResult
    data object RateLimited : FioSyncResult
    data class Error(val message: String) : FioSyncResult
}

/**
 * Import pohybů z Fio do účtu CalmMoney. Deduplikace přes unikátní `fioTransactionId`.
 * Při importu rovnou **kategorizuje** (naučená pravidla + seed) a vlastní převody mezi
 * účty (protistrana = majitel účtu) označí jako TRANSFER (mimo příjmy/výdaje ve statistikách).
 */
@Singleton
class FioRepository @Inject constructor(
    private val client: FioApiClient,
    private val recordDao: RecordDao,
    private val categorization: CategorizationRepository,
) {
    private val zone: ZoneId = ZoneId.systemDefault()

    /**
     * Stáhne a naimportuje pohyby za posledních [daysBack] dní do účtu [accountId].
     * Fio bez silné autorizace poskytne jen data ne starší 90 dní — proto výchozí okno 90 dní.
     */
    suspend fun sync(token: String, accountId: String, daysBack: Long = 90): FioSyncResult {
        if (token.isBlank()) return FioSyncResult.Error("Chybí Fio token.")
        val to = LocalDate.now()
        val from = to.minusDays(daysBack)
        return when (val r = client.fetchPeriods(token, from, to)) {
            is FioFetchResult.Success -> importJson(r.json, accountId)
            FioFetchResult.RateLimited -> FioSyncResult.RateLimited
            is FioFetchResult.HttpError -> when (r.code) {
                404 -> FioSyncResult.Error("Neplatný token (HTTP 404). Zkontroluj, že je správně zkopírovaný.")
                422 -> FioSyncResult.Error(
                    "Pro data starší 90 dní odemkni token ve Fio internetbankingu: Nastavení → API → " +
                        "ikona zámku u tokenu → potvrď (SMS/aplikace). Pak do 10 minut stáhni znovu.",
                )
                else -> FioSyncResult.Error("Fio odmítlo dotaz (HTTP ${r.code}).")
            }
            is FioFetchResult.NetworkError -> FioSyncResult.Error("Síťová chyba: ${r.message}")
        }
    }

    /** Naparsuje JSON, zkategorizuje a vloží nové pohyby; vrátí přidané / celkem / zařazené. */
    suspend fun importJson(json: String, accountId: String): FioSyncResult {
        val txs = runCatching { FioParser.parse(json) }
            .getOrElse { return FioSyncResult.Error("Nepodařilo se přečíst data z Fia.") }

        val learned = categorization.learnedSorted()
        val ownerNorm = ownerOf(txs)

        var added = 0
        var categorized = 0
        for (t in txs) {
            val minor = BigDecimal(t.amount).movePointRight(2).setScale(0, RoundingMode.HALF_UP).toLong()
            val ts = System.currentTimeMillis()

            // U karetních plateb je protiúčet prázdný a obchodník je v textu („Nákup: …").
            val rawDesc = t.message ?: t.userIdentification ?: t.comment
            val payee = t.counterName ?: merchantFrom(rawDesc) ?: t.counterAccount
            val note = if (rawDesc != null && rawDesc != payee) rawDesc else null

            var recType = if (minor < 0) RecordType.EXPENSE else RecordType.INCOME
            var categoryId: String? = null
            var transferOut: Boolean? = null
            when (val res = Categorizer.categorize(payee, note, t.type, minor >= 0, ownerNorm, learned)) {
                Categorizer.Result.Transfer -> { recType = RecordType.TRANSFER; transferOut = minor < 0 }
                is Categorizer.Result.Category -> categoryId = res.id
                Categorizer.Result.None -> {}
            }

            val rec = RecordEntity(
                id = UUID.randomUUID().toString(),
                type = recType,
                accountId = accountId,
                categoryId = categoryId,
                amountMinor = abs(minor),
                dateTime = t.date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli(),
                payee = payee,
                note = note,
                transferOut = transferOut,
                source = RecordSource.FIO,
                fioTransactionId = t.id,
                createdAt = ts,
                updatedAt = ts,
            )
            if (recordDao.insertIgnore(rec) != -1L) {
                added++
                if (categoryId != null || recType == RecordType.TRANSFER) categorized++
            }
        }
        return FioSyncResult.Success(added, txs.size, categorized)
    }

    /** Majitel účtu = nejčastější „Provedl" (normalizovaně) — pro detekci vlastních převodů. */
    private fun ownerOf(txs: List<FioTx>): String =
        txs.mapNotNull { it.performedBy }
            .map { MerchantText.normalize(it) }
            .filter { it.isNotEmpty() }
            .groupingBy { it }.eachCount()
            .maxByOrNull { it.value }?.key ?: ""

    /** Z popisu karetní platby vytáhne obchodníka: „Nákup: ALBERT…, Havířov…" → „ALBERT…". */
    private fun merchantFrom(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        var s = raw.trim()
        for (prefix in listOf("Nákup:", "Platba kartou:", "Výběr:", "Kredit:", "Nákup")) {
            if (s.startsWith(prefix)) { s = s.removePrefix(prefix).trim(); break }
        }
        return s.substringBefore(',').trim().ifBlank { null }
    }
}
