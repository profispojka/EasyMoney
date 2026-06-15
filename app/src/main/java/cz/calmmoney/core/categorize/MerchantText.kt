package cz.calmmoney.core.categorize

import java.text.Normalizer

/**
 * Normalizace textu obchodníka pro porovnávání a učení.
 * Obecné (nezávislé na bance): malá písmena, bez diakritiky, jen [a-z0-9] + mezery.
 */
object MerchantText {

    private val diacritics = Regex("\\p{M}+")
    private val nonAlnum = Regex("[^a-z0-9]+")

    /** „ALBERT VÁM DĚKUJE" → „albert vam dekuje". */
    fun normalize(s: String?): String {
        if (s.isNullOrBlank()) return ""
        val stripped = Normalizer.normalize(s, Normalizer.Form.NFD).replace(diacritics, "")
        return stripped.lowercase().replace(nonAlnum, " ").trim()
    }

    /**
     * Klíč pro naučené pravidlo: normalizováno a bez „kódových" tokenů (číselné kódy,
     * krátké zkratky), aby „ZABKA Z8921 K.1" i „ZABKA ZC451 K.1" daly stejný klíč „zabka".
     */
    fun key(s: String?): String {
        val norm = normalize(s)
        if (norm.isEmpty()) return ""
        val tokens = norm.split(' ').filter { t ->
            t.length >= 3 && t.none { it.isDigit() } && t !in STOP
        }
        return tokens.joinToString(" ").ifBlank { norm }
    }

    private val STOP = setOf(
        "sro", "spol", "kasa", "pos", "kiosk", "www", "platba", "kartou", "nakup",
        "the", "and", "und",
    )
}
