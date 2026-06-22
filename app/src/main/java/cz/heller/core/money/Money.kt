package cz.heller.core.money

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/**
 * Formátování peněz. Aplikace je jednoměnová — měnu si uživatel zvolí na začátku (kosmetické,
 * částky jsou vždy v minor units ×100). Číselný formát je český (mezera tisíce, čárka desetiny),
 * mění se jen symbol měny. Žádný float — výpočty přes BigDecimal.
 */
object Money {

    private val LOCALE: Locale = Locale.forLanguageTag("cs-CZ")
    private const val MINUS = "−" // typografické minus

    /** Symbol zvolené měny (Kč/€/$/£/CHF…). Nastavuje se při startu z nastavení (viz [applyCurrency]). */
    @Volatile
    var currencySymbol: String = AppCurrency.CZK.symbol

    /** Nastaví zvolenou měnu podle kódu (viz [AppCurrency]). */
    fun applyCurrency(code: String?) {
        currencySymbol = AppCurrency.fromCode(code).symbol
    }

    /** Hodnota v hlavní jednotce jako BigDecimal. */
    fun toMajor(amountMinor: Long): BigDecimal = BigDecimal(amountMinor).movePointLeft(2)

    /**
     * Naformátuje částku se symbolem zvolené měny. Při [withSign] přidá + / − a nezáporné/záporné
     * rozliší znaménkem (na E-Ink nese význam znaménko a tučnost, ne barva).
     */
    fun format(amountMinor: Long, withSign: Boolean = false): String {
        val nf = NumberFormat.getNumberInstance(LOCALE).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        val base = nf.format(toMajor(kotlin.math.abs(amountMinor))) + " " + currencySymbol
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
