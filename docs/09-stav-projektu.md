# 09 — Stav projektu (Fáze 0–2 hotové, ověřeno na emulátoru)

Android projekt **CalmMoney** (`cz.calmmoney`) — monochromatický E-Ink design system,
navigace, **jádro** (účty, transakce, dashboard) a **rozpočty + analýzy**.
Cílové zařízení: **Mudita Kompakt**. Build OK; ověřeno během na emulátoru (API 35) s demo daty.

## Co je hotové (Fáze 2)
- **Rozpočty** — entita + repo; vytvoření (název, skupiny kategorií, limit, období),
  seznam s **monochromatickým ukazatelem čerpání** (naplněnost pruhu + „Zbývá“ / „⚠ Přečerpáno o…“),
  mazání. Čerpání počítá i z podkategorií dané skupiny v aktuálním období.
- **Analýzy** (přepínač v sekci Rozpočty): souhrn měsíce (příjmy/výdaje/saldo),
  **žebříček výdajů po kategoriích** (pruhy + %, místo barevného koláče),
  **cash flow** za 6 měsíců (příjmové/výdajové pruhy). Vše bez barev, bez animací.
- Dev: seed kategorií běží i při destruktivní migraci (změna schématu DB).
- **Detail záznamu** (tapni řádek) + **editace** (předvyplněný formulář) a **smazání**
  (u převodu i druhá noha). Ověřeno na emulátoru.
- **Výběr data a času** u transakce (Material date + time picker, vybraný den černě).
- **Filtr záznamů** — podle typu, účtu a kategorie (skupiny); souhrn i seznam se přepočítají.
- **Editace účtu** — tap účtu v seznamu → předvyplněný formulář (název, typ, počáteční zůstatek),
  uložení = update.
- **Plánované (opakované) platby** — přidání (název, typ, účet, kategorie, částka, frekvence,
  začátek, volitelný konec), seznam s frekvencí a datem příštího výskytu, mazání. Frekvence
  obecně jako jednotka + počet (presety: týdně/měsíčně/čtvrtletně/3×/2× ročně/ročně).
  **Přehled „příští měsíc"** sčítá jen výskyty, které reálně padnou do daného měsíce
  (např. 2×/ročně se započítá jen v měsíci, kdy nastane).
- **Plánované platby — dotaženo:** **detail** platby + **„Zaplatit teď"** (vytvoří reálný
  záznam), **editace** (předvyplněný formulář), **smazání**; na dashboardu karta
  **„Nadcházející platby"** (3 nejbližší výskyty dle data).
- **Vlastní kategorie** (Více → Kategorie) — seznam s hierarchií (skupina + podkategorie),
  přepínač Výdaje/Příjmy, **přidat / upravit / smazat** (mazání skupiny i s podkategoriemi).
  Formulář: název, typ, nadřazená kategorie, **výběr ikony** (mřížka ~69 monochrom ikon).
  Nové kategorie se objeví i ve formuláři transakce.
