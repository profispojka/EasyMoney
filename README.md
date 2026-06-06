# Money — osobní finanční tracker (Wallet klon)

Android aplikace pro sledování příjmů a výdajů, postavená jako věrná kopie funkcí
**Wallet by BudgetBakers** ([budgetbakers.com](https://budgetbakers.com/en/products/wallet/)),
rozšířená o **konektor na Fio banku** (automatický import transakcí).

## Rozhodnutá technologie (potvrzeno)

| Oblast | Volba |
|---|---|
| Platforma | **Android, nativně** |
| Jazyk / UI | **Kotlin + Jetpack Compose (Material 3)** |
| Data | **Offline-first** (vše lokálně v telefonu), cloud sync až v pozdější fázi |
| Banka | **Fio banka — pouze import transakcí** (read-only token, žádné odchozí platby) |

## Tento adresář = plán, ne kód

Zatím jde **čistě o plán** k odsouhlasení. Žádný kód se nepíše, dokud neschválíš směr.

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

## Další krok

Projdi prosím hlavně [docs/00-prehled-a-rozsah.md](docs/00-prehled-a-rozsah.md),
[docs/02-obrazovky.md](docs/02-obrazovky.md) a [docs/07-otevrene-otazky.md](docs/07-otevrene-otazky.md).
Pak doladíme rozsah MVP a začneme zakládat projekt podle [docs/06-roadmap.md](docs/06-roadmap.md).
