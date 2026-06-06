# 03 — Datový model

Lokální databáze **Room** (SQLite). Vše offline; schéma připravené na pozdější cloud
sync (každá entita má `id` typu UUID/String, `createdAt`, `updatedAt`, příznak `deleted`
pro budoucí soft-delete sync).

## Zásady pro peníze a čas
- **Částky:** `amountMinor: Long` (haléře/centy) + `currency: String` (ISO 4217).
  Nikdy `float`/`double`. Výpočty přes `BigDecimal`, formátování přes `NumberFormat`.
- **Čas:** ukládat jako epoch millis (UTC) + při zobrazení lokální zóna. Datum-only
  pole (splatnost) jako `LocalDate` (ISO string).

## Entity (přehled)

### Account
| pole | typ | pozn. |
|---|---|---|
| id | String (UUID) | PK |
| name | String | |
| type | enum | CASH, CHECKING, CREDIT_CARD, SAVINGS, INVESTMENT, OTHER |
| currency | String | ISO 4217 |
| initialBalanceMinor | Long | počáteční zůstatek |
| color | Int | ARGB |
| icon | String | klíč ikony |
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
| color | Int | |
| icon | String | |
| sortOrder | Int | |
| isDefault | Boolean | z přednastavené sady |

### Record (transakce)
| pole | typ | pozn. |
|---|---|---|
| id | String | PK |
| type | enum | EXPENSE, INCOME, TRANSFER |
| accountId | String | FK → Account |
| categoryId | String? | FK → Category (null u převodu) |
| amountMinor | Long | u výdaje záporné? → viz pozn. |
| currency | String | měna účtu |
| dateTime | Long | epoch millis |
| payee | String? | příjemce/plátce |
| note | String? | |
| paymentType | enum? | CASH, CARD, TRANSFER, OTHER |
| photoUri | String? | *P1* |
| latitude / longitude | Double? | *P1* |
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

### Label / RecordLabelCrossRef *(P1)*
- `Label(id, name, color)`
- `RecordLabelCrossRef(recordId, labelId)` — M:N.

### Budget
| pole | typ | pozn. |
|---|---|---|
| id, name | | |
| categoryIds | List<String> | serializované / přes cross-ref |
| amountMinor, currency | | limit |
| period | enum | WEEK, MONTH, YEAR, CUSTOM |
| startDate | LocalDate | |
| rollover | Boolean | *P1* |
| notifyThresholdPct | Int | např. 80 |

### PlannedPayment *(P1)*
- type, accountId, categoryId, amountMinor, currency, payee, note,
  frequency(enum DAILY/WEEKLY/MONTHLY/YEARLY/CUSTOM), nextDate, endDate,
  autoCreate(Boolean), reminderDaysBefore(Int).

### Debt *(P1)*
- `Debt(id, direction[OWE/OWED], personName, amountMinor, currency, date, dueDate, note, accountId?, settled)`
- `DebtPayment(id, debtId, amountMinor, date)`.

### Goal *(P1)*
- `Goal(id, name, targetAmountMinor, currency, deadline, accountId?, icon, color)`
- `GoalContribution(id, goalId, amountMinor, date)`.

### ShoppingList / ShoppingItem *(P1)*
- `ShoppingList(id, name, createdAt)`
- `ShoppingItem(id, listId, name, quantity, priceMinor?, checked)`.

### Warranty *(P1)*
- `Warranty(id, productName, brand?, purchaseDate, warrantyMonths, expiryDate, photoUri?, note?, reminderEnabled)`.

### Template *(P1)*
- `Template(id, name, type, accountId, categoryId, amountMinor?, payee?, note?)`.

### Currency / ExchangeRate *(P1)*
- `Currency(code, symbol, decimalDigits)`
- `ExchangeRate(code, rateToBase, updatedAt)`; výchozí měna v nastavení.

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
- baseCurrency, language, theme, firstDayOfWeek, firstDayOfMonth,
  appLockEnabled, lockType, onboardingDone, notif flags.

## Vztahy (zjednodušeně)

```
Account 1───* Record *───1 Category
Account 1───* FioConnection
Record  *───* Label
Budget  *───* Category
Debt    1───* DebtPayment
Goal    1───* GoalContribution
ShoppingList 1───* ShoppingItem
```

## Indexy a výkon
- Indexy na `Record(accountId)`, `Record(categoryId)`, `Record(dateTime)`,
  **unikátní** `Record(fioTransactionId)` (kde not null) kvůli deduplikaci.
- Agregace (součty, reporty) přes SQL `SUM/GROUP BY` v DAO, ne v paměti.
- DAO vrací `Flow<...>` → UI reaktivně překresluje.

## Migrace a zálohy
- Verzované Room migrace.
- Záloha = export celé DB + nastavení do souboru (sdílení/obnova). *(P1)*
