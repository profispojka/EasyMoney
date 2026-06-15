package cz.calmmoney.core.categorize

/**
 * Jádro kategorizace. Čisté funkce, nezávislé na Roomu i na Fiu.
 * Priorita: vlastní převod → příjmová refundace → naučená pravidla → seed → heuristiky.
 */
object Categorizer {

    sealed interface Result {
        /** Vlastní převod mezi účty (jméno protistrany = majitel účtu) → typ TRANSFER, mimo statistiky. */
        data object Transfer : Result
        data class Category(val id: String) : Result
        data object None : Result
    }

    /**
     * @param learned naučená pravidla (klíč → categoryId), seřazená sestupně dle délky klíče.
     * @param ownerNorm normalizované jméno majitele účtu (pro detekci vlastních převodů), "" = nepoužít.
     */
    fun categorize(
        payee: String?,
        note: String?,
        txType: String?,
        isIncome: Boolean,
        ownerNorm: String,
        learned: List<Pair<String, String>>,
    ): Result {
        val payeeNorm = MerchantText.normalize(payee)

        // 1) Vlastní převod — protistrana = majitel účtu.
        if (ownerNorm.isNotEmpty() && payeeNorm == ownerNorm) return Result.Transfer

        val hay = (payeeNorm + " " + MerchantText.normalize(note)).trim()

        // 2) Příjmová refundace karetní platby („Kredit: …").
        if (isIncome && hay.contains("kredit")) return Result.Category("income_refunds")

        // 3) Naučená pravidla (přednost před seed).
        for ((kw, cat) in learned) {
            if (kw.isNotEmpty() && hay.contains(kw)) return Result.Category(cat)
        }

        // 4) Seed slovník (nejspecifičtější klíč první).
        for ((kw, cat) in SeedRules.sorted) {
            if (hay.contains(kw)) return Result.Category(cat)
        }

        // 5) Heuristiky dle typu pohybu.
        val t = MerchantText.normalize(txType)
        if (t.contains("poplatek")) return Result.Category("financial_fees")

        // Výběr z bankomatu apod. necháváme bez kategorie.
        return Result.None
    }
}
