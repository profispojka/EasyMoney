# 02 — Obrazovky (detailní plán)

Popis každé obrazovky: účel, layout, prvky, akce a přechody. Navigace je
**Bottom Navigation** se 4–5 záložkami + centrální FAB pro rychlé přidání.

## Navigační kostra

```
┌──────────────────────────────────────────────┐
│  Bottom navigation                             │
│  [ Přehled ] [ Platby ]  (+)  [ Rozpočty ] [ Více ] │
└──────────────────────────────────────────────┘
```

- **Přehled** (Dashboard) — domovská
- **Platby** — plánované (opakované) platby
- **(+) FAB** — rychlé přidání transakce (uprostřed, vystouplý)
- **Rozpočty / Analýzy** — rozpočty + reporty
- **Více** (More) — záznamy, účty, kategorie, záloha dat

Vybraná položka je **podtržená** (ne tmavá bublina). **Záznamy** jsou dostupné z „Více".
Mimo bottom nav se otevírají detailní/edit obrazovky jako full-screen.

---

## A. Onboarding a vstup

### A1. Splash
- **Účel:** start, načtení DB, rozhodnutí kam dál.
- **Logika:** pokud appka ještě neproběhla onboardingem → A2, jinak → B1 (Dashboard).

### A2. Onboarding intro (carousel)
- **Účel:** 3–4 slidy s hodnotou appky (sleduj výdaje, rozpočty, Fio sync).
- **Prvky:** stránkovací indikátor, „Přeskočit“, „Začít“.
- **Akce:** „Začít“ → A3.

### A3. Prvotní nastavení (setup wizard)
- **První účet:** název (např. „Hotovost“), typ, počáteční zůstatek (CZK).
- **Akce:** „Hotovo“ → vytvoří účet, založí přednastavené kategorie → B1.

---

## B. Přehled (Dashboard)

### B1. Dashboard
- **Účel:** rychlý finanční obrázek „tady a teď“.
- **Top bar:** název/avatar.
- **Sekce (scrollovatelné karty):**
  1. **Čisté jmění** — součet zůstatků všech nevyloučených účtů (CZK),
     malý trend (↑/↓ vs. minulý měsíc).
  2. **Účty** — horizontální karty účtů (ikona, název, zůstatek; orámované, bez barev);
     tap → D2 (detail účtu), poslední karta „+ Přidat účet“ → D3.
  3. **Tento měsíc** — příjmy vs. výdaje (dvě čísla + mini sloupcový graf), zbývá rozpočet.
  4. **Rozpočty** — top 3 rozpočty s progress barem; tap → E1.
  5. **Výdaje podle kategorií** — monochromatický žebříček (pruhy) za období; tap → F2.
  6. **Nadcházející platby** — 2–3 nejbližší plánované platby; tap → G1.
  7. **Poslední záznamy** — 5 posledních transakcí; „Zobrazit vše“ → C1.
- **FAB (+):** → C2 (přidat transakci).
- **Tlačítko „Synchronizovat“:** spustí Fio sync u připojených účtů (respektuje rate limit). Bez pull-to-refresh kvůli E-Ink.

---

## C. Záznamy / Transakce

### C1. Seznam záznamů (Records)
- **Účel:** chronologický seznam všech transakcí.
- **Top bar:** název „Záznamy“, ikona filtru (→ C4).
- **Přepínač období:** segment/šipky (tento měsíc ◀▶), volba „vlastní rozsah“.
- **Souhrnný pruh:** příjmy / výdaje / saldo za vybrané období.
- **Seznam:** seskupený po dnech (hlavička s datem + denní součet). Položka:
  ikona kategorie, název kategorie/příjemce, účet, poznámka (zkráceně),
  částka se znaménkem (**+** příjem / **−** výdaj, tučně), případně badge „Fio“ u importované.
- **Akce:** tap na položku → C3 (detail); dlouhý stisk = menu rychlých akcí (smazat / duplikovat). Bez swipe (E-Ink).
- **FAB (+):** → C2.

### C2. Přidat / upravit transakci
- **Účel:** zadání nebo editace záznamu. Otevírá se jako full-screen.
- **Přepínač typu nahoře:** **Výdaj | Příjem | Převod** (aktivní záložka inverzně — černé pozadí, bílý text).
- **Částka:** běžné číselné pole — po kliknutí naskočí systémová číselná klávesnice (žádná vlastní klávesnice na obrazovce).
- **Pole (Výdaj/Příjem):**
  - **Účet** (řádek → výběr v dialogu; default poslední použitý)
  - **Kategorie** (řádek → **výběr na samostatné obrazovce** s hierarchií skupin, ne mřížka)
  - **Datum a čas** (default teď)
  - **Příjemce / plátce** (text s našeptáváním z historie)
  - **Poznámka**
  - **Platební metoda** (hotovost/karta/převod) — *P1*
