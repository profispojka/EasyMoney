package cz.calmmoney.core.time

import cz.calmmoney.data.db.BudgetPeriod
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/** Pomocné výpočty časových oken (epoch millis, lokální zóna). */
object Periods {
    private val zone: ZoneId = ZoneId.systemDefault()
    private val monthFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("LLLL yyyy", Locale.forLanguageTag("cs-CZ"))

    /** Název měsíce v nominativu, např. „srpen 2026". */
    fun monthName(ym: YearMonth): String = monthFormatter.format(ym)

    /** Popisek měsíce pro přepínač: „Tento měsíc" pro aktuální, jinak název měsíce. */
    fun monthLabel(ym: YearMonth, today: LocalDate = LocalDate.now()): String =
        if (ym == YearMonth.from(today)) "Tento měsíc" else monthName(ym)

    private fun startMillis(date: LocalDate): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    /** Aktuální okno pro období rozpočtu: [start, endExclusive). */
    fun currentWindow(period: BudgetPeriod, today: LocalDate = LocalDate.now()): Pair<Long, Long> {
        return when (period) {
            BudgetPeriod.WEEK -> {
                val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                startMillis(start) to startMillis(start.plusWeeks(1))
            }
            BudgetPeriod.MONTH -> {
                val ym = YearMonth.from(today)
                startMillis(ym.atDay(1)) to startMillis(ym.plusMonths(1).atDay(1))
            }
            BudgetPeriod.YEAR -> {
                val start = today.withDayOfYear(1)
                startMillis(start) to startMillis(start.plusYears(1))
            }
        }
    }

    /** Okno kalendářního měsíce. */
    fun monthWindow(ym: YearMonth): Pair<Long, Long> =
        startMillis(ym.atDay(1)) to startMillis(ym.plusMonths(1).atDay(1))

    /** Posledních [n] měsíců včetně aktuálního (od nejstaršího). */
    fun lastMonths(n: Int, today: LocalDate = LocalDate.now()): List<YearMonth> {
        val current = YearMonth.from(today)
        return (n - 1 downTo 0).map { current.minusMonths(it.toLong()) }
    }
}
