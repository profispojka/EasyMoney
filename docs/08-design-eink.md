# 08 — Vizuální styl: monochromatický E-Ink

CalmMoney je navržen pro **černobílý E-Ink displej** (styl telefonů Mudita). Vzhled
i interakce vychází z omezení elektronického papíru a z „calm“ filozofie značky —
klid, jednoduchost, vysoký kontrast, žádné rušivé prvky.

## Cílové zařízení
- **Mudita Kompakt** (E-Ink Android telefon) → tech stack Kotlin + Compose
  z [05-architektura.md](05-architektura.md) **platí**; appka se instaluje běžně.
- Pozn.: Mudita **Pure** běží na MuditaOS (ne Android), proto cílíme na Kompakt.
- Vizuální systém níže je **na zařízení nezávislý** — sedí na libovolný E-Ink panel
  (Kompakt, Boox, Hisense…).

## 1. Omezení E-Ink → designová pravidla

| Omezení displeje | Důsledek pro návrh |
|---|---|
| **Jen černá a bílá** (max pár odstínů šedi) | Žádné barvy; význam nese tvar, tloušťka, pozice, ikona |
| **Pomalý překreslovací cyklus, ghosting** | Žádné animace, přechody, plynulý scroll, „živé“ prvky |
| **Reflexní displej bez podsvícení** | Maximální kontrast; velká, tučná typografie |
| **Částečné překreslení zanechává stíny** | Plné překreslení při změně obrazovky; občasný full-refresh |
| **Nízká obnovovací frekvence** | Stránkování místo nekonečného scrollu; statické grafy |

**Tři hlavní zásady:**
1. **Kontrast nad barvou** — vše čitelné jako čistá černá na bílé.
2. **Klid nad efektem** — žádný pohyb, minimum prvků, hodně bílého prostoru.
3. **Tvar nad odstínem** — rozlišuj ikonou, rámečkem, tučností a polohou, ne šedí.

## 2. Barevné (tónové) tokeny

Primárně **1-bit** (čistá černá/bílá). Odstíny šedi jen jako doplněk; na striktně
1-bitovém panelu se nahrazují **šrafováním / ditheringem**.

| Token | Hodnota | Použití |
|---|---|---|
| `ink` | `#000000` | Text, ikony, rámečky, výplně, grafy |
| `paper` | `#FFFFFF` | Pozadí |
| `gray-700` | `#3A3A3A` | (volitelně) silný sekundární text |
| `gray-500` | `#7A7A7A` | (volitelně) sekundární text, neaktivní |
| `gray-300` | `#BFBFBF` | (volitelně) jemné oddělovače |
| `gray-100` | `#E6E6E6` | (volitelně) výplň pruhů/grafů |
| `pattern-*` | šrafy/tečky | Náhrada šedi tam, kde panel zvládá jen 1-bit |

- **Žádné sémantické barvy** (zelená/červená/oranžová) — nikde.
- Výběr/aktivní stav = **inverze** (černé pozadí, bílý text), ne barevné zvýraznění.

## 3. Typografie
- **Bezpatkové, vysoce čitelné** písmo optimalizované pro E-Ink (systémové písmo
  zařízení; v návrhu např. Inter / system-sans).
- Význam nese **kontrast tloušťky** (Regular vs. Bold), ne barva.
- Omezená sada velikostí (vyšší než na LCD kvůli čitelnosti):

| Styl | Velikost / váha | Použití |
|---|---|---|
| Display | 32–40 / Bold | Hlavní částka, čisté jmění |
| Title | 22–24 / Bold | Nadpisy obrazovek, sekce |
| Body | 16–18 / Regular | Běžný text, položky seznamu |
| Label | 14–16 / Medium | Popisky polí, taby |
| Caption | 12–13 / Regular | Sekundární info, data |

- Větší řádkování a mírný letter-spacing pro ostrost na papíru.

## 4. Mřížka, prostor, komponenty
- **Ploché, orámované.** Místo stínů a elevace **plné černé linky 1–2 px**.
- **Karty** = orámované obdélníky (bez stínu, bez barevné výplně); rohy ostré nebo
  jemně zaoblené (≤ 4 px).
- **Oddělovače** = tenké černé/`gray-300` linky; v seznamech vždy jasné dělení řádků.
- **Hodně bílého prostoru** — vzdušné rozvržení, klid.
- **Tlačítka:** primární = plná černá výplň + bílý text; sekundární = orámované,
  černý text na bílé. Velké dotykové cíle (≥ 56 dp kvůli E-Ink přesnosti).
- **Pole formulářů:** orámovaná, s výrazným labelem; aktivní pole zvýrazněno tučným
  rámečkem, ne barvou.

## 5. Ikony
- **Liniové, monochromatické**, tloušťka 2 px, jednotná sada.
- Kategorie se rozlišují **ikonou + názvem** (ne barvou). Každá výchozí kategorie má
  jasně odlišitelný piktogram.
- Stavové glyfy místo barev: `⚠` přečerpáno, `●/◑/○` míra naplnění, `▲/▼` směr.