- **Pole (Převod):** **Z účtu**, **Na účet**, částka (volitelně poplatek).
- **Akce:** „Uložit“ (a volitelně „Uložit a přidat další“); v editaci „Smazat“.
  Možnost „Uložit jako šablonu“ (*P1*).
- **Zkratky:** lišta posledních použitých kategorií, tlačítko „Ze šablony“ (*P1*).

### C3. Detail transakce
- **Účel:** zobrazení jednoho záznamu.
- **Prvky:** velká částka, kategorie, účet, datum, příjemce, poznámka;
  u Fio záznamu blok „Z banky“ (VS/KS/SS, protiúčet, typ).
- **Akce:** „Upravit“ → C2, „Duplikovat“, „Smazat“.

### C4. Filtr záznamů (bottom sheet)
- **Prvky:** účty (multi), kategorie (multi), typ, rozsah částky, období,
  jen Fio / jen ruční.
- **Akce:** „Použít“ aplikuje na C1, „Vyčistit“.

---

## D. Účty

### D1. Seznam účtů
- **Účel:** správa účtů a přehled zůstatků.
- **Prvky:** nahoře čisté jmění; seznam účtů (ikona, název, typ, zůstatek,
  badge „Fio“ u napojených, „vyloučeno“ u excluded). Možnost přepnout pořadí (drag).
- **Akce:** tap → D2, „+ Přidat účet“ → D3.

### D2. Detail účtu
- **Prvky:** zůstatek, malý graf vývoje, příjmy/výdaje měsíce, seznam transakcí účtu.
- **Akce:** „Upravit“ → D3, „Připojit Fio“ → I1 (jen u kompatibilních), archivovat, smazat.

### D3. Přidat / upravit účet
- **Pole:** název, typ (hotovost/běžný/kreditka/spoření/investice/jiné),
  počáteční zůstatek (CZK), ikona, **vyloučit ze statistik** (switch), archivovat.
- **Akce:** „Uložit“ / „Smazat“ (s varováním na navázané transakce).

---

## E. Rozpočty a analýzy

### E1. Seznam rozpočtů
- **Prvky:** karty rozpočtů — název, kategorie, období, progress bar
  (utraceno / limit), zbývá nebo „přečerpáno o…“; stav vyznačen **naplněností pruhu + textem + `⚠`** při přečerpání (bez barev).
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
  - **Struktura výdajů** — žebříček po kategoriích (pruhy) → F2.
  - **Vývoj zůstatku** — spojnicový graf čistého jmění v čase.
  - **Příjmy vs. výdaje** — saldo, průměry.

### F2. Výdaje po kategoriích (drill-down)
- **Prvky:** seřazený žebříček kategorií (pruhy, částka, %), srovnání s minulým obdobím.
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
- **Pole:** typ (výdaj/příjem), účet, kategorie, částka, název, poznámka,
  frekvence (presety), začátek, volitelný konec.
- **Akce:** „Uložit“. V detailu navíc **„Zaplatit teď"** (vytvoří reálný záznam), „Smazat".
- Bez připomínek/notifikací (vědomě neděláme).

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
- Dlaždice: Účty, Plánované platby, Kategorie, Šablony,
  Připojené banky (Fio), Nastavení.

### J2. Nastavení (hlavní)
- **Sekce:**
  - **Obecné:** první den týdne/měsíce.
  - **Kategorie / Šablony:** správa (vlastní CRUD obrazovky).
  - **Data:** lokální záloha/obnova DB. *(P1)*
  - **Připojené banky:** → I2.
  - **O aplikaci:** verze, licence, zdroje.

---

## Přehled přechodů (zjednodušeně)

```
Splash → (Onboarding → Setup) → Dashboard
Dashboard ─FAB→ Přidat transakci → (Uložit) → Dashboard/Záznamy
Dashboard → Účty/Rozpočty/Analýzy/Nadcházející/Poslední → příslušné detaily
Záznamy → Detail → Upravit → (Uložit) → Záznamy
Více → Účty → Detail účtu → Připojit Fio → Stav sync
Více → Nastavení → (sekce)
```

## Poznámky k UX (společné)
- **Prázdné stavy** každé obrazovky mají ilustraci + jasnou výzvu (např. „Zatím žádné
  transakce — přidej první přes +“).
- **Měna:** celá aplikace pracuje výhradně v CZK; částky formátované dle locale `cs-CZ` (symbol „Kč“).
- **Mazání** vždy s potvrzením a u entit s vazbami (účet → transakce) jasně vysvětlí dopad.
- **Přístupnost:** dostatečný kontrast, velikost dotykových cílů ≥ 48 dp, podpora TalkBack.
