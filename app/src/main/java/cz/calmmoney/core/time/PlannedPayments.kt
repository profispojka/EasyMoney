package cz.calmmoney.core.time

import cz.calmmoney.data.db.FrequencyUnit
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Výpočty výskytů opakovaných (plánovaných) plateb. Datum-only (epoch day). */
object PlannedPayments {

    private val dateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d. M. yyyy", Locale.forLanguageTag("cs-CZ"))

    fun frequencyLabel(unit: FrequencyUnit, count: Int): String = when {
        unit == FrequencyUnit.DAY && count == 1 -> "Denně"
        unit == FrequencyUnit.WEEK && count == 1 -> "Týdně"
        unit == FrequencyUnit.MONTH && count == 1 -> "Měsíčně"
        unit == FrequencyUnit.MONTH && count == 3 -> "Čtvrtletně (4× ročně)"
        unit == FrequencyUnit.MONTH && count == 4 -> "3× ročně"
        unit == FrequencyUnit.MONTH && count == 6 -> "Pololetně (2× ročně)"
        unit == FrequencyUnit.YEAR && count == 1 -> "Ročně"
        else -> when (unit) {
            FrequencyUnit.DAY -> "Každých $count dní"
            FrequencyUnit.WEEK -> "Každých $count týdnů"
            FrequencyUnit.MONTH -> "Každých $count měsíců"
            FrequencyUnit.YEAR -> "Každých $count let"
        }
    }

    private fun step(date: LocalDate, unit: FrequencyUnit, count: Int): LocalDate = when (unit) {
        FrequencyUnit.DAY -> date.plusDays(count.toLong())
        FrequencyUnit.WEEK -> date.plusWeeks(count.toLong())
        FrequencyUnit.MONTH -> date.plusMonths(count.toLong())
        FrequencyUnit.YEAR -> date.plusYears(count.toLong())
    }

    /** První výskyt v den [from] nebo později (respektuje konec). null = už neproběhne. */
    fun nextOccurrence(
        startEpochDay: Long,
        unit: FrequencyUnit,
        count: Int,
        endEpochDay: Long?,
        from: LocalDate = LocalDate.now(),
    ): LocalDate? {
        var d = LocalDate.ofEpochDay(startEpochDay)
        val end = endEpochDay?.let { LocalDate.ofEpochDay(it) }
        if (d.isBefore(from)) d = fastForward(d, unit, count, from)
        // doladění (between u WEEK/MONTH/YEAR může podstřelit o jeden interval)
        var guard = 0
        while (d.isBefore(from) && guard < 1000) { d = step(d, unit, count); guard++ }
        if (end != null && d.isAfter(end)) return null
        return d
    }

    /** Počet výskytů platby v kalendářním měsíci [ym]. */
    fun occurrencesInMonth(
        startEpochDay: Long,
        unit: FrequencyUnit,
        count: Int,
        endEpochDay: Long?,
        ym: YearMonth,
    ): Int {
        val rangeStart = ym.atDay(1)
        val rangeEnd = ym.plusMonths(1).atDay(1) // exkluzivně
        val end = endEpochDay?.let { LocalDate.ofEpochDay(it) }
        var d = LocalDate.ofEpochDay(startEpochDay)
        if (d.isBefore(rangeStart)) d = fastForward(d, unit, count, rangeStart)
        var occ = 0
        var guard = 0
        while (d.isBefore(rangeEnd) && guard < 5000) {
            if (!d.isBefore(rangeStart) && (end == null || !d.isAfter(end))) occ++
            d = step(d, unit, count)
            guard++
        }
        return occ
    }

    fun formatDate(date: LocalDate): String = dateFormatter.format(date)

    /** Skok dopředu k prvnímu výskytu >= [target] (bez kroku po jednom). */
    private fun fastForward(start: LocalDate, unit: FrequencyUnit, count: Int, target: LocalDate): LocalDate {
        val between = when (unit) {
            FrequencyUnit.DAY -> ChronoUnit.DAYS.between(start, target)
            FrequencyUnit.WEEK -> ChronoUnit.WEEKS.between(start, target)
            FrequencyUnit.MONTH -> ChronoUnit.MONTHS.between(start, target)
            FrequencyUnit.YEAR -> ChronoUnit.YEARS.between(start, target)
        }
        if (between <= 0) return start
        val steps = between / count // může podstřelit; doladí volající smyčka
        return when (unit) {
            FrequencyUnit.DAY -> start.plusDays(steps * count)
            FrequencyUnit.WEEK -> start.plusWeeks(steps * count)
            FrequencyUnit.MONTH -> start.plusMonths(steps * count)
            FrequencyUnit.YEAR -> start.plusYears(steps * count)
        }
    }
}
