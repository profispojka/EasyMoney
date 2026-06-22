package cz.heller.core.money

/** Podporované měny (jen zobrazení — částky se ukládají v minor units ×100 pro všechny). */
enum class AppCurrency(val code: String, val symbol: String) {
    CZK("CZK", "Kč"),
    EUR("EUR", "€"),
    USD("USD", "$"),
    GBP("GBP", "£"),
    CHF("CHF", "CHF"),
    PLN("PLN", "zł"),
    SEK("SEK", "kr"),
    NOK("NOK", "kr"),
    DKK("DKK", "kr"),
    HUF("HUF", "Ft");

    companion object {
        fun fromCode(code: String?): AppCurrency = entries.firstOrNull { it.code == code } ?: CZK
    }
}
