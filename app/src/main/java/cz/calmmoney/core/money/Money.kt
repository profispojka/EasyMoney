package cz.calmmoney.core.money

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/**
 * Formátování peněz. Aplikace je jednoměnová (CZK), částky jsou v minor units
 * (haléře jako Long). Žádný float — výpočty přes BigDecimal.
 */
object Money {

    private val LOCALE: Locale = Locale.forLanguageTag("cs-CZ")
    private const val MINUS = "−" // typografické minus

    /** Hodnota v hlavní jednotce (Kč) jako BigDecimal. */
    fun toMajor(amountMinor: Long): BigDecimal = BigDecimal(amountMinor).movePointLeft(2)

    /**
     * Naformátuje částku jako CZK. Při [withSign] přidá + / − a nezáporné/záporné
     * rozliší znaménkem (na E-Ink nese význam znaménko a tučnost, ne barva).
     */
    fun format(amountMinor: Long, withSign: Boolean = false): String {
        val nf = NumberFormat.getCurrencyInstance(LOCALE)
        val base = nf.format(toMajor(kotlin.math.abs(amountMinor)))
        return when {
            withSign && amountMinor > 0 -> "+ $base"
            amountMinor < 0 -> "$MINUS $base"
            else -> base
        }
    }

    /** Prostý editovatelný řetězec částky pro textové pole ("5000", "5000.5"). */
    fun toPlainAmount(amountMinor: Long): String =
        toMajor(amountMinor).stripTrailingZeros().toPlainString()

    /** Naparsuje text ("1234,50") na minor units. null = neplatné, prázdné = 0. */
    fun parseToMinor(text: String): Long? {
        val cleaned = text.filterNot { it.isWhitespace() }.replace(',', '.')
        if (cleaned.isEmpty()) return 0
        return try {
            BigDecimal(cleaned).movePointRight(2).setScale(0, RoundingMode.HALF_UP).toLong()
        } catch (e: NumberFormatException) {
            null
        }
    }
}
