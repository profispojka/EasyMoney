# Cílová struktura kategorií (dle Wallet) — ✅ IMPLEMENTOVÁNO

> Zdroj: screenshoty z Wallet by BudgetBakers. Pro CalmMoney bereme **názvy + hierarchii**,
> barvy ignorujeme (monochrom E-Ink), ikony mapujeme na náš monochrom set.
> **Stav: hotovo** — `DefaultCategories.kt` přepsán podle tohoto stromu (11 skupin /
> 69 podkategorií / 80 celkem), DB verze 5 (reseed přes destruktivní migraci).
> Ikony: rozšířený monochrom set v `CategoryIcons` (~69 klíčů), takže skupina i (skoro)
> každá podkategorie má vlastní výstižný glyph (košík, burger, šálek, kufřík, pumpa, kostka…).
> Ověřeno na emulátoru (Výdaje i Příjmy se zobrazují hierarchicky se správnými ikonami).

## Hlavní skupiny (11) — VŠECHNY KATEGORIE
1. Jídlo a pití
2. Nákupy
3. Bydlení
4. Doprava
5. Vozidlo
6. Život, zábava
7. Komunikace, PC
8. Finanční výdaje
9. Investice
10. Příjem
11. Ostatní

## Podkategorie

### 1. Jídlo a pití ✅
- Potraviny
- Restaurace, fast-food
- Bar, kavárna

### 2. Nákupy ✅ („Drogerie" potvrzeno jako poslední)
- Oděvy a obuv
- Klenoty, módní doplňky
- Zdraví, krása
- Děti
- Domácnost, zahrada
- Mazlíčci, zvířata
- Elektronika, příslušenství
- Dárky, radosti
- Kancelář, nářadí
- Volný čas
- Drogerie

### 3. Bydlení ✅
- Nájemné
- Hypotéka
- Energie, komunál
- Služby
- Údržba, opravy
- Pojištění majetku

### 4. Doprava ✅
- Veřejná doprava
- Taxi
- Dálková doprava
- Služební cesty

### 5. Vozidlo ✅
- Palivo
- Parkování
- Údržba vozidel
- Půjčování
- Pojištění vozidla
- Leasing

### 6. Život, zábava ⏳ (ověřit, zda něco je pod „Charita, dary")
- Zdravotní péče, lékař
- Wellness, krása
- Aktivní sport, fitness
- Kultura, sportovní akce
- Životní události
- Koníčky
- Vzdělávání, osobní rozvoj
- Knihy, audio, předplatné
- Televize, streaming
- Dovolená, výlety, hotely
- Charita, dary

### 7. Komunikace, PC ✅
- Telefon, mobil
- Internet
- Software, aplikace, hry
- Poštovní služby

### 8. Finanční výdaje ⏳ (ověřit, zda něco je pod „Alimenty")
- Daně
- Pojištění
- Půjčky, splátky
- Pokuty
- Poradenství
- Poplatky
- Alimenty

### 9. Investice ✅
- Nemovitosti
- Vozidla, movitý majetek
- Finanční investice
- Spoření
- Sbírky

### 10. Příjem ⏳ (ověřit, zda něco je pod „Dárky") — **typ = INCOME**, ostatní vše EXPENSE
- Plat, mzda, fakturace
- Úroky, dividendy
- Prodej
- Příjem z pronájmu
- Příspěvky a granty
- Příjem ze zapůjčení
- Šeky, kupóny, stravenky
- Loterie, hazard
- Refundace (daň, nákup)
- Alimenty
- Dárky

### 11. Ostatní ✅
- Chybějící

---

## Mapování na datový model CalmMoney
- Skupina **Příjem** + její podkategorie → `CategoryType.INCOME`.
- Všechny ostatní skupiny + podkategorie → `CategoryType.EXPENSE`.
- 11 skupin (parent) + podkategorie (child s `parentId`); `sortOrder` dle pořadí výše.
- Ikony: namapovat na náš monochrom set (`CategoryIcons.keys`), barvy se ignorují.

## Zbývá potvrdit (odrolované konce seznamů)
- **Život, zábava** — je něco pod „Charita, dary"?
- **Finanční výdaje** — je něco pod „Alimenty"?
- **Příjem** — je něco pod „Dárky"?
