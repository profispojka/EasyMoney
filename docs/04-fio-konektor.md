# 04 — Fio konektor (import transakcí)

Read-only import pohybů z **Fio banky** přes její veřejné REST „API Bankovnictví“.
Rozsah: **pouze stahování transakcí**, žádné odchozí platby.

> Zdroje: oficiální stránka [fio.cz/bank-services/internetbanking-api](https://www.fio.cz/bank-services/internetbanking-api)
> a manuál PDF [API_Bankovnictvi.pdf](https://www.fio.cz/docs/cz/API_Bankovnictvi.pdf).
> Endpointy a mapování sloupců níže odpovídají veřejně dokumentovanému API; před
> implementací je **ověříme proti aktuálnímu PDF** (verze API se může lišit).

## 1. Token (autentizace)
- Uživatel si v **Internetbankingu Fio → Nastavení → API** vygeneruje token
  typu **„pro stahování dat / pohyby a výpisy“** (read-only).
- Token je **vázaný na jeden účet** a je platný cca **5 minut po vygenerování**,
  než se aktivuje. Jeden token = jeden Fio účet → pro více účtů více tokenů.
- Token je dlouhý řetězec (~64 znaků). **Kdokoli s tímto tokenem si může stáhnout
  historii pohybů**, proto ho ukládáme šifrovaně (viz §6).

## 2. Base URL a endpointy
Base: `https://fioapi.fio.cz/v1/rest/`

| Účel | Metoda | URL (`{token}` = API token) |
|---|---|---|
| Pohyby za období | GET | `periods/{token}/{datum_od}/{datum_do}/transactions.json` |
| Nové od posledního stažení | GET | `last/{token}/transactions.json` |
| Oficiální výpis dle čísla | GET | `by-id/{token}/{rok}/{cislo}/transactions.json` |
| Posun ukazatele na ID | GET | `set-last-id/{token}/{id}/` |
| Posun ukazatele na datum | GET | `set-last-date/{token}/{datum}/` |
| Číslo posledního výpisu | GET | `last-statement/{token}/statement` |

- Datum ve formátu `YYYY-MM-DD`.
- Formát odpovědi dán příponou: `.json` (použijeme), dále `.xml .csv .gpc .ofx .mt940 .html .pdf`.
- **`last/`** vrací pohyby od posledního „zarážky“ a zároveň ji posune — ideální pro
  inkrementální sync. Na první import použijeme `periods/` (historie) a pak `set-last-date`.

## 3. Rate limit
- **Max. 1 dotaz na token za 30 sekund.** Při porušení vrací **HTTP 409 Conflict**.
- Klient proto:
  - drží per-token časovou zarážku posledního volání,
  - při 409 čeká a zkouší znovu (exponenciální backoff, max N pokusů),
  - víc Fio účtů řadí sekvenčně s rozestupem (ne paralelně na stejný token).

## 4. JSON struktura odpovědi
```json
{
  "accountStatement": {
    "info": {
      "accountId": "2000000000", "bankId": "2010", "currency": "CZK",
      "iban": "CZ...", "bic": "FIOBCZPPXXX",
      "openingBalance": 1000.00, "closingBalance": 1234.56,
      "dateStart": "2024-01-01+0100", "dateEnd": "2024-01-31+0100",
      "idFrom": 123, "idTo": 130, "idLastDownload": 130
    },
    "transactionList": {
      "transaction": [
        {
          "column22": { "value": 26000000123, "name": "ID pohybu", "id": 22 },
          "column0":  { "value": "2024-01-15+0100", "name": "Datum", "id": 0 },
          "column1":  { "value": -250.00, "name": "Objem", "id": 1 },
          "column14": { "value": "CZK", "name": "Měna", "id": 14 },
          "column10": { "value": "ALBERT", "name": "Název protiúčtu", "id": 10 },
          "column5":  { "value": "1234", "name": "VS", "id": 5 },
          "column8":  { "value": "Platba kartou", "name": "Typ", "id": 8 }
        }
      ]
    }
  }
}
```
> Pole, která jsou prázdná, mohou v JSON **chybět nebo být `null`** — parser to musí ustát.

## 5. Mapování polí (Fio → naše `Record`)

| Fio sloupec | Význam | Naše pole |
|---|---|---|
| `column22` | ID pohybu (unikátní) | `fioTransactionId` (dedup) |
| `column0` | Datum | `dateTime` |
| `column1` | Objem (znaménkové) | `amountMinor` + odvození `type` (≥0 INCOME / <0 EXPENSE) |
| `column14` | Měna | `currency` |
| `column2` | Protiúčet (číslo) | součást `payee` / Fio detail |
| `column3` | Kód banky | Fio detail |
| `column10` | Název protiúčtu | `payee` |
| `column12` | Název banky | Fio detail |
| `column4` | KS (konstantní symbol) | Fio detail |
| `column5` | VS (variabilní symbol) | Fio detail + vstup pro pravidla |
| `column6` | SS (specifický symbol) | Fio detail |
| `column7` | Uživatelská identifikace | `note` (fallback) |
| `column16` | Zpráva pro příjemce | `note` (primárně) |
| `column25` | Komentář | `note` (fallback) |
| `column8` | Typ pohybu | `paymentType` / Fio detail |
| `column9` | Provedl | Fio detail |

- **Částka:** `column1` je v hlavní jednotce (Kč) jako desetinné číslo → převést na
  `amountMinor` (×100, bezpečně přes `BigDecimal`, ne float).
- **Payee:** přednostně `column10` (název protiúčtu); když chybí, složit z protiúčtu+banky.
- **Note:** přednostně `column16`, jinak `column25`/`column7`.

## 6. Bezpečné uložení tokenu
- Token **nikdy** do Room ani do logů.
- Uložení do **`EncryptedSharedPreferences`** (Jetpack Security) s klíčem v **Android Keystore**.
- V Room jen `tokenAlias` (klíč do šifrovaného úložiště) v entitě `FioConnection`.
- Token maskovat v UI (zobrazit jen poslední 4 znaky), kopírovat ze schránky.

## 7. Algoritmus synchronizace

### První připojení
1. Ověř token: zkušební `last/` (nebo `periods/` za posledních pár dní) → pokud 200, OK.
2. Dle volby historie zavolej `periods/{token}/{od}/{do}/transactions.json`
   (např. posledních 90 dní; pozor na rate limit, případně po měsících).
3. Naimportuj transakce (viz §8), nastav `lastTransactionId` = `info.idTo`.
4. Zavolej `set-last-date/{token}/{dnes}` (nebo `set-last-id`), ať `last/` napříště
   vrací jen nové.
5. Ulož `FioConnection`, naplánuj periodický sync.

### Inkrementální sync (WorkManager, periodicky)
1. Respektuj 30 s rate limit (per token).
2. GET `last/{token}/transactions.json`.
3. Pro každou transakci: dedup dle `fioTransactionId` → nové vlož, existující přeskoč.
4. Aktualizuj `lastTransactionId`, `lastSyncAt`, dopočítej zůstatek účtu.
5. Pošli notifikaci „X nových transakcí“ (dle nastavení).

### Manuální sync
- Tlačítko „Synchronizovat teď“ / pull-to-refresh → stejná logika, mimo plán.

## 8. Import jedné transakce (pravidla)
- **Dedup:** unikátní index na `Record.fioTransactionId`; existující ID = skip
  (idempotence i při překryvu period).
- **Účet:** `FioConnection.accountId`.
- **Kategorie:** zkus pravidla automatické kategorizace (§9); jinak `null`
  („Nezařazeno“) — uživatel dořeší.
- **Source:** `FIO` (badge v UI, nejde upravit částka/datum, aby seděl zůstatek;
  kategorii/poznámku upravit lze).
- **Měna:** aplikace je jednoměnová (CZK). Pohyby v jiné měně než CZK se
  importují podle částky zaúčtované na účtu (Fio účtuje v CZK); pole `column14`
  z Fio se nepoužívá pro přepočet.

## 9. Automatická kategorizace *(P1)*
- Jednoduchý **pravidlový engine** (`CategorizationRule`): podmínka nad
  `payee`/`note`/`VS`/`protiúčet` → kategorie, dle priority.
- Plus heuristiky: opakující se příjemce → poslední ručně zvolená kategorie pro něj.
- Bez ML; případné ML/AI je mimo MVP.

## 10. Ošetření chyb
| Situace | Reakce |
|---|---|
| 409 Conflict (rate limit) | backoff + retry, jinak odlož na další běh |
| 401/403 / neplatný token | označ připojení jako chybné, vyzvi k zadání tokenu znovu |
| 404 / prázdná data | žádné nové transakce — OK |
| 5xx / timeout / offline | retry s backoffem; nech `WorkManager` zopakovat |
| Nevalidní JSON | zaloguj (bez tokenu), přeskoč běh, nahlas chybu v I2 |

## 11. Omezení / poznámky
- Fio token je **per účet** → vícero Fio účtů = vícero tokenů (UI to zvládá v I1/I2).
- Token má **omezenou platnost dle nastavení ve Fio** (může expirovat) — řešíme chybou 401.
- API je **read-only**; appka nikdy nepošle platbu → nižší bezpečnostní riziko.
- Historie přes `periods/` je omezená dostupností dat na straně Fio.
- **Před implementací ověřit** přesné URL, sloupce a limity proti aktuálnímu
  [oficiálnímu PDF](https://www.fio.cz/docs/cz/API_Bankovnictvi.pdf).

## 12. Rozšiřitelnost na další banky
- Konektor schovat za rozhraní `BankConnector` (metody `verify`, `fetchSince`, `fetchPeriod`).
- `FioConnector` je první implementace; pozdější banky (PSD2/jiné API) přidají další
  implementace beze změny UI a importní logiky.
