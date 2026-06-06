# 02 — Obrazovky (detailní plán)

Popis každé obrazovky: účel, layout, prvky, akce a přechody. Navigace je
**Bottom Navigation** se 4–5 záložkami + centrální FAB pro rychlé přidání.

## Navigační kostra

```
┌──────────────────────────────────────────────┐
│  Bottom navigation                             │
│  [ Přehled ] [ Záznamy ]  (+)  [ Rozpočty ] [ Více ] │
└──────────────────────────────────────────────┘
```

- **Přehled** (Dashboard) — domovská
- **Záznamy** (Records) — seznam transakcí
- **(+) FAB** — rychlé přidání transakce (uprostřed, vystouplý)
- **Rozpočty / Analýzy** — rozpočty + reporty
- **Více** (More) — účty, plánované platby, dluhy, cíle, nákupy, záruky, nastavení

Mimo bottom nav se otevírají detailní/edit obrazovky jako full-screen nebo bottom sheet.

---

## A. Onboarding a vstup

### A1. Splash
- **Účel:** start, načtení DB, rozhodnutí kam dál.
- **Logika:** pokud appka ještě neproběhla onboardingem → A2. Jinak pokud je zapnutý
  zámek → A4 (odemčení), jinak → B1 (Dashboard).

### A2. Onboarding intro (carousel)
- **Účel:** 3–4 slidy s hodnotou appky (sleduj výdaje, rozpočty, Fio sync).
- **Prvky:** stránkovací indikátor, „Přeskočit“, „Začít“.
- **Akce:** „Začít“ → A3.

### A3. Prvotní nastavení (setup wizard)
- **Krok 1 — výchozí měna:** výběr základní měny (default CZK dle locale).
- **Krok 2 — první účet:** název (např. „Hotovost“), typ, počáteční zůstatek.
- **Krok 3 (volitelně):** nabídka zapnout zámek appky.
- **Akce:** „Hotovo“ → vytvoří účet, založí přednastavené kategorie → B1.

### A4. Zámek appky (Lock screen)
- **Účel:** odemčení PINem / biometrií při startu nebo návratu z pozadí.
- **Prvky:** PIN pad, ikona biometrie, „Zapomenutý PIN“ (reset přes potvrzení).
- **Akce:** úspěch → původní cíl. Neúspěch → countdown po N pokusech.

---

## B. Přehled (Dashboard)

### B1. Dashboard
- **Účel:** rychlý finanční obrázek „tady a teď“.
- **Top bar:** název/avatar, ikona hledání (→ C5), ikona notifikací (→ H2).
- **Sekce (scrollovatelné karty):**
  1. **Čisté jmění** — součet zůstatků všech nevyloučených účtů ve výchozí měně,
     malý trend (↑/↓ vs. minulý měsíc).
  2. **Účty** — horizontální karuselové karty účtů (barva, ikona, zůstatek);
     tap → D2 (detail účtu), poslední karta „+ Přidat účet“ → D3.
  3. **Tento měsíc** — příjmy vs. výdaje (dvě čísla + mini sloupcový graf), zbývá rozpočet.
  4. **Rozpočty** — top 3 rozpočty s progress barem; tap → E1.
  5. **Výdaje podle kategorií** — donut graf za období; tap → F2.
  6. **Nadcházející platby** — 2–3 nejbližší plánované platby; tap → G1.
  7. **Poslední záznamy** — 5 posledních transakcí; „Zobrazit vše“ → C1.
- **FAB (+):** → C2 (přidat transakci).
- **Pull-to-refresh:** spustí Fio sync u připojených účtů (respektuje rate limit).

---

## C. Záznamy / Transakce

### C1. Seznam záznamů (Records)
- **Účel:** chronologický seznam všech transakcí.
- **Top bar:** název „Záznamy“, ikona filtru (→ C4), ikona hledání (→ C5).
- **Přepínač období:** segment/šipky (tento měsíc ◀▶), volba „vlastní rozsah“.
- **Souhrnný pruh:** příjmy / výdaje / saldo za vybrané období.
- **Seznam:** seskupený po dnech (hlavička s datem + denní součet). Položka:
  ikona+barva kategorie, název kategorie/příjemce, účet, poznámka (zkráceně),
  částka (zelená příjem / červená výdaj), případně badge „Fio“ u importované.
- **Akce:** tap na položku → C3 (detail); swipe = rychlé akce (smazat / duplikovat).
- **FAB (+):** → C2.

### C2. Přidat / upravit transakci
- **Účel:** zadání nebo editace záznamu. Otevírá se jako full-screen.
- **Přepínač typu nahoře:** **Výdaj | Příjem | Převod** (barevně odlišené).
- **Hlavní vstup:** velká číselná klávesnice s **kalkulačkou** (+ − × ÷), zobrazená částka a měna.
- **Pole (Výdaj/Příjem):**
  - **Účet** (výběr; default poslední použitý)
  - **Kategorie** (mřížka ikon s vyhledáním; pro výdaj výdajové, pro příjem příjmové)
  - **Datum a čas** (default teď)
  - **Příjemce / plátce** (text s našeptáváním z historie)
  - **Poznámka**
  - **Štítky** (multi-select chips) — *P1*
  - **Platební metoda** (hotovost/karta/převod) — *P1*
  - **Foto účtenky**, **lokalita** — *P1*
