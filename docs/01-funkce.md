# 01 — Kompletní seznam funkcí (Wallet → naše appka)

Přehled funkcí Wallet by BudgetBakers (dle
[budgetbakers.com/wallet](https://budgetbakers.com/en/products/wallet/) a
[/features](https://budgetbakers.com/en/products/wallet/features/)) a jak je pokrýváme.

Legenda priorit: **P0** = MVP jádro, **P1** = rozšíření, **P2** = pozdější fáze.

## 1. Účty (Accounts)
- Neomezený počet účtů: hotovost, běžný účet, kreditka, spoření, investice, voucher/klubová karta. **P0**
- Atributy: název, typ, počáteční zůstatek, ikona. **P0** (měna vždy CZK; bez barev — monochrom)
- Vyloučit účet ze statistik / z čistého jmění. **P0**
- Archivace účtu (skrýt, ale zachovat historii). **P1**
- Aktuální zůstatek = počáteční ± transakce (počítá appka). **P0**
- Celkové čisté jmění napříč účty (jednotná měna CZK, bez přepočtu). **P0**

## 2. Transakce / Záznamy (Records)
- Typy: **příjem, výdaj, převod** mezi účty. **P0**
- Pole: částka, kategorie, účet, datum a čas, protistrana/příjemce (payee), poznámka,
  platební metoda. **P0**
- Převod = jedna operace propojující dva účty (případně s poplatkem). **P0**
- Šablony pro rychlé opakované zadání. **P1**
- Filtrování (účet, kategorie, typ, období, částka). **P0**
- Seskupení v seznamu po dnech, se součty. **P0**

## 3. Kategorie (Categories)
- Přednastavená sada kategorií + podkategorií (Jídlo, Doprava, Bydlení, Zábava…). **P0**
- Vlastní kategorie s ikonou a typem (příjem/výdaj); bez barvy (monochrom). **P0**
- Nadřazené kategorie / skupiny (super-categories). **P1**
- Automatická kategorizace (u Fio importu pravidlovým enginem). **P0** (pro Fio)

## 4. Rozpočty (Budgets)
- Rozpočet na kategorii / skupinu kategorií. **P0**
- Období: měsíční, týdenní, roční, vlastní. **P0** (vlastní **P1**)
- Limit, průběh čerpání v reálném čase, zbývá / přečerpáno. **P0**
- Upozornění při blížícím se / překročeném limitu. **P0**
- Přenos zůstatku do dalšího období (rollover). **P1**

## 5. Reporty a analýzy (Reports / Cash Flow)
- Cash flow: příjmy vs. výdaje v čase (sloupcový graf). **P0**
- Výdaje podle kategorií — **monochromatický žebříček/pruhy** (místo barevného koláče) s drill-down. **P0**
- Vývoj zůstatku / čistého jmění v čase (spojnice). **P0**
- Srovnání období (tento vs. minulý měsíc). **P1**
- Volba období (měsíc/rok/vlastní rozsah) a filtr účtů. **P0**

## 6. Plánované / opakované platby (Planned Payments)
- Opakované příjmy/výdaje (nájem, předplatné, výplata). **P1**
- Frekvence (denně/týdně/měsíčně/ročně/vlastní), datum příště, konec. **P1**
- Přehled nadcházejících plateb na dashboardu. **P1**

## 7. Měna (Currency)
- Jednotná měna celé aplikace: **CZK**. Žádné přepočty ani kurzy. **P0**
- Více měn a kurzy nejsou v plánu (vědomé zjednodušení pro českého uživatele).

## 8. Bankovní synchronizace (Bank Sync) — Fio
- Připojení Fio účtu read-only tokenem. **P0**
- Automatické stahování pohybů na pozadí (WorkManager). **P0**
- Deduplikace, párování do kategorií, aktualizace zůstatku. **P0**
- Detail v [04-fio-konektor.md](04-fio-konektor.md). **P0**

## 9. Zabezpečení (Security)
- Šifrované uložení Fio tokenu (Keystore / EncryptedSharedPreferences). **P0**

## 10. Upozornění (vizuální)
- Stav rozpočtu přímo v UI (naplněnost pruhu + `⚠` při překročení). **P0**
- **Žádné push notifikace ani připomínky** (vědomě neděláme).

## 11. Záloha dat
- Záloha/obnova celé databáze do lokálního souboru. **P1**

## 12. Onboarding a nastavení
- Úvodní průvodce: vytvoření prvního účtu (měna je vždy CZK). **P0**
- Nastavení: první den týdne/měsíce,
  správa kategorií/šablon, záloha/obnova dat, o aplikaci. **P0** (UI jen česky)

## 13. Cloud (pozdější fáze)
- Volitelná cloud záloha / sync mezi vlastními zařízeními. **P2**
