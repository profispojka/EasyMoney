# 03 — Datový model

Lokální databáze **Room** (SQLite). Vše offline; schéma připravené na pozdější cloud
sync (každá entita má `id` typu UUID/String, `createdAt`, `updatedAt`, příznak `deleted`
pro budoucí soft-delete sync).

## Zásady pro peníze a čas
- **Částky:** `amountMinor: Long` (haléře). Měna je vždy **CZK** — neukládá se
  do entit, žádné přepočty ani kurzy. Nikdy `float`/`double`. Výpočty přes
  `BigDecimal`, formátování přes `NumberFormat` (locale `cs-CZ`).
- **Čas:** ukládat jako epoch millis (UTC) + při zobrazení lokální zóna. Datum-only
  pole (splatnost) jako `LocalDate` (ISO string).

## Entity (přehled)

### Account
| pole | typ | pozn. |
|---|---|---|
| id | String (UUID) | PK |
| name | String | |
| type | enum | CASH, CHECKING, CREDIT_CARD, SAVINGS, INVESTMENT, OTHER |
| initialBalanceMinor | Long | počáteční zůstatek (CZK) |
| icon | String | klíč ikony (monochrom, bez barvy) |
| excludeFromStats | Boolean | |
| archived | Boolean | |
| sortOrder | Int | |
| createdAt / updatedAt | Long | |

> Aktuální zůstatek se **nepočítá do sloupce**, ale dopočítává dotazem
> (`initialBalance + Σ transakcí`), nebo se drží cache pole aktualizované při zápisu.

### Category
| pole | typ | pozn. |
|---|---|---|
| id | String | PK |
| name | String | |
| type | enum | INCOME, EXPENSE |
| parentId | String? | podkategorie |
| icon | String | monochrom, bez barvy |
| sortOrder | Int | |
| isDefault | Boolean | z přednastavené sady |

### Record (transakce)
| pole | typ | pozn. |
|---|---|---|
| id | String | PK |
| type | enum | EXPENSE, INCOME, TRANSFER |
| accountId | String | FK → Account |
| categoryId | String? | FK → Category (null u převodu) |
| amountMinor | Long | u výdaje záporné? → viz pozn. (CZK) |
| dateTime | Long | epoch millis |
| payee | String? | příjemce/plátce |
| note | String? | |
| paymentType | enum? | CASH, CARD, TRANSFER, OTHER |
| transferAccountId | String? | druhý účet u převodu |
| transferRecordId | String? | spárovaný protizáznam |
| plannedPaymentId | String? | původ z plánované platby |
| source | enum | MANUAL, FIO, IMPORT |
| fioTransactionId | Long? | ID pohybu z Fio (dedup) |
| createdAt / updatedAt | Long | |

> **Znaménko:** doporučení — ukládat `amountMinor` vždy kladně a směr odvozovat z `type`
> (čistší dotazy). Alternativa: znaménkově (výdaj záporný). Rozhodneme v
> [07-otevrene-otazky.md](07-otevrene-otazky.md).
> **Převod:** modelován jako 2 propojené Record (odchozí na účtu A, příchozí na účtu B)
> přes `transferRecordId`, nebo jako 1 Record s `transferAccountId`. Volba ovlivní reporty.

### Budget
| pole | typ | pozn. |
|---|---|---|
| id, name | | |
| categoryIds | List<String> | serializované / přes cross-ref |
| amountMinor | | limit (CZK) |
| period | enum | WEEK, MONTH, YEAR, CUSTOM |
| startDate | LocalDate | |
| rollover | Boolean | *P1* |
| notifyThresholdPct | Int | např. 80 |

### PlannedPayment
- `id, name, type (EXPENSE/INCOME), accountId, categoryId?, amountMinor, note?`
- Frekvence obecně: `frequencyUnit (DAY/WEEK/MONTH/YEAR) + frequencyCount` (např. 2× ročně = MONTH×6).
- `startEpochDay` (kotva/první výskyt), `endEpochDay?` (volitelný konec).
- Bez připomínek a bez auto-vytváření záznamu (vědomě). „Zaplatit teď“ vytvoří reálný záznam ručně.

### Template *(P1)*
- `Template(id, name, type, accountId, categoryId, amountMinor?, payee?, note?)`.

> **Měna:** aplikace je jednoměnová (CZK). Žádné entity pro měny ani kurzy.

### FioConnection
| pole | typ | pozn. |
|---|---|---|
| id | String | PK |
| accountId | String | FK → Account (kam importovat) |
| tokenAlias | String | klíč do šifrovaného úložiště (token NENÍ v Room) |
| displayName | String | |
| lastSyncAt | Long? | |
| lastTransactionId | Long? | pointer pro `last/` |
| lastDownloadDate | LocalDate? | |
| syncEnabled | Boolean | |
| syncIntervalHours | Int | např. 6 |
| lastError | String? | |

> **Token nikdy neukládáme do Room.** Drží se v `EncryptedSharedPreferences`
> (Android Keystore), v Room je jen alias/klíč. Detail v [04-fio-konektor.md](04-fio-konektor.md).

### CategorizationRule *(P1)*
- `Rule(id, matchField[PAYEE/NOTE/VS/COUNTER_ACCOUNT], operator[CONTAINS/EQUALS], value, categoryId, priority)`.

### Settings (DataStore, ne Room)
- theme, firstDayOfWeek, firstDayOfMonth, onboardingDone, notif flags.

## Vztahy (zjednodušeně)

```
Account 1───* Record *───1 Category
Account 1───* FioConnection
Budget  *───* Category
```

## Indexy a výkon
- Indexy na `Record(accountId)`, `Record(categoryId)`, `Record(dateTime)`,
  **unikátní** `Record(fioTransactionId)` (kde not null) kvůli deduplikaci.
- Agregace (součty, reporty) přes SQL `SUM/GROUP BY` v DAO, ne v paměti.
- DAO vrací `Flow<...>` → UI reaktivně překresluje.

## Migrace a zálohy
- Verzované Room migrace.
- Lokální záloha/obnova = export celé DB + nastavení do souboru a zpětné načtení. *(P1)*