- **Pole (Převod):** **Z účtu**, **Na účet**, částka (volitelně jiná částka/měna na druhé straně + poplatek).
- **Akce:** „Uložit“ (a volitelně „Uložit a přidat další“); v editaci „Smazat“.
  Možnost „Uložit jako šablonu“ (*P1*).
- **Zkratky:** lišta posledních použitých kategorií, tlačítko „Ze šablony“ (*P1*).

### C3. Detail transakce
- **Účel:** zobrazení jednoho záznamu.
- **Prvky:** velká částka, kategorie, účet, datum, příjemce, poznámka, štítky,
  foto (zvětšení), mapa lokality; u Fio záznamu blok „Z banky“ (VS/KS/SS, protiúčet, typ).
- **Akce:** „Upravit“ → C2, „Duplikovat“, „Smazat“.

### C4. Filtr záznamů (bottom sheet)
- **Prvky:** účty (multi), kategorie (multi), typ, rozsah částky, štítky, období,
  jen Fio / jen ruční.
- **Akce:** „Použít“ aplikuje na C1, „Vyčistit“.

### C5. Hledání
- **Prvky:** vyhledávací pole (poznámka, příjemce, částka), výsledky live.
- **Akce:** tap → C3.

---

## D. Účty

### D1. Seznam účtů
- **Účel:** správa účtů a přehled zůstatků.
- **Prvky:** nahoře čisté jmění; seznam účtů (ikona, název, typ, měna, zůstatek,
  badge „Fio“ u napojených, „vyloučeno“ u excluded). Možnost přepnout pořadí (drag).
- **Akce:** tap → D2, „+ Přidat účet“ → D3.

### D2. Detail účtu
- **Prvky:** zůstatek, malý graf vývoje, příjmy/výdaje měsíce, seznam transakcí účtu.
- **Akce:** „Upravit“ → D3, „Připojit Fio“ → I1 (jen u kompatibilních), archivovat, smazat.

### D3. Přidat / upravit účet
- **Pole:** název, typ (hotovost/běžný/kreditka/spoření/investice/jiné), měna,
  počáteční zůstatek, barva, ikona, **vyloučit ze statistik** (switch), archivovat.
- **Akce:** „Uložit“ / „Smazat“ (s varováním na navázané transakce).

---

## E. Rozpočty a analýzy

### E1. Seznam rozpočtů
- **Prvky:** karty rozpočtů — název, kategorie, období, progress bar
  (utraceno / limit), zbývá nebo „přečerpáno o…“, barva dle stavu (zelená/oranžová/červená).
- **Akce:** tap → E2, „+ Nový rozpočet“ → E3.

### E2. Detail rozpočtu
- **Prvky:** velký progress, denní „tempo“ (kolik můžeš utratit/den), seznam
  transakcí spadajících do rozpočtu, historie minulých období.
- **Akce:** „Upravit“ → E3, smazat.

### E3. Přidat / upravit rozpočet
- **Pole:** název, kategorie (jedna/skupina/„vše“), částka, období (měsíc/týden/rok/vlastní),
  začátek, **rollover** (přenos zůstatku, *P1*), práh upozornění (např. 80 %).
- **Akce:** „Uložit“ / „Smazat“.

### F. Analýzy / Reporty (záložka nebo tab v sekci Rozpočty)

### F1. Přehled analýz
- **Přepínač období** (měsíc/rok/vlastní) + filtr účtů.
- **Karty grafů:**
  - **Cash flow** — sloupce příjmy vs. výdaje po měsících/dnech → F3.
  - **Struktura výdajů** — donut po kategoriích → F2.
  - **Vývoj zůstatku** — spojnicový graf čistého jmění v čase.
  - **Příjmy vs. výdaje** — saldo, průměry.

### F2. Výdaje po kategoriích (drill-down)
- **Prvky:** donut + seřazený seznam kategorií (částka, %), srovnání s minulým obdobím.
- **Akce:** tap na kategorii → rozpad na podkategorie / seznam transakcí (C1 s filtrem).

### F3. Detail cash flow
- **Prvky:** sloupcový graf v čase, tabulka příjmů/výdajů/salda po obdobích.

---

## G. Plánované platby *(P1)*

### G1. Seznam plánovaných plateb
- **Prvky:** nadcházející i opakované — příjemce, částka, frekvence, datum příště,
  stav (čeká, dnes, po splatnosti).
- **Akce:** tap → G2, „+“ → G2. U položky „Zaplaceno“ (vytvoří reálnou transakci).

### G2. Přidat / upravit plánovanou platbu
- **Pole:** typ (příjem/výdaj/převod), účet, kategorie, částka, příjemce, poznámka,
  frekvence, datum příště, konec, **auto-vytvoření** vs. jen připomínka, kolik dní předem upozornit.
