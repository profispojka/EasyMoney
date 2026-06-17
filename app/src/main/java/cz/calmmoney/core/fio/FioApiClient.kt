package cz.calmmoney.core.fio

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/** Výsledek HTTP volání Fio API. */
sealed interface FioFetchResult {
    data class Success(val json: String) : FioFetchResult
    /** Fio dovolí jen 1 dotaz za 30 s (HTTP 409). */
    data object RateLimited : FioFetchResult
    data class HttpError(val code: Int) : FioFetchResult
    data class NetworkError(val message: String) : FioFetchResult
}

/**
 * Klient pro Fio „API Bankovnictví" (read-only). Token nese oprávnění „pouhé monitorování
 * účtu" — z tohoto kanálu nelze poslat platbu. Pouze čtení přes HTTPS, žádná knihovna navíc.
 */
@Singleton
class FioApiClient @Inject constructor() {

    private val iso: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /** Stáhne pohyby za období [from]..[to] (včetně) jako JSON. */
    suspend fun fetchPeriods(token: String, from: LocalDate, to: LocalDate): FioFetchResult =
        withContext(Dispatchers.IO) {
            val url = "https://fioapi.fio.cz/v1/rest/periods/$token/" +
                "${iso.format(from)}/${iso.format(to)}/transactions.json"
            try {
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 30_000
                    readTimeout = 90_000 // velký výpis (stovky pohybů) na pomalé síti
                    setRequestProperty("Accept", "application/json")
                }
                try {
                    when (val code = conn.responseCode) {
                        HttpURLConnection.HTTP_OK ->
                            FioFetchResult.Success(conn.inputStream.bufferedReader().use { it.readText() })
                        409 -> FioFetchResult.RateLimited
                        else -> FioFetchResult.HttpError(code)
                    }
                } finally {
                    conn.disconnect()
                }
            } catch (e: Exception) {
                FioFetchResult.NetworkError(e.message ?: "Síťová chyba")
            }
        }
}
