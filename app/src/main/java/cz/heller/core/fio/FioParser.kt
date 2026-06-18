package cz.heller.core.fio

import org.json.JSONObject
import java.time.LocalDate

/** Jeden pohyb z Fio výpisu. amount je znaménkové, v hlavní jednotce (Kč). */
data class FioTx(
    val id: Long,
    val date: LocalDate,
    val amount: Double,
    val currency: String?,
    val counterAccount: String?,
    val counterName: String?,
    val variableSymbol: String?,
    val message: String?,
    val userIdentification: String?,
    val comment: String?,
    val type: String?,
    /** „Provedl" (column9) — typicky majitel účtu; pro detekci vlastních převodů. */
    val performedBy: String?,
)

/**
 * Parser JSON odpovědi Fio API. Pohyb je objekt s poli `columnN` { value, name, id }:
 * 0=datum, 1=objem, 2=protiúčet, 5=VS, 10=název protiúčtu, 14=měna, 16=zpráva, 22=ID pohybu.
 */
object FioParser {

    private fun str(tx: JSONObject, key: String): String? {
        val c = tx.optJSONObject(key) ?: return null
        if (c.isNull("value")) return null
        return c.get("value").toString().trim().ifEmpty { null }
    }

    private fun num(tx: JSONObject, key: String): Double? {
        val c = tx.optJSONObject(key) ?: return null
        if (c.isNull("value")) return null
        val d = c.optDouble("value", Double.NaN)
        return if (d.isNaN()) null else d
    }

    /** Číslo vlastního účtu z výpisu (info.accountId) — pro detekci převodů mezi vlastními účty. */
    fun accountNumber(json: String): String? = runCatching {
        JSONObject(json).optJSONObject("accountStatement")
            ?.optJSONObject("info")?.optString("accountId")?.trim()?.ifBlank { null }
    }.getOrNull()

    /** Skutečný (běžný) zůstatek účtu ke konci výpisu v minor units — z info.closingBalance. */
    fun closingBalanceMinor(json: String): Long? = runCatching {
        val info = JSONObject(json).optJSONObject("accountStatement")?.optJSONObject("info") ?: return null
        if (info.isNull("closingBalance")) return null
        // Přes string, ať nezavedeme chybu z double (16407.51 vs 16407.5099…).
        java.math.BigDecimal(info.get("closingBalance").toString())
            .movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).toLong()
    }.getOrNull()

    fun parse(json: String): List<FioTx> {
        val stmt = JSONObject(json).optJSONObject("accountStatement") ?: return emptyList()
        val arr = stmt.optJSONObject("transactionList")?.optJSONArray("transaction") ?: return emptyList()
        val out = ArrayList<FioTx>(arr.length())
        for (i in 0 until arr.length()) {
            val tx = arr.optJSONObject(i) ?: continue
            val id = str(tx, "column22")?.substringBefore('.')?.toLongOrNull() ?: continue
            val amount = num(tx, "column1") ?: continue
            val dateRaw = str(tx, "column0") ?: continue
            val date = runCatching { LocalDate.parse(dateRaw.substring(0, 10)) }.getOrNull() ?: continue
            out += FioTx(
                id = id,
                date = date,
                amount = amount,
                currency = str(tx, "column14"),
                counterAccount = str(tx, "column2"),
                counterName = str(tx, "column10"),
                variableSymbol = str(tx, "column5"),
                message = str(tx, "column16"),
                userIdentification = str(tx, "column7"),
                comment = str(tx, "column25"),
                type = str(tx, "column8"),
                performedBy = str(tx, "column9"),
            )
        }
        return out
    }
}
