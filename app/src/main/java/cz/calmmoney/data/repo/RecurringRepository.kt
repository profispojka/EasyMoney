package cz.calmmoney.data.repo

import cz.calmmoney.core.categorize.MerchantText
import cz.calmmoney.core.recurring.RecurringDetector
import cz.calmmoney.data.db.FrequencyUnit
import cz.calmmoney.data.db.RecordDao
import cz.calmmoney.data.db.RecordSource
import cz.calmmoney.data.db.RecordType
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** Detekce opakovaných plateb z Fio historie → návrhy na plánované platby. */
@Singleton
class RecurringRepository @Inject constructor(
    private val recordDao: RecordDao,
    private val categorization: CategorizationRepository,
    private val planned: PlannedPaymentRepository,
) {
    /** Nové (ještě nezaložené) kandidáty na opakovanou platbu pro daný účet. */
    suspend fun detectNew(accountId: String?): List<RecurringDetector.Candidate> {
        if (accountId == null) return emptyList()
        val recs = recordDao.observeByAccount(accountId).first().filter { it.source == RecordSource.FIO }
        val learned = categorization.learnedSorted()
        val existing = planned.observeAll().first()
            .map { dedupKey(it.accountId, it.amountMinor, it.name) }.toSet()
        return RecurringDetector.detect(recs, learned)
            .filter { dedupKey(accountId, it.amountMinor, it.name) !in existing }
    }

    /** Založí vybrané kandidáty jako měsíční plánované platby; vrátí počet. */
    suspend fun addAsPlanned(accountId: String, candidates: List<RecurringDetector.Candidate>): Int {
        candidates.forEach { c ->
            planned.create(
                name = c.name.trim().ifBlank { "Platba" }.take(40),
                type = RecordType.EXPENSE,
                accountId = accountId,
                categoryId = c.categoryId,
                amountMinor = c.amountMinor,
                frequencyUnit = FrequencyUnit.MONTH,
                frequencyCount = 1,
                startEpochDay = c.nextStartEpochDay,
                endEpochDay = null,
                note = "Z Fio (odhad z historie)",
            )
        }
        return candidates.size
    }

    private fun dedupKey(accountId: String?, amountMinor: Long, name: String?): String =
        "$accountId|$amountMinor|${MerchantText.key(name)}"
}