- **Akce:** „Uložit“ / „Smazat“.

---

## H. Dluhy, cíle, nákupy, záruky *(P1)*

### H-Dluhy
- **Seznam:** dvě sekce „Dlužím“ / „Dluží mi“, součty; položka = osoba, částka, splatnost.
- **Detail/Edit:** osoba, částka, měna, datum, splatnost, poznámka, navázaný účet;
  **částečné splátky** (seznam), tlačítko „Vyrovnáno“.

### H-Cíle (Goals)
- **Seznam:** karty cílů s progress (naspořeno/cíl, %), termín.
- **Detail/Edit:** název, cílová částka, termín, účet, ikona/barva; přidání příspěvku.

### H-Nákupní seznamy
- **Seznam seznamů:** název, počet položek, kolik zaškrtnuto.
- **Detail seznamu:** položky se zaškrtáváním, množství, volitelná cena, součet;
  „Převést na transakci“ (z odškrtnutých vytvoří výdaj).

### H-Záruky
- **Seznam:** produkt, expirace (badge „brzy vyprší“).
- **Detail/Edit:** produkt, značka, datum nákupu, délka záruky, expirace (auto-výpočet),
  foto účtenky/produktu, připomínka.

### H2. Notifikace / připomínky (centrum)
- **Prvky:** seznam upozornění (rozpočet přečerpán, platba zítra, záruka brzy vyprší,
  Fio sync hotov). Tap → odpovídající detail.

---

## I. Fio konektor (UI)

### I1. Připojení Fio účtu
- **Účel:** zadat read-only token a navázat ho na účet v appce.
- **Kroky/Prvky:**
  1. Krátký návod, jak token získat (Fio internetbanking → Nastavení → API),
     s odkazem a upozorněním „token je pouze pro čtení, nepohne penězi“.
  2. Pole **API token** (vložit ze schránky), pole **název** připojení.
  3. Volba: navázat na **existující účet** nebo **vytvořit nový**.
  4. Volba historie pro první import (např. 90 dní / od data / vše dostupné).
- **Akce:** „Připojit“ → ověří token zkušebním dotazem, uloží šifrovaně, spustí první sync → I2.

### I2. Stav synchronizace / připojené banky
- **Prvky:** seznam Fio připojení — název, navázaný účet, poslední sync (čas, počet
  nových transakcí), stav (OK / chyba / běží). Přepínač „auto-sync“ a interval.
- **Akce:** „Synchronizovat teď“, „Odpojit“, „Pravidla kategorizace“ → I3, řešení chyb
  (např. neplatný token → znovu zadat).

### I3. Pravidla automatické kategorizace *(P1)*
- **Prvky:** seznam pravidel „pokud (příjemce/VS/poznámka obsahuje X) → kategorie Y“.
- **Akce:** přidat/upravit/smazat pravidlo; pořadí priorit.

---

## J. Více / Nastavení

### J1. „Více“ (rozcestník)
- Dlaždice: Účty, Plánované platby, Dluhy, Cíle, Nákupní seznamy, Záruky,
  Kategorie, Štítky, Šablony, Připojené banky (Fio), Nastavení.

### J2. Nastavení (hlavní)
- **Sekce:**
  - **Obecné:** výchozí měna, jazyk, motiv (světlý/tmavý/systém), první den týdne/měsíce.
  - **Zabezpečení:** zámek appky (PIN/biometrie), změna PINu.
  - **Měny a kurzy:** seznam měn, zdroj kurzů, ruční přepis. *(P1)*
  - **Notifikace:** rozpočty, plánované platby, záruky, Fio sync.
  - **Kategorie / Štítky / Šablony:** správa (vlastní CRUD obrazovky).
  - **Data:** export/import CSV, záloha/obnova DB. *(P1)*
  - **Připojené banky:** → I2.
  - **O aplikaci:** verze, licence, zdroje.

---

## Přehled přechodů (zjednodušeně)

```
Splash → (Onboarding → Setup) / (Lock) → Dashboard
Dashboard ─FAB→ Přidat transakci → (Uložit) → Dashboard/Záznamy
Dashboard → Účty/Rozpočty/Analýzy/Nadcházející/Poslední → příslušné detaily
Záznamy → Detail → Upravit → (Uložit) → Záznamy
Více → Účty → Detail účtu → Připojit Fio → Stav sync
Více → Nastavení → (sekce)
```

## Poznámky k UX (společné)
- **Prázdné stavy** každé obrazovky mají ilustraci + jasnou výzvu (např. „Zatím žádné
  transakce — přidej první přes +“).
- **Měny:** částky vždy s kódem/symbolem měny účtu; souhrny ve výchozí měně s poznámkou o přepočtu.
- **Mazání** vždy s potvrzením a u entit s vazbami (účet → transakce) jasně vysvětlí dopad.
- **Přístupnost:** dostatečný kontrast, velikost dotykových cílů ≥ 48 dp, podpora TalkBack.
