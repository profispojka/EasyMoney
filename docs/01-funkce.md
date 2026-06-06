# 01 — Kompletní seznam funkcí (Wallet → naše appka)

Přehled funkcí Wallet by BudgetBakers (dle
[budgetbakers.com/wallet](https://budgetbakers.com/en/products/wallet/) a
[/features](https://budgetbakers.com/en/products/wallet/features/)) a jak je pokrýváme.

Legenda priorit: **P0** = MVP jádro, **P1** = rozšíření, **P2** = pozdější fáze.

## 1. Účty (Accounts)
- Neomezený počet účtů: hotovost, běžný účet, kreditka, spoření, investice, voucher/klubová karta. **P0**
- Atributy: název, typ, měna, počáteční zůstatek, barva, ikona. **P0**
- Vyloučit účet ze statistik / z čistého jmění. **P0**
- Archivace účtu (skrýt, ale zachovat historii). **P1**
- Aktuální zůstatek = počáteční ± transakce (počítá appka). **P0**
- Celkové čisté jmění napříč účty (přepočet do výchozí měny). **P0**

## 2. Transakce / Záznamy (Records)
- Typy: **příjem, výdaj, převod** mezi účty. **P0**
- Pole: částka, kategorie, účet, datum a čas, protistrana/příjemce (payee), poznámka,
  štítky, platební metoda, foto účtenky, lokalita (GPS). **P0** (foto/GPS **P1**)
- Převod = jedna operace propojující dva účty (případně s poplatkem). **P0**
- Šablony pro rychlé opakované zadání. **P1**
- Rozdělení transakce (split) do více kategorií. **P2**
- Filtrování (účet, kategorie, typ, období, štítek, částka) a fulltext hledání. **P0**
- Seskupení v seznamu po dnech, se součty. **P0**

## 3. Kategorie (Categories)
- Přednastavená sada kategorií + podkategorií (Jídlo, Doprava, Bydlení, Zábava…). **P0**
- Vlastní kategorie s ikonou, barvou a typem (příjem/výdaj). **P0**
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
- Výdaje podle kategorií (koláč/donut) s drill-down. **P0**
- Vývoj zůstatku / čistého jmění v čase (spojnice). **P0**
- Srovnání období (tento vs. minulý měsíc). **P1**
- Volba období (měsíc/rok/vlastní rozsah) a filtr účtů. **P0**

## 6. Plánované / opakované platby (Planned Payments)
- Opakované příjmy/výdaje (nájem, předplatné, výplata). **P1**
- Frekvence (denně/týdně/měsíčně/ročně/vlastní), datum příště, konec. **P1**
- Připomínka X dní předem; volitelně automatické vytvoření transakce. **P1**
- Přehled nadcházejících plateb na dashboardu. **P1**

## 7. Dluhy (Debts)
- Dvě strany: **dlužím** / **dluží mi**. **P1**
- Pole: osoba, částka, měna, datum, splatnost, poznámka. **P1**
- Částečné splátky; stav vyrovnáno. **P1**
- Připomínka splatnosti. **P1**

## 8. Spořicí cíle (Goals)
- Cílová částka, termín, navázaný účet, ikona/barva. **P1**
- Příspěvky a průběh (% naspořeno). **P1**

## 9. Nákupní seznamy (Shopping Lists)
- Více seznamů, položky se zaškrtáváním, volitelně cena a množství. **P1**
- Převod nakoupeného seznamu na výdajovou transakci. **P1**

## 10. Záruční karty (Warranties)
- Evidence záruk: produkt, značka, datum nákupu, délka záruky, expirace, foto. **P1**
- Připomínka před koncem záruky. **P1**

## 11. Měny (Currencies)
- Více měn, výchozí (základní) měna, přepočet zůstatků a reportů. **P1**
- Kurzy (manuální + aktualizace z online zdroje). **P1**

## 12. Bankovní synchronizace (Bank Sync) — Fio
- Připojení Fio účtu read-only tokenem. **P0**
- Automatické stahování pohybů na pozadí (WorkManager). **P0**
- Deduplikace, párování do kategorií, aktualizace zůstatku. **P0**
- Detail v [04-fio-konektor.md](04-fio-konektor.md). **P0**

## 13. Zabezpečení (Security)
- Zámek appky: PIN / biometrie (otisk, obličej). **P0**
- Šifrované uložení Fio tokenu (Keystore / EncryptedSharedPreferences). **P0**

## 14. Widgety a notifikace
- Domovský widget: rychlé přidání + přehled zůstatku/rozpočtu. **P1**
- Notifikace: připomínky plateb, překročení rozpočtu, dokončený Fio sync. **P0/P1**

## 15. Import / Export
- Export dat do CSV; import transakcí z CSV. **P1**
- Záloha/obnova celé databáze (lokální soubor). **P1**

## 16. Onboarding a nastavení
- Úvodní průvodce: výběr výchozí měny, vytvoření prvního účtu. **P0**
- Nastavení: měny, zabezpečení, notifikace, první den týdne/měsíce, motiv, jazyk,
  správa kategorií/štítků/šablon, data (export/import), o aplikaci. **P0**

## 17. Cloud / sdílení (pozdější fáze)
- Cloud sync mezi zařízeními, web app, sdílené účty (rodina). **P2**
