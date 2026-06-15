package cz.calmmoney.data.repo

import cz.calmmoney.core.categorize.Categorizer
import cz.calmmoney.core.categorize.MerchantText
import cz.calmmoney.data.db.RecordDao
import cz.calmmoney.data.db.RecordType
import cz.calmmoney.data.settings.CategoryRulesStore
import javax.inject.Inject
import javax.inject.Singleton

/** Učení a dávková aplikace pravidel kategorizace. */
@Singleton
class CategorizationRepository @Inject constructor(
    private val rulesStore: CategoryRulesStore,
    private val recordDao: RecordDao,
) {
    /** Naučená pravidla, nejdelší klíč první (specifičtější vyhrává). */
    suspend fun learnedSorted(): List<Pair<String, String>> =
        rulesStore.snapshot().entries
            .map { it.key to it.value }
            .sortedByDescending { it.first.length }

    /** Zapamatuje si `plátce → kategorie` z ručního zařazení uživatele. */
    suspend fun learn(payee: String?, categoryId: String?) {
        if (categoryId.isNullOrBlank()) return
        val key = MerchantText.key(payee)
        if (key.isNotBlank()) rulesStore.learn(key, categoryId)
    }

    /** Kolik dalších nezařazených záznamů má stejného obchodníka jako [payee]. */
    suspend fun countUncategorizedForMerchant(payee: String?): Int {
        val key = MerchantText.key(payee)
        if (key.isBlank()) return 0
        return recordDao.getUncategorized().count { MerchantText.key(it.payee) == key }
    }

    /** Zařadí všechny nezařazené záznamy od stejného obchodníka do [categoryId]; vrátí počet. */
    suspend fun applyToMerchant(payee: String?, categoryId: String): Int {
        val key = MerchantText.key(payee)
        if (key.isBlank()) return 0
        var n = 0
        for (r in recordDao.getUncategorized()) {
            if (MerchantText.key(r.payee) == key) {
                recordDao.update(r.copy(categoryId = categoryId, updatedAt = System.currentTimeMillis()))
                n++
            }
        }
        return n
    }

    /** Projede nezařazené příjmy/výdaje a zařadí, co jde; vrátí počet zařazených. */
    suspend fun recategorizeUncategorized(): Int {
        val learned = learnedSorted()
        var n = 0
        for (r in recordDao.getUncategorized()) {
            val res = Categorizer.categorize(
                payee = r.payee,
                note = r.note,
                txType = null,
                isIncome = r.type == RecordType.INCOME,
                ownerNorm = "",
                learned = learned,
            )
            if (res is Categorizer.Result.Category) {
                recordDao.update(r.copy(categoryId = res.id, updatedAt = System.currentTimeMillis()))
                n++
            }
        }
        return n
    }
}
