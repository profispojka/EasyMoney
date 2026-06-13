# 00 — Přehled a rozsah

## Vize

Osobní finanční tracker pro Android, který uživateli ukáže **kam jdou jeho peníze**.
Funkčně vychází z Wallet by BudgetBakers: účty, transakce, kategorie, rozpočty,
reporty a plánované platby — plus **automatický import pohybů z Fio banky**.

Hlavní hodnota: rychlé zadání výdaje (pár tapů), přehledný dashboard, a u Fio účtu
se transakce natáhnou samy.

## Principy

1. **Offline-first** — appka je plně funkční bez internetu, data jsou v telefonu.
   Internet je potřeba jen pro Fio sync a (později) cloud zálohu.
2. **Rychlost zadání** — přidat výdaj musí jít do ~3 sekund (FAB, šablony).
3. **Peníze přesně** — žádné `float`/`double` na částky; ukládáme v minor units
   (haléře jako `Long`), jednotná měna **CZK**, počítáme přes `BigDecimal`.
4. **Soukromí** — Fio token i citlivá data šifrovaná, vše zůstává v telefonu.
5. **E-Ink monochrom** — černobílý vysoký kontrast, bez animací, hodně bílého
   prostoru; klid v duchu „Calm“. Detail v [08-design-eink.md](08-design-eink.md).

## Rozsah — co stavíme

### MVP (jádro, fáze 1–3)
- Účty (hotovost, běžný účet, karta, spoření…) s počátečním zůstatkem (vše v CZK)
- Transakce: **příjem / výdaj / převod mezi účty**
- Kategorie a podkategorie (přednastavená sada + vlastní)
- Dashboard (čisté jmění, přehled měsíce, poslední transakce, mini-grafy)
- Seznam transakcí s filtrováním
- Rozpočty (měsíční/týdenní/vlastní, upozornění při překročení)
- Reporty: cash flow, výdaje po kategoriích, vývoj zůstatku
- **Fio konektor** — import transakcí read-only tokenem + automatické párování do kategorií
- Nastavení

### Rozšíření (fáze 4–5)
- Plánované/opakované platby (přehled „příští měsíc", „zaplatit teď")
- Šablony
- Lokální záloha/obnova dat

### Mimo rozsah teď (pozdější fáze 6)
- Cloud sync mezi zařízeními (volitelné)
- Jiné banky než Fio (architektura ale počítá s více konektory)

## Srovnání s Wallet — co vědomě zjednodušujeme

| Wallet má | My v MVP | Pozn. |
|---|---|---|
| Sync 15 000+ bank (PSD2) | Jen Fio (1 konektor) | Architektura rozšiřitelná |
| Cloud + web + sdílení účtů | Jen lokálně | Cloud sync volitelně později |
| AI kategorizace | Pravidlový engine (klíčová slova, VS, protiúčet) | Bez ML v MVP |
| Prémiové předplatné | Vše zdarma, lokálně | Bez plateb/účtů |

## Cílová skupina a jazyk

- Výhradně český uživatel (Fio banka, jednotná měna CZK).
- UI **pouze v češtině** — žádná lokalizace ani i18n.
