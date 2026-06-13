# 05 — Architektura a tech stack

## Stack (potvrzeno)
- **Jazyk:** Kotlin
- **UI:** Jetpack Compose + Material 3 přebarvené na **monochrom** (černobílý E-Ink);
  bez dynamických barev a bez animací — viz [08-design-eink.md](08-design-eink.md)
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
| Bezpečnost | **Jetpack Security** (EncryptedSharedPreferences) + Keystore (Fio token) |
| Grafy | **Vico** (monochrom, statické, bez animací) pro cash flow/trend; žebříček (pruhy) místo donutu |
| Peníze | `BigDecimal` + `NumberFormat` (locale `cs-CZ`, vždy CZK) |
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
     ├─ planned/  templates/
     ├─ fio/        (připojení, stav sync)
     └─ settings/  onboarding/
```
> Pozdější přechod na multi-modul (`:core`, `:data`, `:feature-*`) kvůli rychlosti buildu.

## Klíčová rozhodnutí
- **Offline-first:** žádný backend; všechna data lokálně. Sync je samostatná pozdější vrstva.
- **Reaktivně:** DAO `Flow` → ViewModel `StateFlow` → Compose; UI se samo aktualizuje
  po importu z Fio i ručním zápisu.
- **Agregace v SQL:** součty/reporty přes `SUM/GROUP BY`, ne v paměti.
- **Peníze přesně:** minor units (`Long`), jednotná měna CZK; `BigDecimal` na výpočty.
- **Bezpečnost:** Fio token jen v šifrovaném úložišti.
- **E-Ink monochrom:** vlastní černobílé téma (override Material 3), **žádné animace**
  ani přechody, ploché orámované komponenty (bez stínů/elevace), statické grafy,
  okamžité překreslení obrazovek, místo pull-to-refresh tlačítko. Detail v
  [08-design-eink.md](08-design-eink.md). Cílové zařízení = **Mudita Kompakt**
  (E-Ink Android, potvrzeno).
- **Jen čeština:** UI výhradně v češtině, žádné i18n. Texty drženy ve `strings.xml`
  (jen `values/`, bez dalších jazykových variant) kvůli udržovatelnosti.

## Testovací strategie (zkráceně)
- **Unit:** mapper Fio DTO → Record, výpočty zůstatků/rozpočtů, kategorizační pravidla.
- **DAO:** Room in-memory (dedup dle `fioTransactionId`, agregace).
- **Síť:** MockWebServer s ukázkovými Fio JSON (vč. 409, prázdná data, chybějící pole).
- **UI:** smoke testy klíčových toků (přidat transakci, vytvořit účet).
- Detailní plán v případné fázi testů (viz roadmap).

## CI/CD (volitelně, později)
- GitHub Actions: build + unit testy + lint na každý push.
- Podepsané release buildy, distribuce přes interní track / Play Console.
