# CalmMoney — osobní finanční tracker

Android aplikace pro sledování příjmů a výdajů, inspirovaná funkcemi
**Wallet by BudgetBakers** ([budgetbakers.com](https://budgetbakers.com/en/products/wallet/)),
rozšířená o **konektor na Fio banku** (automatický import transakcí).

## Rozhodnutá technologie (potvrzeno)

| Oblast | Volba |
|---|---|
| Název / package | **CalmMoney** · `cz.calmmoney` |
| Platforma | **Android, nativně** |
| Jazyk / UI | **Kotlin + Jetpack Compose (Material 3, přebarvené na monochrom)** |
| Vzhled | **Černobílý E-Ink** — monochrom, vysoký kontrast, bez animací ([docs/08](docs/08-design-eink.md)) |
| Cílové zařízení | **Mudita Kompakt** (E-Ink Android) — potvrzeno |
| Data | **Offline-first** (vše lokálně v telefonu), cloud sync až v pozdější fázi |
| Banka | **Fio banka — pouze import transakcí** (read-only token, žádné odchozí platby) |

## Stav: založen projekt (Fáze 0)

V `docs/` je kompletní plán, v kořeni je **Android projekt** (`cz.calmmoney`) — kostra
appky, monochromatický E-Ink design system a navigace. Logo v `assets/logo/`.

> ⚠️ Projekt zatím **nebyl zkompilován** (prostředí nemá JDK ani Android SDK).
> Otevři kořen `Money/` v **Android Studiu** (doplní SDK) nebo sestav přes
> `./gradlew :app:assembleDebug` (vyžaduje JDK 17 + Android SDK).
> Detail a další kroky v [docs/09-stav-projektu.md](docs/09-stav-projektu.md).

## Mapa dokumentů

| Soubor | Obsah |
|---|---|
| [docs/00-prehled-a-rozsah.md](docs/00-prehled-a-rozsah.md) | Vize, rozsah, co je a není v MVP |
| [docs/01-funkce.md](docs/01-funkce.md) | Kompletní seznam funkcí Wallet + jak je pokryjeme |
| [docs/02-obrazovky.md](docs/02-obrazovky.md) | **Detailní popis každé obrazovky** a co se na ní děje |
| [docs/03-datovy-model.md](docs/03-datovy-model.md) | Datový model (Room entity, vztahy, peníze, měny) |
| [docs/04-fio-konektor.md](docs/04-fio-konektor.md) | Fio API, token, sync, mapování polí, bezpečnost |
| [docs/05-architektura.md](docs/05-architektura.md) | Tech stack, vrstvy, knihovny, struktura projektu |
| [docs/06-roadmap.md](docs/06-roadmap.md) | Fáze vývoje a milníky |
| [docs/07-otevrene-otazky.md](docs/07-otevrene-otazky.md) | Otázky a rozhodnutí k odsouhlasení |
| [docs/08-design-eink.md](docs/08-design-eink.md) | **Vizuální styl: černobílý E-Ink** (tokeny, typografie, grafy, interakce) |
| [docs/09-stav-projektu.md](docs/09-stav-projektu.md) | **Stav kódu** — co je v Fázi 0 hotové a jak projekt sestavit |

## Další krok

Otevřít projekt v Android Studiu a ověřit build, pak **Fáze 1** (přednastavené kategorie,
onboarding, formulář transakce, napojení dashboardu na DB) dle [docs/06-roadmap.md](docs/06-roadmap.md).
