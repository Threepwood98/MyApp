# Couchlist

A cozy watchlist manager for movies and TV. Couchlist lets you keep a running
queue of everything you want to watch, move titles through your lists as you
actually watch them, and figure out where to stream something — all backed by
The Movie Database (TMDB).

## What it's for

Instead of juggling streaming-app "add to list" buttons scattered across
services, Couchlist keeps one personal list with three stages:

| List       | What lives there                                        |
|------------|---------------------------------------------------------|
| Watchlist  | Everything you plan to watch                            |
| Watching   | What you're currently working through                   |
| Watched    | Titles you've finished                                  |

The whole watchlist is stored **on-device** (Room), so your lists are always
available — even with no connection. Network is only needed to search for new
titles and fetch details.

## Features

- **Home — three-stage watchlist.** Tabs for Watchlist / Watching / Watched.
  Every row shows a poster, the title, media type, and when it was added.
  - **Swipe right** advances a title to the next list (Watchlist → Watching →
    Watched).
  - **Swipe left** removes it (with a snackbar confirmation).
  - **Tap a row** to open the full detail screen.
- **Search.** Type-ahead TMDB search (movies *and* TV in one grid) with
  debounced queries, poster cards, and a one-tap **Add to Watchlist** button.
  Errors surface a friendly message with a retry button.
- **Detail screen.** Backdrop, title, year · type, rating, overview, and
  **where to watch** — streaming, rental, and purchase providers grouped by
  category. Status controls let you add the title and move it between lists.
  Works **offline**: if the network is unavailable but the title is already in
  your lists, saved details are shown along with an offline banner.
- **Settings.** App version and TMDB attribution (as required by TMDB's terms).
- **Theming.** Follows your system light/dark setting; uses dynamic color on
  Android 12+, with a warm "couch" palette as the fallback.

## Tech stack

- **Language / UI:** Kotlin, Jetpack Compose (Material 3)
- **Architecture:** MVVM with MVI-style UI state + one-shot effects
  (`StateFlow` + `Channel`)
- **DI:** Hilt
- **Local storage:** Room (offline-first watchlist)
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
│   │   ├── model/           # MediaItem, MediaDetail, MediaType, WatchStatus…
│   │   └── repository/      # Repository interfaces
│   ├── ui/
│   │   ├── components/      # TMDB image URLs
│   │   ├── navigation/      # Routes + NavHost
│   │   └── theme/           # Material 3 theme
│   └── …
└── feature/
    ├── detail/              # Detail screen + ViewModel
    ├── home/                # Three-stage watchlist with swipe gestures
    ├── search/              # TMDB search
    └── settings/            # About + TMDB attribution
```

## Notes

- **Picture handling:** swiping a card advances it between lists; swiping the
  wrong way is not recoverable yet (no undo). Removing from a row is confirmed
  via snackbar.
- **Offline behavior:** your lists are always available offline. Search and
  fresh detail data need a connection; already-saved titles still open with
  their stored details.
- The app uses the TMDB API but is not endorsed or certified by TMDB.