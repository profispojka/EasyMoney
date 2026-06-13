# 10 — Výchozí kategorie (seed)

Přednastavená sada kategorií a podkategorií, kterou appka založí při prvním spuštění
(Room `onCreate`). Vychází ze standardní taxonomie Wallet by BudgetBakers, lokalizováno
do češtiny. Ikony jsou klíče (názvy Material ikon), barvy se nepoužívají (monochrom).

> Zdroj struktury: [Wallet Help — Categories](https://support.budgetbakers.com/hc/en-us/articles/7077082048146-All-about-Categories-and-Subcategories)
> (kompletní strom není veřejně strojově dostupný; seznam je rekonstruovaná a počeštěná
> standardní taxonomie). **Klidně názvy uprav** — je to jen výchozí sada.

Implementace: `app/src/main/java/cz/calmmoney/data/db/DefaultCategories.kt`.

## Výdajové kategorie

### 🍽 Jídlo a pití
- Potraviny
- Restaurace a občerstvení
- Bar, kavárna

### 🛍 Nákupy
- Oblečení a obuv
- Elektronika
- Drogerie
- Zdraví a krása
- Domácnost a zahrada
- Děti
- Domácí mazlíčci
- Dárky
- Volný čas
- Šperky a doplňky

### 🏠 Bydlení
- Nájem
- Hypotéka
- Energie a inkaso
- Služby
- Údržba a opravy
- Pojištění domácnosti

### 🚌 Doprava
- MHD
- Taxi
- Letenky a dálková doprava

### 🚗 Vozidlo
- Palivo
- Parkování
- Údržba vozidla
- Pojištění vozidla
- Leasing

### 🎉 Život a zábava
- Sport a fitness
- Koníčky
- Kultura a akce
- Dovolená a cestování
- Zdraví a lékař
- Vzdělávání
- Knihy, audio, předplatné
- TV a streaming
- Wellness a péče
- Alkohol a tabák
- Charita
- Loterie a sázky

### 📱 Komunikace, PC
- Telefon
- Internet
- Software a aplikace
- Poštovní služby

### 🏦 Finanční výdaje
- Daně
- Pojištění
- Úvěry a úroky
- Poplatky
- Pokuty
- Poradenství

### 📈 Investice
- Spoření
- Cenné papíry
- Nemovitosti
- Sbírky

### ⋯ Ostatní
- Nezařazeno

## Příjmové kategorie
- Mzda a faktury
- Úroky a dividendy
- Prodej
- Pronájem
- Dárky
- Vratky a refundace
- Granty a dávky
- Výhry
- Ostatní příjmy

---

**Souhrn:** 10 výdajových skupin + 54 výdajových podkategorií, 9 příjmových kategorií (celkem 73).
Každá má stabilní `id` (slug) kvůli budoucím pravidlům automatické kategorizace Fio.
