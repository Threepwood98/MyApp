# Couchlist

A cozy watchlist manager for movies and TV. Couchlist lets you keep a running
queue of everything you want to watch, move titles through your lists as you
actually watch them, and figure out where to stream something — all backed by
The Movie Database (TMDB).

## What it's for

Instead of juggling streaming-app "add to list" buttons scattered across
services, Couchlist keeps catalog metadata, personal watch state, and list
membership separate. Titles move through four watch states:

| Status     | What lives there                                        |
|------------|---------------------------------------------------------|
| Backlog    | Everything you plan to watch                            |
| Watching   | What you're currently working through                   |
| Completed  | Titles you've finished                                  |
| Abandoned  | Titles you chose not to finish                          |

The whole watchlist is stored **on-device** (Room), so your lists are always
available — even with no connection. Network is only needed to search for new
titles and fetch details.

## Features

- **Home dashboard.** Continue Watching, Recently Added, and Recently Completed
  sections keep the most useful titles close.
- **Library.** Default Watchlist and The Pile list summaries plus Backlog /
  Watching / Completed / Abandoned status tabs.
  - **Swipe right** advances a title to its next status.
  - **Swipe left** removes it from the library and its lists.
  - Both actions offer **Undo** from the snackbar.
  - **Tap a row** to open the full detail screen.
- **Search.** Type-ahead TMDB search (movies *and* TV in one grid) with
  debounced queries, poster cards, and a one-tap **Add to Watchlist** button.
  Errors surface a friendly message with a retry button.
- **Detail screen.** Backdrop, title, year · type, rating, overview, and
  **where to watch** — streaming, rental, and purchase providers grouped by
  category. Status controls let you add the title and move it between lists.
  Works **offline**: if the network is unavailable but the title is already in
  your lists, saved details are shown along with an offline banner.
- **Settings.** System/light/dark theme modes, dynamic-color control, streaming
  provider region, app version, and TMDB attribution.
- **Theming.** Uses dynamic color on Android 12+ when enabled, with a warm
  "couch" palette as the fallback.

## Tech stack

- **Language / UI:** Kotlin, Jetpack Compose (Material 3)
- **Architecture:** MVVM with MVI-style UI state + one-shot effects
  (`StateFlow` + `Channel`)
- **DI:** Hilt
- **Local storage:** Room (offline-first catalog/library) + Preferences DataStore
- **Networking:** Retrofit + OkHttp + `kotlinx.serialization`
- **Images:** Coil 3
- **Navigation:** Navigation Compose with type-safe `@Serializable` routes
- **Minimum SDK:** 26 · Target/compile SDK: 37
- Backed by the [TMDB API](https://www.themoviedb.org/documentation/api)

## How to build and run

1. **Add an API key.** Couchlist reads `TMDB_API_KEY` from
   `local.properties` (which is git-ignored):

   ```properties
   TMDB_API_KEY=your_key_here
   ```

   Get a key by creating an account at
   [themoviedb.org](https://www.themoviedb.org/signup) and requesting API
   access. Without a key the app builds fine, but search and details will
   return errors at runtime.

2. **Build & install.**

   ```bash
   ./gradlew assembleDebug
   ```

   Then install the APK at `app/build/outputs/apk/debug/app-debug.apk`, or run
   it from Android Studio on an emulator/device.

   > Build requires the JDK bundled with Android Studio (`jbr`), e.g.:
   > `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"` on
   > Windows before invoking the Gradle wrapper.

## Project layout

```
app/src/main/java/com/couchlist/app/
├── core/
│   ├── common/              # Shared error helpers
│   ├── data/
│   │   ├── di/              # Hilt modules (Room, Retrofit)
│   │   ├── local/           # Room database, entity, DAO
│   │   ├── remote/          # TMDB API, DTOs, API-key interceptor
│   │   └── repository/      # Repository implementations
│   ├── domain/
│   │   ├── model/           # Catalog, library, list, status, and settings models
│   │   └── repository/      # Repository interfaces
│   ├── ui/
│   │   ├── components/      # TMDB image URLs
│   │   ├── navigation/      # Routes + NavHost
│   │   └── theme/           # Material 3 theme
│   └── …
└── feature/
    ├── detail/              # Detail screen + ViewModel
    ├── home/                # Continue/recent dashboard
    ├── library/             # Lists, status tabs, and undoable swipes
    ├── search/              # TMDB search
    └── settings/            # About + TMDB attribution
```

## Notes

- **Swipe handling:** status changes and removals are recoverable through the
  snackbar Undo action.
- **Offline behavior:** your lists are always available offline. Search and
  fresh detail data need a connection; already-saved titles still open with
  their stored details.
- The app uses the TMDB API but is not endorsed or certified by TMDB.
