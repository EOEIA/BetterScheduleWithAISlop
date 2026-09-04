# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

**Lepší rozvrh / Better Schedule** — an alternative Android client for the *Bakaláři* school schedule system, built for speed by combining cached offline data with live API data. GPLv3, published on Google Play and F-Droid. Single Gradle module (`:app`). The codebase is a Java→Kotlin migration in progress: newer code is Kotlin + Jetpack Compose, older code is Java + Android Views, and both coexist. Prefer Kotlin/Compose for new work.

Much of the code (packages, models, comments) uses Czech terms. Key vocabulary: `rozvrh` = schedule/timetable, `hodina` = lesson/hour, `den` = day, `motiv` = theme, `přihlášení` = login.

## Commands

```bash
./gradlew assembleDevelopmentDebug   # build the debug APK (use the `development` flavor)
./gradlew installDevelopmentDebug    # build + install on a connected device/emulator
./gradlew testDevelopmentDebugUnitTest   # JVM unit tests
./gradlew connectedDevelopmentDebugAndroidTest   # instrumented tests (needs a device)
./gradlew lint                       # Android lint
```

- Use the **`development`** product flavor for all local work. The `official` and `play` flavors enable Sentry crash reporting managed by the upstream author — do not build/submit crashes from those.
- Build variants combine flavor + type, e.g. `assembleOfficialRelease`. Release builds require signing secrets in `secrets.properties` (or env vars in CI); you won't have these.
- Requires JDK 17. `compileSdk`/`targetSdk` 34, `minSdk` 21.
- CI is GitLab (`.gitlab-ci.yml`) using Fastlane; the canonical remote is GitLab, mirrored to GitHub.

## Architecture

### Multi-account, repository-driven data flow

The app supports **multiple Bakaláři accounts** (identified by a `Long` account id). `MainApplication` is the composition root — it holds singletons: `rozvrhDb` (Room), `accountRepository`, `rozvrhStatusStore`, `mainScope`, and shared Jackson/Retrofit config. Access it via `context.applicationContext as MainApplication`. (`AppSingleton.java` is legacy and being phased out — only widget settings still live there.)

Data flows: **`RozvrhWebservice` (Retrofit) → `RozvrhRepository` → Room (`RozvrhDatabase`) → `LiveData` → ViewModels → UI**. The repository merges cache + network and exposes results wrapped in `Resource<T>` (`SUCCESS`/`LOADING`/`ERROR` + optional `@StringRes` message) so the UI can show cached data immediately while refreshing. Schedules are keyed by `RozvrhRecord.Key(account, monday)`; the permanent schedule uses the sentinel `Rozvrh.PERM`.

### Two schedule models — do not confuse them

- **`bakaAPI/rozvrh/rozvrh3/*` (`Rozvrh3`, `Atom3`, `Day3`, …)** — the raw wire format returned by the Bakaláři API. Deserialized with Jackson.
- **`model/rozvrh/*` (`Rozvrh`, `RozvrhDay`, `RozvrhLesson`, …)** — the app's own model, `@Serializable`, stored in Room (serialized to a text column via `Rozvrh.Converter`).
- **`RozvrhConverter.convert(...)`** is the single bridge from API model → app model (resolves changes, day types, holidays, translated strings). New API-shape handling goes in `rozvrh3`; new display logic goes in `model/rozvrh`.

### Authentication & token refresh

Bakaláři uses OAuth-style access + refresh tokens per account (stored in the `Account` Room entity). `AccountRepository` owns login and token lifecycle and is fully thread-safe: it holds a per-account `Mutex` to serialize refreshes and avoid races/duplicate refreshes. `TokenAuthenticator` (an OkHttp `Authenticator` + `Interceptor`) injects the `Bearer` token and triggers a refresh on 401 via `getFreshAccountCallback`; when a request can't be made because the token is expired and refresh failed (e.g. offline), it surfaces the synthetic code `HTTP_NO_FRESH_TOKEN = 900`. Be careful preserving this locking/retry logic when touching networking — offline correctness depends on it.

### Persistence

- **Room** (`RozvrhDatabase`, version 2) holds `RozvrhRecord` and `Account`. Schemas are exported to `app/schemas/`. Add a `Migration` in `database/RozvrhDatabase.kt` (`object Migrations`) for any schema change. Note `MIGRATION_1_2` simply drops cached schedule/account tables — cached schedules are disposable, but account data is not, so avoid destructive account migrations.
- **SharedPreferences** — app settings. Accessed via `SharedPrefs`/`SharedPrefsKt`, the `prefs` extension, `SharedPrefsLiveData`, and key constants in `PrefsConsts`. One-time upgrades between app versions live in `migration/` (`MigrationInterface`, e.g. `v1_9`).

### UI

- **Compose** is the target for new UI: `MainActivity` (Compose), `LoginScreen`, settings, theme editor, "what's new". Theming: app Compose theme in `ui/theme/`; the user-customizable **schedule** colors are a separate system in `theme/` (`RozvrhTheme`, `DefaultRozvrhThemes`) — importable/exportable and shareable as `.motiv` files (`ExportThemeActivity`/`ImportThemeActivity`).
- **Legacy Views** still render the main schedule grid: `view/rozvrhtable/` (`RozvrhLayout`, `CellView`, `DenView`, `HodinaView`) is a custom-measured table. Touch with care.
- Each screen typically pairs an Activity/Composable with a `ViewModel` (`RozvrhViewModel`, `MainActivityViewModel`, `LoginViewModel`, etc.).

### Background components (outside the app UI)

- **Persistent notification** (`notification/PermanentNotification`, `NotificationState`) — shows the next/current lesson without opening the app.
- **Home-screen widgets** (`widget/`) — small (next lesson) and wide (full day) `AppWidgetProvider`s with per-widget config activities and account selection. `WidgetsSettings` persist through `AppSingleton`.
- `UpdateBroadcastReciever` / boot receiver keep notification and widgets refreshed.

These read the same `RozvrhRepository`/`LiveData` as the UI — a schedule change propagates to all of them. `RozvrhRepository.updateTime()` must be called periodically to roll `currentWeekLD` over to the new week.

## Conventions

- Match the surrounding language: don't rewrite Java files to Kotlin unless that's the task, but write new files in Kotlin.
- Keep Czech domain terminology consistent with existing names rather than translating identifiers.
- User-facing strings go through `R.string.*` (localized cs/en); pass `@StringRes` ids around (as `Resource.message` does) rather than literal text.
- Bump `versionCode`/`versionName` in `app/build.gradle` and add a `fastlane/metadata/android/{en-US,cs-CZ}/changelogs/<versionCode>.txt` plus update `CHANGELOG.md` when releasing (see recent commits for the pattern).
