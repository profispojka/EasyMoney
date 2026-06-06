# 05 — Architektura a tech stack

## Stack (potvrzeno)
- **Jazyk:** Kotlin
- **UI:** Jetpack Compose + Material 3 (světlý/tmavý motiv, dynamické barvy)
- **Min SDK:** 26 (Android 8.0), target nejnovější stabilní
- **Architektura:** MVVM + jednosměrný tok dat (UDF), repository vrstva

## Vrstvy

```
UI (Compose)  →  ViewModel (StateFlow UiState)  →  Repository  →  DAO/Room + BankConnector
        ▲ events                ▲ business logika           ▲ data
        └────────── stav ───────┘
```

- **UI / Compose:** bezstavové composable, stav z `collectAsStateWithLifecycle()`.
- **ViewModel:** drží `UiState` (`StateFlow`), zpracovává eventy, volá repozitáře.
- **Repository:** jediný zdroj pravdy, kombinuje Room + síť (Fio), business pravidla.
- **Data:** Room (DAO vrací `Flow`), Retrofit (Fio), DataStore (nastavení).

## Knihovny
| Oblast | Knihovna |
|---|---|
| DI | **Hilt** |
| DB | **Room** (+ KSP) |
| Async | **Coroutines + Flow** |
| Navigace | **Navigation-Compose** (typed routes) |
| Síť (Fio) | **Retrofit + OkHttp** + **kotlinx.serialization** |
| Background sync | **WorkManager** (periodic + one-time) |
| Nastavení | **DataStore (Preferences)** |
| Bezpečnost | **Jetpack Security** (EncryptedSharedPreferences) + Keystore, **BiometricPrompt** |
| Grafy | **Vico** (Compose-native) pro cash flow/trend; vlastní Canvas donut |
| Obrázky | **Coil** (foto účtenek, záruky) |
| Peníze | `BigDecimal` + `java.util.Currency` / ICU `NumberFormat` |
| Testy | JUnit, **Turbine** (Flow), Room in-memory, Compose UI test, MockWebServer (Fio) |

## Struktura projektu (start: 1 modul, připraveno na rozdělení)
```
app/
 ├─ core/            (design system, theme, peníze, utily, navigace)
 ├─ data/
 │   ├─ db/          (Room: entity, DAO, migrace)
 │   ├─ datastore/   (nastavení)
 │   ├─ fio/         (Retrofit API, DTO, mapper, BankConnector)
 │   └─ repo/        (repozitáře)
 ├─ domain/          (modely, use-cases, pravidla)
 └─ feature/
     ├─ dashboard/  records/  accounts/  budgets/  analytics/
     ├─ planned/  debts/  goals/  shopping/  warranties/
     ├─ fio/        (připojení, stav sync)
     └─ settings/  onboarding/  lock/
```
> Pozdější přechod na multi-modul (`:core`, `:data`, `:feature-*`) kvůli rychlosti buildu.

## Klíčová rozhodnutí
- **Offline-first:** žádný backend; všechna data lokálně. Sync je samostatná pozdější vrstva.
- **Reaktivně:** DAO `Flow` → ViewModel `StateFlow` → Compose; UI se samo aktualizuje
  po importu z Fio i ručním zápisu.
- **Agregace v SQL:** součty/reporty přes `SUM/GROUP BY`, ne v paměti.
- **Peníze přesně:** minor units (`Long`) + měna; `BigDecimal` na výpočty.
- **Bezpečnost:** Fio token jen v šifrovaném úložišti; volitelný zámek appky.
- **i18n od začátku:** všechny texty ve `strings.xml` (cs + en).

## Testovací strategie (zkráceně)
- **Unit:** mapper Fio DTO → Record, výpočty zůstatků/rozpočtů, kategorizační pravidla.
- **DAO:** Room in-memory (dedup dle `fioTransactionId`, agregace).
- **Síť:** MockWebServer s ukázkovými Fio JSON (vč. 409, prázdná data, chybějící pole).
- **UI:** smoke testy klíčových toků (přidat transakci, vytvořit účet).
- Detailní plán v případné fázi testů (viz roadmap).

## CI/CD (volitelně, později)
- GitHub Actions: build + unit testy + lint na každý push.
- Podepsané release buildy, distribuce přes interní track / Play Console.
