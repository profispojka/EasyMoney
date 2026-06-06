# 06 — Roadmap (fáze a milníky)

Postup po inkrementech, které jsou vždy spustitelné. Odhady jsou orientační (1 vývojář).

## Fáze 0 — Základ projektu  *(~3–5 dní)*
- Založit Android projekt (Kotlin, Compose, Hilt, Room, Navigation).
- Design system: barvy, typografie, motiv (light/dark), ikony, komponenty.
- Navigační kostra (bottom nav + prázdné obrazovky), DataStore nastavení.
- **Výstup:** appka se spustí, přepíná záložky.

## Fáze 1 — Jádro: účty + transakce  *(~1–2 týdny)* **MVP**
- Room schéma: Account, Category, Record (+ migrace).
- Přednastavené kategorie.
- Onboarding + setup wizard (měna, první účet).
- CRUD účtů (D1–D3), CRUD transakcí vč. převodů (C1–C4), kalkulačka částky.
- Dashboard v1 (čisté jmění, účty, poslední záznamy).
- **Výstup:** lze plně ručně sledovat příjmy/výdaje.

## Fáze 2 — Rozpočty + analýzy  *(~1–1,5 týdne)* **MVP**
- Rozpočty (E1–E3) + upozornění při překročení.
- Reporty (F1–F3): cash flow, výdaje po kategoriích (donut), vývoj zůstatku.
- Hledání (C5), filtry, prázdné stavy.
- **Výstup:** přehled „kam jdou peníze“.

## Fáze 3 — Fio konektor  *(~1–1,5 týdne)* **MVP**
- Retrofit Fio API, DTO, mapper, `BankConnector`.
- Šifrované uložení tokenu (Keystore), připojení účtu (I1), stav sync (I2).
- WorkManager periodický sync, dedup, dopočet zůstatku, notifikace.
- Ošetření chyb (409 rate limit, neplatný token…).
- **Výstup:** Fio transakce se importují automaticky.

## Fáze 4 — Rozšiřující moduly  *(~2–3 týdny)* **P1**
- Plánované platby (G1–G2) + připomínky.
- Dluhy, cíle, nákupní seznamy, záruky (sekce H).
- Šablony, štítky, automatická kategorizace Fio (I3 + pravidla).
- Zámek appky (PIN/biometrie), notifikační centrum (H2).

## Fáze 5 — Polish a data  *(~1–2 týdny)* **P1**
- Vícenásobné měny + kurzy, foto účtenek, lokalita.
- Domovské widgety, CSV import/export, záloha/obnova DB.
- Lokalizace cs/en, přístupnost, výkonové ladění, testy.

## Fáze 6 — Cloud (pozdější)  *(samostatná etapa)* **P2**
- Backend, účty/přihlášení, sync mezi zařízeními, web app, sdílené účty.
- Případně další banky přes `BankConnector`.

## Milníky
| Milník | Obsah |
|---|---|
| **M1** | Fáze 0–1: ruční tracking funguje |
| **M2** | Fáze 2: rozpočty + reporty |
| **M3** | Fáze 3: **Fio automatický import** (hlavní cíl MVP) |
| **M4** | Fáze 4–5: feature parita s Wallet (lokálně) |
| **M5** | Fáze 6: cloud a sdílení |

## Doporučené pořadí ke schválení
1. Odsouhlasit rozsah MVP (M1–M3) a [07-otevrene-otazky.md](07-otevrene-otazky.md).
2. Začít **Fází 0** (založení projektu) — malý, ověřitelný krok.
