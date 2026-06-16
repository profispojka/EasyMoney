package cz.calmmoney.core.recurring

import cz.calmmoney.core.categorize.MerchantText
import cz.calmmoney.core.time.PlannedPayments
import cz.calmmoney.data.db.PlannedPaymentEntity
import cz.calmmoney.data.db.RecordEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

/**
 * Párování plánovaných plateb se skutečnými transakcemi.
 *
 * Plánovaná platba „zmizí" z nadcházejících, jakmile je její splatný výskyt zaplacený. Auto-párování
 * najde transakci se **stejným obchodníkem**, **přibližně stejnou částkou** a **datem kolem očekávaného
 * výskytu** → posune „zaplaceno do". Co takhle napojit nejde (jiný/žádný obchodník), řeší uživatel ručně.
 */
object PlannedMatcher {
    private val zone: ZoneId = ZoneId.systemDefault()

    private const val DAYS_BEFORE = 8L   // transakce smí přijít pár dní před očekávaným dnem
    private const val DAYS_AFTER = 12L   // …nebo o něco později
    private const val FUTURE_LIMIT = 3L  // budoucí výskyty (víc než pár dní dopředu) se nepárují

    fun epochDayOf(epochMillis: Long): Long =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate().toEpochDay()

    private fun amountTolerance(amountMinor: Long): Long = maxOf(500L, amountMinor * 3 / 100)

    /** Sedí záznam [r] na výskyt platby [p] očekávaný v den [dueEpochDay]? */
    fun matches(r: RecordEntity, p: PlannedPaymentEntity, dueEpochDay: Long): Boolean {
        if (r.type != p.type) return false
        if (MerchantText.key(r.payee) != MerchantText.key(p.name)) return false
        if (abs(r.amountMinor - p.amountMinor) > amountTolerance(p.amountMinor)) return false
        val d = epochDayOf(r.dateTime)
        return d in (dueEpochDay - DAYS_BEFORE)..(dueEpochDay + DAYS_AFTER)
    }

    /** Lze tuhle platbu vůbec auto-napojit? (existuje transakce se stejným obchodníkem) */
    fun isAutoLinkable(p: PlannedPaymentEntity, records: List<RecordEntity>): Boolean {
        val key = MerchantText.key(p.name)
        return records.any { it.type == p.type && MerchantText.key(it.payee) == key }
    }

    /**
     * Posune „zaplaceno do" co nejdál podle existujících transakcí. Vrací nové `paidThroughEpochDay`
     * nebo null (žádná změna). Postupuje výskyt po výskytu — dožene i víc měsíců najednou.
     */
    fun reconcile(p: PlannedPaymentEntity, records: List<RecordEntity>, today: LocalDate): Long? {
        val key = MerchantText.key(p.name)
        val sameMerchant = records.filter { it.type == p.type && MerchantText.key(it.payee) == key }
        if (sameMerchant.isEmpty()) return null

        var paid = p.paidThroughEpochDay
        var changed = false
        var guard = 0
        while (guard++ < 120) {
            val due = PlannedPayments.dueOccurrence(
                p.startEpochDay, p.frequencyUnit, p.frequencyCount, p.endEpochDay, paid,
            ) ?: break
            if (due.isAfter(today.plusDays(FUTURE_LIMIT))) break
            val dueEd = due.toEpochDay()
            if (sameMerchant.none { matches(it, p, dueEd) }) break
            paid = dueEd
            changed = true
        }
        return if (changed) paid else null
    }
}
