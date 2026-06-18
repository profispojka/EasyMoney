# CalmMoney — personal finance tracker

Android app for tracking income and expenses, inspired by the features of
**Wallet by BudgetBakers** ([budgetbakers.com](https://budgetbakers.com/en/products/wallet/)),
extended with a **Fio bank connector** for automatic transaction import.

## Chosen technology (confirmed)

| Area | Choice |
|---|---|
| Name / package | **CalmMoney** · `cz.calmmoney` |
| Platform | **Android, native** |
| Language / UI | **Kotlin + Jetpack Compose (Material 3, recolored to monochrome)** |
| Visual style | **Black-and-white E-Ink** — monochrome, high contrast, no animations ([docs/08](docs/08-design-eink.md)) |
| Target device | **Mudita Kompakt** (E-Ink Android) — confirmed |
| Data | **Offline-first** (all data stored locally on the phone), cloud sync planned later |
| Bank | **Fio banka — import only** (read-only token, no outgoing payments) |

## Status: project initialized (Phase 0)

`docs/` contains the full project plan. The repository root includes an **Android project** (`cz.calmmoney`) — app skeleton, monochrome E-Ink design system, and navigation. Logo assets are in `assets/logo/`.

> ⚠️ The project has not been built yet (the current environment does not include JDK or Android SDK).
> Open the `Money/` root in **Android Studio** (it will install the SDK) or build with
> `./gradlew :app:assembleDebug` (requires JDK 17 + Android SDK).
> Details and next steps are in [docs/09-stav-projektu.md](docs/09-stav-projektu.md).

## Document map

| File | Contents |
|---|---|
| [docs/00-prehled-a-rozsah.md](docs/00-prehled-a-rozsah.md) | Vision, scope, what is and is not in the MVP |
| [docs/01-funkce.md](docs/01-funkce.md) | Complete feature list from Wallet + how we will cover it |
| [docs/02-obrazovky.md](docs/02-obrazovky.md) | **Detailed description of every screen** and its behavior |
| [docs/03-datovy-model.md](docs/03-datovy-model.md) | Data model (Room entities, relationships, money, currencies) |
| [docs/04-fio-konektor.md](docs/04-fio-konektor.md) | Fio API, token, sync, field mapping, security |
| [docs/05-architektura.md](docs/05-architektura.md) | Tech stack, layers, libraries, project structure |
| [docs/06-roadmap.md](docs/06-roadmap.md) | Development phases and milestones |
| [docs/07-otevrene-otazky.md](docs/07-otevrene-otazky.md) | Open questions and decisions to approve |
| [docs/08-design-eink.md](docs/08-design-eink.md) | **Visual style: black-and-white E-Ink** (tokens, typography, charts, interactions) |
| [docs/09-stav-projektu.md](docs/09-stav-projektu.md) | **Code status** — what is done in Phase 0 and how to build the project |

## Next step

Open the project in Android Studio and verify the build, then move to **Phase 1** (preset categories, onboarding, transaction form, dashboard wired to the DB) according to [docs/06-roadmap.md](docs/06-roadmap.md).
