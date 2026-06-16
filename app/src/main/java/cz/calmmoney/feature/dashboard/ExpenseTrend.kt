package cz.calmmoney.feature.dashboard

import cz.calmmoney.core.time.Periods
import cz.calmmoney.data.db.RecordEntity
import cz.calmmoney.data.db.RecordType
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/** Období grafu vývoje výdajů. */
enum class TrendPeriod(val label: String) {
    DAYS_30("30 dní"),
    WEEKS_12("12 týdnů"),
    MONTHS_6("6 měsíců"),
    YEAR("1 rok"),
}

data class TrendResult(
    val points: List<Long>,
    val axisLabels: List<String>,
    val totalMinor: Long,
    /** Změna oproti předchozímu stejně dlouhému období (v %), null = nelze spočítat. */
    val changePct: Int?,
)

/** Výpočet vývoje výdajů po obdobích (denní / týdenní / měsíční buckety) + % změna. */
object ExpenseTrend {

    private val zone: ZoneId = ZoneId.systemDefault()
    private val dayFmt = DateTimeFormatter.ofPattern("d.M.", Locale.forLanguageTag("cs-CZ"))

    fun compute(period: TrendPeriod, records: List<RecordEntity>, today: LocalDate = LocalDate.now()): TrendResult {
        val buckets = buckets(period, records, today)
        val total = buckets.sumOf { it.second }
        val prev = previousTotal(period, records, today)
        val pct = if (prev > 0) (((total - prev) * 100) / prev).toInt() else null
        return TrendResult(
            points = buckets.map { it.second },
            axisLabels = thinLabels(buckets.map { it.first }),
            totalMinor = total,
            changePct = pct,
        )
    }

    /** Páry (popisek, součet výdajů) v daném období, od nejstaršího po nejnovější. */
    private fun buckets(period: TrendPeriod, records: List<RecordEntity>, today: LocalDate): List<Pair<String, Long>> =
        when (period) {
            TrendPeriod.MONTHS_6 -> monthBuckets(records, today, 6)
            TrendPeriod.YEAR -> monthBuckets(records, today, 12)
            TrendPeriod.WEEKS_12 -> spanBuckets(records, today, count = 12, unitDays = 7)
            TrendPeriod.DAYS_30 -> spanBuckets(records, today, count = 30, unitDays = 1)
        }

    private fun monthBuckets(records: List<RecordEntity>, today: LocalDate, n: Int): List<Pair<String, Long>> =
        Periods.lastMonths(n, today).map { ym ->
            val (s, e) = Periods.monthWindow(ym)
            Periods.monthShort(ym) to sumExpense(records, s, e)
        }

    private fun spanBuckets(records: List<RecordEntity>, today: LocalDate, count: Int, unitDays: Int): List<Pair<String, Long>> =
        (0 until count).map { i ->
            val endDate = today.minusDays((count - 1 - i).toLong() * unitDays)
            val startDate = endDate.minusDays((unitDays - 1).toLong())
            dayFmt.format(startDate) to daysSum(records, startDate, endDate)
        }

    /** Součet výdajů v předchozím stejně dlouhém období (pro % změnu). */
    private fun previousTotal(period: TrendPeriod, records: List<RecordEntity>, today: LocalDate): Long =
        when (period) {
            TrendPeriod.MONTHS_6 -> monthsAgoSum(records, today, fromAgo = 12, count = 6)
            TrendPeriod.YEAR -> monthsAgoSum(records, today, fromAgo = 24, count = 12)
            TrendPeriod.WEEKS_12 -> daysSum(records, today.minusDays(167), today.minusDays(84))
            TrendPeriod.DAYS_30 -> daysSum(records, today.minusDays(59), today.minusDays(30))
        }

    private fun monthsAgoSum(records: List<RecordEntity>, today: LocalDate, fromAgo: Int, count: Int): Long {
        val cur = YearMonth.from(today)
        var sum = 0L
        for (k in (fromAgo - 1) downTo (fromAgo - count)) {
            val (s, e) = Periods.monthWindow(cur.minusMonths(k.toLong()))
            sum += sumExpense(records, s, e)
        }
        return sum
    }

    private fun daysSum(records: List<RecordEntity>, startDate: LocalDate, endDateInclusive: LocalDate): Long =
        sumExpense(records, startMillis(startDate), startMillis(endDateInclusive.plusDays(1)))

    private fun sumExpense(records: List<RecordEntity>, sMillis: Long, eMillis: Long): Long =
        records.asSequence()
            .filter { it.type == RecordType.EXPENSE && it.dateTime in sMillis until eMillis }
            .sumOf { it.amountMinor }

    private fun startMillis(d: LocalDate): Long = d.atStartOfDay(zone).toInstant().toEpochMilli()

    /** Z mnoha popisků vybere ~6 rovnoměrně (vč. prvního a posledního), ať osa není přeplněná. */
    private fun thinLabels(labels: List<String>, max: Int = 6): List<String> {
        if (labels.size <= max) return labels
        val step = (labels.size - 1).toFloat() / (max - 1)
        return (0 until max).map { labels[(it * step).roundToInt()] }
    }
}
