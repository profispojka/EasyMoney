# 07 — Otevřené otázky a rozhodnutí

Body k odsouhlasení, než začneme kódit. U každého je **návrh (doporučení)**.

## Produkt / rozsah
1. **Rozsah MVP** — stačí M1–M3 (účty, transakce, kategorie, rozpočty, reporty,
   **Fio import**) a zbytek až potom? **Návrh: ano.**
2. **Jazyk UI** — čeština + angličtina od začátku? **Návrh: ano (cs výchozí).**
3. **Název appky / package id** — pracovně „Money“. Finální název? Package např.
   `cz.tvujnazev.money`. **Potřebuju od tebe jméno.**

## Fio konektor
4. **Historie při prvním importu** — kolik dní zpět natáhnout (30 / 90 / vlastní)?
   **Návrh: volitelné, default 90 dní.**
5. **Interval auto-sync** — jak často na pozadí (respektuje 30 s limit)?
   **Návrh: každých 6 h + pull-to-refresh + tlačítko.**
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
11. **Vizuální styl** — věrně podle Wallet, nebo vlastní vizuální identita (barvy,
    logo)? **Návrh: vlastní Material 3 identita inspirovaná Wallet (ne 1:1 kopie kvůli
    značce/ikonám).** Máš barevnou preferenci / logo?
12. **Dynamic color (Material You)** — přebírat barvy z tapety (Android 12+)?
    **Návrh: ano, s pevným fallbackem.**

## Provoz
13. **Distribuce** — Google Play, nebo zatím jen interní APK na tvůj telefon?
    **Návrh: nejdřív interní APK, Play později.**
14. **Účet pro Fio token na testování** — máš Fio účet, na kterém vygeneruješ
    read-only token pro reálné testy importu? (Bez něj otestujeme jen na mocku.)

## Co potřebuju od tebe k odpálení Fáze 0
- [ ] Potvrdit rozsah MVP (otázka 1).
- [ ] Název appky + package id (otázka 3).
- [ ] Případná barevná/logo preference (otázka 11).
- [ ] Zda máš Fio read-only token na testy (otázka 14).

Po odsouhlasení založím projekt podle [06-roadmap.md](06-roadmap.md) (Fáze 0).
