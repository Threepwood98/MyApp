# Couchlist — Agent Instructions

## Project

Couchlist is an original Android personal media library and tracking application.

It is inspired by the general concept of personal media organizers, but must not copy
Sofa's branding, UI, assets, or implementation.

Current media scope:
- Movies
- TV shows
- Seasons
- Episodes

The architecture must remain extensible for future media types such as games,
books, anime/manga, music, podcasts, etc.

## Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- MVVM + UDF
- StateFlow
- Coroutines / Flow
- Room
- Hilt
- Retrofit + OkHttp
- kotlinx.serialization
- Coil
- Type-safe Navigation Compose
- Single Android module with feature-oriented packages
- minSdk 26

## Architecture Principles

### MediaItem

MediaItem represents external/catalog metadata only.

It must NOT contain user-specific information.

Examples of catalog data:
- TMDB ID
- title
- original title
- overview
- poster
- backdrop
- release date
- genres
- runtime
- external rating
- vote count

### LibraryItem

LibraryItem represents the user's relationship with a MediaItem.

It may contain:
- status
- personal rating
- favorite
- notes
- addedAt
- startedAt
- completedAt
- createdAt
- updatedAt

### Status

Allowed statuses:

- BACKLOG
- WATCHING
- COMPLETED
- ABANDONED

`Watchlist` is a default TODO list and is NOT equivalent to BACKLOG status.

List membership must remain independent from media status.

### Personal Rating

Store personal ratings as:

`Int?` from 1 to 10.

The UI represents these values as 0.5–5 stars.

Do not use Double for personal ratings.

## Lists

Lists have independent membership from status.

A media item may belong to multiple lists.

Initial seeded lists:

- Watchlist — TODO
- The Pile — PILE

Only items that were previously in the old WATCHLIST state should automatically
join the Watchlist list during migration.

## Logbook

LogEntry is append-only historical data.

It must support:
- movie/show-level history
- optional episodeId
- action/type
- date
- personal rating
- notes
- createdAt

Rewatches must create new LogEntry records.

History must never be overwritten merely because an item is watched again.

## Room

Target database version:

`3`

Tables:

- media_items
- library_items
- lists
- media_list_joins
- seasons
- episodes
- log_entries
- watch_provider_cache

Season and Episode entities are currently schema preparation only.

Do NOT implement TV tracking logic until the dedicated TV tracking phase.

The database currently has no real users or production data, so migrations do not
need complex production-data preservation.

## Data Sources

Room is the source of truth for personal library data.

TMDB is the source of truth for external media metadata.

CatalogRepository should be Room-cache-first and refresh TMDB metadata when appropriate.

TMDB credentials must never be committed to Git.

Development credentials are provided through local.properties / BuildConfig.

## Navigation

Current intended bottom navigation:

- Home
- Search
- Library
- Logbook

Media detail screens should navigate using the local `mediaId`, not directly using
TMDB IDs.

## Implementation Phases

### Phase 0 — Repository Audit

Understand and document:
- existing implementation
- Room schema/version
- navigation
- repositories
- ViewModels
- TMDB integration

### Phase 1 — Normalized Data Model

Implement:

- MediaItem
- LibraryItem
- Lists
- Seasons/Episodes schema preparation
- LogEntry schema preparation
- Watch provider cache schema
- Room v3
- migration
- DAOs
- domain models
- repositories
- rewire Home/Search/Detail
- build verification

### Phase 2 — Home + Library

- Dashboard-style Home
- Library screen
- filtering
- sorting
- status handling
- list handling

### Phase 3 — TV Tracking

- seasons
- episodes
- watched episode/season/show actions
- next unwatched episode
- progress tracking

### Phase 4 — Logbook

- completion history
- rewatches
- episode history
- dates
- notes
- ratings

### Phase 5 — Personal Metadata

- favorites
- personal ratings
- notes
- rewatch handling

### Phase 6 — Search & Discovery

- debounced search
- search improvements
- detail improvements
- providers
- duplicate prevention

### Phase 7 — Lists & Organization

- custom lists
- TODO vs COLLECTION
- sorting/filtering
- multi-select
- undo actions

### Phase 8 — Statistics

Statistics should be derived from LogEntry history.

### Phase 9 — Smart Lists

Implement only after the core library/list/history architecture is stable.

### Phase 10 — Import/Export

Implement after the core data model is stable.

## Agent Behavior

Before modifying code:

1. Inspect the existing repository.
2. Inspect git status and current diff.
3. Determine what has already been implemented.
4. Never assume a previous phase is incomplete merely because the conversation
   context is missing.
5. Do not recreate existing work unnecessarily.
6. Make incremental, compilable changes.
7. Run the build after significant implementation phases.
8. Fix compilation errors before proceeding.
9. Do not introduce dependencies without a clear reason.
10. Do not implement future phases prematurely.

When switching models or sessions, use the repository and this AGENTS.md as the
source of truth rather than relying on conversation memory.