## 6. Význam bez barvy (klíčové převody)

| Kde Wallet používá barvu | CalmMoney na E-Ink |
|---|---|
| Příjem zeleně / výdaj červeně | **Znaménko a tučnost**: `+ 1 250 Kč` (bold) vs. `− 350 Kč`; volitelně `▲/▼` |
| Barevné karty účtů | Ikona účtu + název + rámeček; rozlišení tvarem, ne barvou |
| Barevné kategorie | Ikona + název; bez barvy |
| Stav rozpočtu (zelená/oranžová/červená) | **Naplněnost pruhu** (černá výplň/šrafa) + text „Zbývá / Přečerpáno o…“ + `⚠` při překročení |
| Barevné segmenty koláče | Šrafované/šedé pruhy s **přímými popisky** (viz §7) |

## 7. Grafy na E-Ink
Grafy jsou **statické** (vykreslí se jednou, žádná animace), čistě černobílé.

- **Struktura výdajů (místo barevného donutu):** **vodorovný žebříček** kategorií
  seřazený sestupně — ikona, název, částka, %, a černý pruh dle podílu. Čitelné a
  bez závislosti na barvě. (Donut jen volitelně, segmenty odlišené **šrafováním**, ne barvou.)
- **Cash flow:** skupinové sloupce — příjem = **obrys** (bílá výplň, černý rámeček),
  výdaj = **plná černá** (nebo šrafování); hodnoty popsané přímo.
- **Vývoj zůstatku:** jedna černá linka na bílé mřížce.
- Žádné gradienty, stíny, průhlednosti; popisky přímo u dat (ne barevná legenda).

## 8. Pohyb a překreslení
- **Žádné animace ani přechody** — obrazovky se přepínají okamžitě (plné překreslení).
- **Žádné spinnery / pull-to-refresh.** Synchronizace Fio = **explicitní tlačítko**
  „Synchronizovat“ (na dashboardu a v sekci připojených bank) místo tažení dolů.
- Po několika částečných překresleních provést **plný refresh** (potlačení ghostingu).
- Bez „skeleton“ loaderů; místo nich prostý text „Načítám…“ / prázdný stav.

## 9. Interakce
- **Dotyk** s velkými cíli; počítat i s nižší přesností a odezvou E-Ink.
- **Stránkování** dlouhých seznamů (např. po dnech/po stránkách) místo nekonečného
  scrollu; scroll povolen, ale ne jako hlavní vzorec.
- **Bez swipe-to-delete** (vyžaduje rychlé překreslení) — akce přes **dlouhý stisk →
  menu** nebo tlačítka v detailu (Upravit/Smazat/Duplikovat).
- Potvrzení a stavy řešit textem a inverzí, ne barvou.

## 10. Motiv
- **Jediný motiv: bílý papír / černý text** (přirozený reflexní stav E-Ink, nejméně ghostingu).
  Žádné přepínání motivu, žádná inverze celé aplikace, žádný „dynamic color“ režim.
- **Material You / dynamické barvy se nepoužívají vůbec.**
- Pozn.: „inverze“ jinde v dokumentu = **zvýraznění vybraného prvku** (černé pozadí/bílý
  text u aktivního chipu/tlačítka), ne přepínání motivu.

## 11. Realizace v Compose (shrnutí)
- Vlastní `ColorScheme` s monochromatickými tokeny (override Material 3 na černobílou);
  primárně `ink`/`paper`.
- Globálně vypnout animace: `MotionScheme`/přechody na „none“, žádné `AnimatedVisibility`,
  okamžité navigační přechody.
- Komponenty: ploché, orámované (`BorderStroke`), bez `shadow`/`tonalElevation`.
- Grafy přes **Vico** konfigurované monochromaticky (plné/obrysové/šrafované série),
  bez animací; donut nahrazen žebříčkem (jednoduchý seznam + pruhy).
- Ikony: jedna liniová sada (např. vlastní/`Material Symbols Outlined` v jedné váze).
- Téma testovat na reálném E-Ink (kontrast, ghosting, velikost písma).

## 12. Dopady na ostatní dokumenty (provedeno)
- [00](00-prehled-a-rozsah.md): princip „Material 3“ → **E-Ink monochrom**.
- [01](01-funkce.md): u účtů/kategorií zrušena **barva** (zůstává ikona); stav
  rozpočtu a příjem/výdaj bez barev.
- [02](02-obrazovky.md): donut → žebříček, zrušeny barvy karet a zelená/červená,
  pull-to-refresh → tlačítko, swipe → dlouhý stisk.
- [03](03-datovy-model.md): odstraněna pole `color` (volitelně `icon`/`shade`).
- [05](05-architektura.md): zrušen dynamic color/Material You; přidána E-Ink pravidla.
- [06](06-roadmap.md): Fáze 0 staví **monochromatický E-Ink design system**.
- [07](07-otevrene-otazky.md): styl rozhodnut (E-Ink monochrom); přidána otázka 15 (Pure vs. Kompakt).
