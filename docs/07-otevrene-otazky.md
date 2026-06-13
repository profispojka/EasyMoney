# 07 — Otevřené otázky a rozhodnutí

Body k odsouhlasení, než začneme kódit. U každého je **návrh (doporučení)**.

## Produkt / rozsah
1. **Rozsah MVP** — stačí M1–M3 (účty, transakce, kategorie, rozpočty, reporty,
   **Fio import**) a zbytek až potom? **Návrh: ano.**
3. **Název appky / package id** — **Rozhodnuto: „CalmMoney“, package `cz.calmmoney`.**
   (package id odvozen od názvu — kdyžtak lze změnit před založením projektu)

## Fio konektor
4. **Historie při prvním importu** — kolik dní zpět natáhnout (30 / 90 / vlastní)?
   **Návrh: volitelné, default 90 dní.**
5. **Interval auto-sync** — jak často na pozadí (respektuje 30 s limit)?
   **Návrh: každých 6 h + tlačítko „Synchronizovat“ (bez pull-to-refresh kvůli E-Ink).**
6. **Více Fio účtů** — počítat hned s vícero tokeny/účty? **Návrh: ano, UI to umožní.**
7. **Editace Fio záznamů** — zamknout částku/datum (kvůli souladu se zůstatkem),
   povolit jen kategorii/poznámku? **Návrh: ano, zamknout částku a datum.**

## Datový model
8. **Znaménko částky** — ukládat vždy kladně + směr z `type` (čistší dotazy), nebo
   znaménkově (výdaj záporný)? **Návrh: kladně + `type`.**
9. **Model převodu** — 2 propojené záznamy (lepší pro zůstatky účtů) vs. 1 záznam se
   dvěma účty? **Návrh: 2 propojené záznamy.**
10. **Zůstatek účtu** — dopočítávat dotazem, nebo držet cache pole? **Návrh: cache pole
    aktualizované při zápisu (rychlejší dashboard), s možností přepočtu.**

## Vzhled
11. **Vizuální styl** — **Rozhodnuto: černobílý E-Ink monochrom** (styl Mudita),
    detail v [08-design-eink.md](08-design-eink.md). Logo: navrhneme minimalistické
    monochromatické. Máš nějakou představu loga / typografie?
12. **Dynamic color (Material You)** — **Rozhodnuto: nepoužívat** (monochrom).

## Cílové zařízení
15. **Cílové zařízení** — **Rozhodnuto: Mudita Kompakt (E-Ink Android).** Tech stack
    Kotlin + Compose platí; appka se na Kompakt nainstaluje jako běžná Android aplikace.
    (Pozn.: Mudita Pure běží na MuditaOS, ne Androidu — proto Kompakt.)

## Provoz
13. **Distribuce** — Google Play, nebo zatím jen interní APK na tvůj telefon?
    **Návrh: nejdřív interní APK, Play později.**
14. **Účet pro Fio token na testování** — máš Fio účet, na kterém vygeneruješ
    read-only token pro reálné testy importu? (Bez něj otestujeme jen na mocku.)

## Co potřebuju od tebe k odpálení Fáze 0
- [ ] Potvrdit rozsah MVP (otázka 1).
- [x] Název appky + package id → **CalmMoney** (`cz.calmmoney`).
- [x] Vizuální styl → **černobílý E-Ink monochrom** (otázka 11). Logo k doladění.
- [x] Cílové zařízení → **Mudita Kompakt** (E-Ink Android) (otázka 15).
- [ ] Zda máš Fio read-only token na testy (otázka 14).

Po odsouhlasení založím projekt podle [06-roadmap.md](06-roadmap.md) (Fáze 0).