- **Fio – import** (Více → Fio – import) — read-only napojení přes Fio **„API Bankovnictví"
  token** (uživatel si v internetbankingu vytvoří token „pouhé monitorování účtu"). Stáhne
  pohyby (`HttpURLConnection` + `org.json`, žádná knihovna navíc), namapuje na záznamy (znaménko
  → výdaj/příjem, `source=FIO`) do zvoleného účtu; **deduplikace přes ID pohybu** (opakovaný sync
  nepřidá duplicity). U karetních plateb vytáhne **obchodníka** z textu („Nákup: ALBERT…" → „ALBERT…").
  Tlačítko „Importovat ukázková data" zkusí import bez tokenu. Vyžádalo oprávnění **INTERNET**.
  **Ověřeno naživo** (424 transakcí za 90 dní, součty sedí). Pozn.: Fio bez silné autorizace dá
  jen data ne starší **90 dní** (proto okno 90 dní; starší historie přes autorizaci v IB).
  (PSD2 cesta jako Wallet nejde — chce vlastní AISP licenci od ČNB.)
- **Záloha / obnova dat** (Více → Záloha dat) — export celé SQLite DB do souboru přes systémový
  dialog (SAF, žádná oprávnění navíc), obnova soubor ověří (`SQLite format 3`), přepíše DB a
  restartuje appku. Záloha ověřena živě na emulátoru (validní soubor 90 kB v Downloads).
- **Statistiky** (3. pozice spodní lišty, vzor Wallet) — karty **Zůstatek / Výdaje / Příjmy /
  Cash flow / Výhled** za vybraný měsíc; dole **přepínač měsíce** (`<` / „Tento měsíc ▼" / `>`).
  Výdaje = výdaje vybraného měsíce, **Výhled = výdaje příštího měsíce** (z plánovaných plateb).
  Tap na **Výdaje** → rozpad: **monochromatický prstenec (donut)** + seznam skupin kategorií
  (%/částka/pruh); **klik na kategorii rozbalí její transakce** pod ní (→ detail záznamu).
- **UI vylepšení dle feedbacku:** spodní lišta = Přehled / **Platby** / **Statistiky** / Více
  (Záznamy i **Rozpočty** jsou v „Více"), vybraná položka **podtržená** (ne bublina); ve formuláři
  transakce je **částka běžné číselné pole** (systémová klávesnice) a **kategorie se vybírá
  na samostatné obrazovce** (řádek → picker „Výběr kategorie" s hierarchií), ne přeplněná mřížka.
  **Stejný picker má i formulář plánované platby** („Nová platba") — dřív tam zůstávala stará
  mřížka všech kategorií hned pod řádkem „Účet". Ověřeno na emulátoru.

## Co je hotové (Fáze 1)
- **Repository vrstva** (`data/repo`) + **ViewModely** (Hilt) napojené reaktivně přes `Flow`.
- **Onboarding** — při prvním spuštění (žádný účet) průvodce vytvořením prvního účtu.
- **Účty** — seznam s dopočteným zůstatkem (vč. převodů), přidání, mazání (z „Více → Účty“).
- **Formulář transakce** — typy **Výdaj / Příjem / Převod**, číselná klávesnice (plnění zprava),
  výběr účtu, mřížka kategorií (s ikonami), poznámka, uložení do DB.
- **Dashboard** — čisté jmění, přehled účtů, poslední záznamy (živě z DB).
- **Záznamy** — seskupení po dnech s denními součty + souhrn příjmů/výdajů.
- **Kategorie** — seed dle Wallet (viz [kategorie-seed.md](kategorie-seed.md)): **11 skupin /
  69 podkategorií / 80 celkem**, z toho skupina **Příjem** = příjmová (INCOME), zbytek výdajové.
  Vkládá se při vytvoření DB; ikony monochrom — **rozšířený set (~69 ikon)**, takže skupina
  i (téměř) každá podkategorie mají vlastní výstižný glyph (košík, burger, šálek, kufřík,
  pumpa, kostka, …).
  DB **verze 5** — změna seedu (vč. ikon) se nasadí destruktivní migrací (reseed).

## Co je hotové (Fáze 0)
- **Gradle projekt** (Kotlin DSL, version catalog `gradle/libs.versions.toml`, wrapper 8.11.1).
- **Stack:** Kotlin 2.0.21, Compose (BOM 2024.12.01), Material 3, Hilt, Room, Navigation,
  DataStore, KSP. AGP 8.7.3, compileSdk 35, minSdk 26.
- **Monochromatické téma** (`core/designsystem/theme`): tokeny Ink/Paper/šedi, typografická
  škála pro E-Ink, ostré tvary, override Material 3 na černobílou, **vypnutý ripple**.
  Jediný motiv (bílý papír / černý inkoust) — žádné přepínání motivu.
- **Komponenty** (`core/designsystem/component`): `CalmCard` (orámovaná), `CalmTopBar`
  (linka místo stínu), `MoneyAmount` (částka se znaménkem), `EmptyState`, `SectionHeader`.
- **Navigace** (`core/navigation`): spodní lišta (Přehled / Platby / Statistiky / Více),
  kulatý FAB (+) — na Statistikách skrytý (dole je přepínač měsíce), `NavHost` **bez animací** (E-Ink).
- **Obrazovky-kostry**: Dashboard, Záznamy, Rozpočty, Více, Nový záznam (placeholdery).
- **Datová vrstva** (`data/db`): entity `Account`, `Category`, `Record` (+ enumy, konvertory),
  DAO s `Flow`, dotaz na čisté jmění, unikátní index `fioTransactionId` (dedup pro Fázi 3),
  Room databáze. DI přes Hilt (`di/DatabaseModule`).
- **Nastavení** (`data/settings`): `SettingsRepository` na DataStore (onboarding).
- **Peníze** (`core/money/Money`): formátování CZK z minor units, znaménko, `BigDecimal`.
- **Launcher ikona**: adaptivní, monochrom (značka mince + vlnka), `+` monochrome vrstva.
- **Logo**: `assets/logo/` — `calmmoney-mark.svg`, `…-mark-inverted.svg`, `…-lockup.svg`.

## ⚠️ Tady to nešlo sestavit
V tomto prostředí **chybí JDK a Android SDK**, takže projekt **nebyl zkompilován ani spuštěn**.
Kód je psaný tak, aby seděl na uvedené verze, ale **build je nutné ověřit** u tebe.

## Jak projekt otevřít / sestavit
1. **Android Studio** (Ladybug+ / novější s podporou AGP 8.7): `File → Open` → kořen `Money/`.
   Studio doplní `local.properties` s cestou k SDK a stáhne závislosti.
2. Případně CLI (vyžaduje **JDK 17** a **Android SDK**, `ANDROID_HOME`):
   ```
   ./gradlew :app:assembleDebug      # sestaví APK
   ./gradlew :app:installDebug       # nainstaluje na připojené zařízení/emulátor
   ```
3. Spustit na zařízení **Mudita Kompakt** (nebo libovolný Android E-Ink / emulátor).

## Co je vědomě vynecháno
- **Hledání záznamů** — vědomě neděláme (stačí filtr).
- **Přepínání motivu** — neděláme (jediný motiv).
- **Štítky (labels)** — vědomě neděláme (stačí kategorie).
- **Připomínky/notifikace** plánovaných plateb — neděláme.
- **Obrazovka Nastavení** (první den týdne/měsíce ap.) — neděláme (přístup je přes dlaždice ve „Více").

## Další krok
- **Fio – živý test** s reálným read-only tokenem (parser/import ověřen jen na ukázce).
- Volitelně: auto-kategorizace Fio pohybů (dle protiúčtu/VS), šablony rychlého zadání.

Viz [06-roadmap.md](06-roadmap.md). Design pravidla v [08-design-eink.md](08-design-eink.md).
