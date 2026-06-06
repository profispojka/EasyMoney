# 00 — Přehled a rozsah

## Vize

Osobní finanční tracker pro Android, který uživateli ukáže **kam jdou jeho peníze**.
Funkčně kopíruje Wallet by BudgetBakers: účty, transakce, kategorie, rozpočty,
reporty, plánované platby, dluhy, cíle, nákupní seznamy, záruky — plus **automatický
import pohybů z Fio banky**.

Hlavní hodnota: rychlé zadání výdaje (pár tapů), přehledný dashboard, a u Fio účtu
se transakce natáhnou samy.

## Principy

1. **Offline-first** — appka je plně funkční bez internetu, data jsou v telefonu.
   Internet je potřeba jen pro Fio sync a (později) cloud zálohu.
2. **Rychlost zadání** — přidat výdaj musí jít do ~3 sekund (FAB, šablony, widget).
3. **Peníze přesně** — žádné `float`/`double` na částky; ukládáme v minor units
   (haléře jako `Long`) + kód měny, počítáme přes `BigDecimal`.
4. **Soukromí** — Fio token i citlivá data šifrovaná; volitelný zámek appky (PIN/biometrie).
5. **Material 3** — moderní vzhled, světlý i tmavý režim.

## Rozsah — co stavíme

### MVP (jádro, fáze 1–3)
- Účty (hotovost, běžný účet, karta, spoření…) s počátečním zůstatkem a měnou
- Transakce: **příjem / výdaj / převod mezi účty**
- Kategorie a podkategorie (přednastavená sada + vlastní)
- Dashboard (čisté jmění, přehled měsíce, poslední transakce, mini-grafy)
- Seznam transakcí s filtrováním a hledáním
- Rozpočty (měsíční/týdenní/vlastní, upozornění při překročení)
- Reporty: cash flow, výdaje po kategoriích, vývoj zůstatku
- **Fio konektor** — import transakcí read-only tokenem + automatické párování do kategorií
- Nastavení, zámek appky, výchozí měna

### Rozšíření (fáze 4–5)
- Plánované/opakované platby s připomínkami
- Dluhy (komu dlužím / kdo dluží mně) s částečnými splátkami
- Spořicí cíle s příspěvky
- Nákupní seznamy
- Záruční karty (foto, expirace, připomínka)
- Šablony, štítky (labels), vícenásobné měny s kurzy
- Widgety, notifikace, CSV import/export

### Mimo rozsah teď (pozdější fáze 6)
- Cloud sync, přihlašování, web aplikace
- Sdílení účtů / rodinné finance (shared wallets)
- Jiné banky než Fio (architektura ale počítá s více konektory)
- Odchozí platby z appky
- iOS verze

## Srovnání s Wallet — co vědomě zjednodušujeme

| Wallet má | My v MVP | Pozn. |
|---|---|---|
| Sync 15 000+ bank (PSD2) | Jen Fio (1 konektor) | Architektura rozšiřitelná |
| Cloud + web + sdílení | Lokálně | Sync je fáze 6 |
| AI kategorizace | Pravidlový engine (klíčová slova, VS, protiúčet) | Bez ML v MVP |
| Prémiové předplatné | Vše zdarma, lokálně | Bez plateb/účtů |

## Cílová skupina a jazyk

- Primárně český uživatel (Fio banka, CZK jako výchozí měna).
- UI lokalizace: **čeština + angličtina** (stringy připravené na i18n od začátku).
