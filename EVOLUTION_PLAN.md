# EVOLUTION_PLAN

## 0. Propósito de este documento

Este archivo es la **fuente principal de verdad para la evolución de la
aplicación**.

El proyecto se desarrolla en:

`https://github.com/Threepwood98/MyApp`

La aplicación comenzó como Couchlist, centrada en películas y series
mediante TMDB, y está evolucionando hacia una aplicación Android general
para organizar, descubrir y hacer seguimiento del entretenimiento y ocio
personal.

### Estado histórico del proyecto

A efectos de este roadmap:

-   **Phase 0 --- Audit / planning:** completada.
-   **Phase 1 --- Generic Library domain / desacoplamiento inicial de
    TMDB:** completada.
-   **Phase 2 --- Lists / Groups / The Pile:** completada.
-   **Phase 3 --- Generic Tracking / Enjoying / Progress:** completada.
-   El código real del repositorio puede contener además partes de
    funcionalidades que originalmente estaban previstas para fases
    posteriores.
-   Antes de modificar una feature existente hay que inspeccionar su
    implementación real. No asumir que el número de fase describe
    perfectamente el estado del código.

### Cambio de estrategia

A partir de este punto el desarrollo pasa a ser **UI-FIRST**.

La prioridad inmediata NO es seguir añadiendo lógica, APIs o tablas.

La prioridad es construir primero la experiencia visual y de interacción
completa que queremos para la aplicación, utilizando datos mock/sample
cuando sea conveniente. Después conectaremos esa interfaz con el
dominio, Room, repositorios y proveedores reales.

La UI actual **no es sagrada**.

Si una pantalla, navegación, componente Compose o estructura visual
existente dificulta alcanzar la experiencia objetivo:

-   puede refactorizarse profundamente;
-   puede reemplazarse;
-   puede reconstruirse desde cero.

Esto NO significa recrear innecesariamente todo el proyecto Android.
Kotlin, Jetpack Compose, Material 3, Hilt, Room, Retrofit, Coil,
Navigation y la arquitectura útil existente deben conservarse cuando
aporten valor.

### AGENTS.md

**AGENTS.md ya no existe y no forma parte del flujo del proyecto.**

No lo busques. No lo recrees automáticamente. No dependas de
instrucciones externas en AGENTS.md.

Este `EVOLUTION_PLAN.md`, el código real y las referencias visuales
proporcionadas son la guía de implementación.

------------------------------------------------------------------------

# 1. MISIÓN

Actúa como:

-   Senior Android Engineer
-   Software Architect
-   Product Designer especializado en Android
-   especialista en Kotlin, Jetpack Compose, Material 3, Room y
    aplicaciones offline-first

Trabaja SOBRE el proyecto existente.

No crees otro proyecto Android salvo que exista una razón técnica
extraordinaria y explícitamente justificada.

El objetivo es convertir la aplicación en una experiencia Android de
alta calidad inspirada conceptualmente en Sofa, pero diseñada de forma
nativa para Android.

------------------------------------------------------------------------

# 2. OBJETIVO DEL PRODUCTO

La aplicación debe permitir organizar y hacer seguimiento de:

-   Películas
-   Series de TV
-   Anime
-   Libros
-   Manga
-   Comics / novelas gráficas
-   Videojuegos

La arquitectura y la UI deben permitir añadir posteriormente:

-   Podcasts
-   Audiobooks
-   Música / álbumes
-   YouTube
-   Artículos
-   Board games
-   Apps
-   Custom items

Debe ser posible responder fácilmente preguntas como:

-   ¿Qué tengo pendiente?
-   ¿Qué estoy viendo?
-   ¿Qué estoy leyendo?
-   ¿Qué estoy jugando?
-   ¿Por qué episodio voy?
-   ¿Cuál es mi siguiente episodio?
-   ¿Qué terminé este año?
-   ¿Qué juegos abandoné?
-   ¿Qué manga estoy leyendo?
-   ¿Qué quiero ver después?
-   ¿Quién me recomendó esto?
-   ¿Qué valoración le puse?
-   ¿Cuándo empecé o terminé algo?
-   ¿Qué he estado disfrutando últimamente?

La aplicación debe servir tanto a un usuario casual como a uno avanzado.

El flujo básico debe seguir siendo sencillo:

`buscar → añadir → disfrutar → registrar progreso → terminar`

La complejidad avanzada debe aparecer progresivamente.

------------------------------------------------------------------------

# 3. REFERENCIA DE PRODUCTO Y DISEÑO

La inspiración principal es Sofa.

Referencias:

-   Sofa Lists / Library
-   The Pile
-   Enjoying
-   Tracking modes
-   TV season / episode tracking
-   Planner
-   Logbook
-   Smart Lists
-   Ingredients
-   List layouts
-   Item Detail

Las screenshots proporcionadas con el proyecto son **referencias
visuales prioritarias**.

Antes de construir o rediseñar una pantalla relacionada, estudiarlas.

## Principio

**SOFA COMO MODELO DE PRODUCTO + MATERIAL 3 COMO LENGUAJE NATIVO DE
ANDROID**

No crear un clon pixel-perfect de iOS.

Conservar de Sofa:

-   jerarquía clara;
-   fuerte uso de artwork;
-   densidad de información;
-   cards;
-   agrupación;
-   progresos visibles;
-   acceso rápido a acciones;
-   navegación simple;
-   organización flexible;
-   sensación calmada/cozy;
-   presentación visual de listas;
-   tracking que se adapta al contenido.

Adaptar a Android:

-   Material 3;
-   NavigationBar / NavigationRail;
-   TopAppBar;
-   SearchBar;
-   ModalBottomSheet;
-   DropdownMenu;
-   FilterChip / AssistChip;
-   FAB cuando tenga sentido;
-   Snackbar;
-   edge-to-edge;
-   predictive back;
-   dynamic color;
-   adaptive layouts.

No copiar:

-   branding de Sofa;
-   assets propietarios;
-   textos distintivos;
-   código;
-   Liquid Glass;
-   controles Cupertino;
-   chrome de iPhone;
-   una TabBar iOS calcada.

------------------------------------------------------------------------

# 4. PRINCIPIO UI-FIRST

A partir de Phase 4, el orden de trabajo es:

1.  diseñar la experiencia;
2.  implementarla en Compose;
3.  hacerla navegable;
4.  alimentarla con sample/mock data;
5.  revisar visualmente;
6.  estabilizar el contrato UI;
7.  solo después conectar lógica y persistencia reales.

Durante la etapa UI Prototype:

-   una pantalla puede usar fake repositories;
-   puede usar fixtures;
-   puede usar preview/sample models;
-   no debe diseñarse alrededor de limitaciones accidentales del esquema
    Room actual;
-   no debe bloquearse porque una API futura aún no esté integrada.

El prototipo NO debe convertirse en código desechable de baja calidad.

Los componentes visuales creados durante esta etapa deben poder
reutilizarse cuando llegue la lógica real.

## Regla de reconstrucción

Preservar arquitectura útil.

Preservar UI **solo si sirve al diseño objetivo**.

Está permitido reemplazar por completo:

-   Home;
-   Lists;
-   Detail;
-   Search;
-   Tracking UI;
-   Navigation shell;
-   cards;
-   list rows;
-   sheets;
-   dialogs;
-   empty states;
-   componentes visuales existentes.

No conservar una pantalla simplemente porque ya existe.

------------------------------------------------------------------------

# 5. POLÍTICA DE BASE DE DATOS PRE-RELEASE

Actualmente no hay usuarios de producción ni datos que debamos
preservar.

Por tanto, durante el desarrollo pre-release:

-   los datos locales son desechables;
-   Room puede rediseñarse libremente;
-   se pueden eliminar tablas;
-   se pueden renombrar tablas;
-   se pueden recrear relaciones;
-   se puede incrementar la versión sin conservar esquemas
    experimentales;
-   se puede borrar la base de datos de desarrollo;
-   se puede usar recreación destructiva cuando sea apropiado para
    builds de desarrollo;
-   NO hay que invertir tiempo en migrar datos de versiones
    experimentales anteriores.

### Objetivo

Preferir un esquema final limpio y coherente frente a mantener
compatibilidad con decisiones experimentales.

### Importante

Esta libertad es TEMPORAL.

Antes de la primera beta/release cuyos datos queramos conservar se
declarará:

## DATABASE STABILITY BASELINE

A partir de ese punto:

-   destructive migration queda prohibida;
-   las migraciones Room explícitas pasan a ser obligatorias;
-   los tests de migración pasan a ser obligatorios;
-   no se podrán eliminar datos del usuario por cambios de esquema.

Hasta entonces, **no crear trabajo artificial de migraciones**.

------------------------------------------------------------------------

# 6. AUDITORÍA ANTES DE CADA FASE

Antes de modificar código de una fase:

1.  inspeccionar el repositorio real;
2.  leer este archivo;
3.  revisar `git status`;
4.  revisar las features y componentes afectados;
5.  identificar qué ya existe;
6.  comprobar si el README está desactualizado;
7.  determinar qué conservar, refactorizar o reemplazar.

No asumir que una feature falta porque el roadmap antiguo decía que
pertenecía a una fase posterior.

No asumir que una feature está terminada porque exista una clase con su
nombre.

El código es la fuente de verdad sobre el estado técnico.

------------------------------------------------------------------------

# 7. MODELO MENTAL DE LA APP

La estructura principal NO debe ser:

`Watchlist → Watching → Watched`

Esos conceptos pertenecen al estado o tracking.

La estructura conceptual es:

``` text
LIBRARY
│
├── The Pile
│
├── Enjoying
│
├── Lists
│   ├── Groups
│   ├── Regular Lists
│   └── Smart Lists
│
├── Tracking
│
├── Planner
│
└── Logbook
```

Un mismo item puede:

-   existir una sola vez en Library;
-   pertenecer a cero, una o varias listas;
-   estar en The Pile;
-   tener tracking activo;
-   aparecer automáticamente en Enjoying;
-   generar eventos de Logbook;
-   tener Ingredients;
-   tener Notes.

**Lista != estado != tracking != historial.**

------------------------------------------------------------------------

# 8. NAVEGACIÓN OBJETIVO

## Teléfono

La navegación principal debe estar optimizada para Android y para las
acciones más frecuentes.

Objetivo conceptual:

-   Lists
-   Enjoying o acceso equivalente de primer nivel cuando la UX lo
    justifique
-   Planner
-   Logbook
-   Search accesible de forma prominente

No copiar literalmente la distribución de tabs de Sofa.

La decisión final debe tomarse durante Phase 4 evaluando:

-   frecuencia de uso;
-   espacio disponible;
-   Material 3;
-   claridad;
-   screenshots de referencia.

## Tablet / Foldable

Preparar posteriormente:

-   NavigationRail;
-   list-detail;
-   panes adaptativos;
-   master/detail;
-   layouts que aprovechen ancho.

No escalar simplemente la UI de teléfono.

------------------------------------------------------------------------

# 9. DESIGN SYSTEM

Antes de reconstruir las pantallas principales crear una base visual
coherente.

Centralizar:

-   spacing;
-   shapes;
-   radii;
-   elevation;
-   typography;
-   icon sizes;
-   touch targets;
-   poster dimensions;
-   backdrop dimensions;
-   list row dimensions;
-   card padding;
-   section spacing;
-   progress visuals.

No dispersar números mágicos por Composables.

## Objetivo visual

La app debe sentirse:

-   cozy;
-   media-first;
-   limpia;
-   calmada;
-   visual;
-   moderna;
-   táctil;
-   densa sin resultar agobiante;
-   consistente;
-   claramente Android.

## Artwork

Poster/cover típico:

`2:3`

Backdrop/episode still:

`16:9`

Permitir otras proporciones cuando una categoría lo requiera.

## Themes

Soportar:

-   light;
-   dark;
-   system;
-   dynamic color cuando esté disponible;
-   paleta fallback cuidada.

Las referencias de Sofa pueden inspirar superficies, jerarquía y ritmo,
pero los colores no deben estar hardcodeados para imitar iOS.

------------------------------------------------------------------------

# 10. COMPONENTES VISUALES BASE

Crear/revisar componentes reutilizables como:

-   `MediaArtwork`
-   `MediaPoster`
-   `MediaBackdrop`
-   `MediaProgressRing`
-   `LibraryItemRow`
-   `LibraryItemCard`
-   `ListCard`
-   `PilePreview`
-   `EnjoyingPreview`
-   `SectionHeader`
-   `MetadataRow`
-   `TrackingSummaryCard`
-   `UpNextCard`
-   `SeasonCard`
-   `EpisodeCard`
-   `EmptyState`
-   `ErrorState`
-   `LoadingState`
-   `CategoryIcon`
-   `StatusChip`
-   `FilterChipRow`

Los nombres exactos pueden variar.

No crear componentes gigantes con decenas de flags booleanos.

Preferir composición.

------------------------------------------------------------------------

# 11. MEDIA PROGRESS RING

Debe existir un componente reusable equivalente a:

`MediaProgressRing()`

Debe soportar:

-   progress `0..1`;
-   compact;
-   normal;
-   optional center content;
-   indeterminate/unknown total cuando tenga sentido;
-   animación suave.

Debe poder aparecer en:

-   artwork;
-   list rows;
-   cards;
-   detail;
-   Enjoying;
-   TV progress;
-   book/manga progress.

------------------------------------------------------------------------

# 12. LISTS / HOME --- CONTRATO VISUAL

La pantalla principal debe inspirarse fuertemente en las referencias
proporcionadas.

Orden conceptual:

## The Pile

Inbox rápido para guardar algo antes de organizarlo.

Debe poder mostrar previews visuales de sus items.

## Enjoying

Resumen automático de contenido con tracking activo.

Ejemplos:

TV: - poster; - progress; - next episode.

Book/Manga: - cover; - progress.

Game: - cover; - playing/current status.

## Pinned / Favorite Lists

Cards visuales cuando tenga sentido.

## Library / Lists

Resto de listas y grupos.

Debe soportar tanto presentación compacta como cards.

------------------------------------------------------------------------

# 13. THE PILE

The Pile es un inbox especial.

NO es:

-   categoría;
-   tracking status;
-   media type.

Puede contener cualquier `LibraryItem`.

Flujo:

`encuentro algo → guardar rápido → organizar después`

Acciones futuras:

-   Move to list
-   Start tracking
-   Remove from Pile
-   Mark completed
-   Open detail

------------------------------------------------------------------------

# 14. LISTAS Y GRUPOS

Conceptualmente mantener:

-   `MediaList`
-   `ListGroup`
-   relación list ↔ LibraryItem

Una lista no duplica el item.

Tipos:

## TODO

Al completar un item puede ocultarse de la lista activa según
configuración.

## COLLECTION

Los completados permanecen visibles.

Funciones objetivo:

-   create;
-   edit;
-   delete;
-   duplicate;
-   reorder;
-   pin/unpin;
-   move between groups;
-   multi-select;
-   move items;
-   copy items;
-   remove items.

------------------------------------------------------------------------

# 15. LAYOUTS DE LISTAS

Soportar progresivamente:

-   LIST
-   SMALL_GRID
-   LARGE_GRID
-   DATA_CARDS

Configuración por lista:

-   layout;
-   sort;
-   group;
-   filters;
-   showLabels;
-   artwork options cuando sean útiles.

No duplicar la pantalla completa por layout.

El contenido y las acciones deben compartir modelos/componentes.

------------------------------------------------------------------------

# 16. ITEM DETAIL --- CONTRATO VISUAL

La pantalla de detalle debe ser una de las piezas visuales principales.

Estructura aproximada:

-   TopAppBar
-   artwork/backdrop
-   title
-   category/list information
-   tracking CTA o tracking summary
-   Up Next cuando aplique
-   progress
-   description
-   category metadata
-   Ingredients
-   Where to Watch cuando aplique
-   Notes
-   History / Logbook
-   overflow actions

Debe existir una base común con bloques específicos por categoría.

NO crear una única `DetailScreen` monstruosa con decenas de `if`.

------------------------------------------------------------------------

# 17. ADD FLOW

El botón de añadir debe minimizar taps.

Entrada conceptual:

-   Search media
-   Add custom item
-   Create list
-   Create group

Al seleccionar un resultado:

-   Add to The Pile
-   Add to list
-   Start Tracking

En el prototipo UI puede utilizar sample search data.

Más adelante conectará providers reales.

------------------------------------------------------------------------

# 18. SEARCH / DISCOVERY

La búsqueda final será global.

Filtros/chips:

-   All
-   Movies
-   TV
-   Anime
-   Books
-   Manga
-   Comics
-   Games

Modelo común conceptual:

`SearchResult`

Debe conservar:

-   source;
-   externalId;
-   category;
-   title;
-   artwork;
-   metadata mínima.

La UI no debe depender directamente de TMDB.

------------------------------------------------------------------------

# 19. TRACKING --- MODELO DE EXPERIENCIA

El tracking es una funcionalidad central.

Modos:

-   JUST_ENJOYING
-   QUICK_LOG
-   SIMPLE_COUNTER
-   CHECKLIST
-   JOURNAL

La UI de selección debe explicar claramente qué hace cada modo y
recomendar opciones según categoría sin obligar al usuario.

## JUST_ENJOYING

-   started date/time;
-   currently enjoying;
-   Mark as Done.

Ideal cuando el usuario no quiere microgestionar.

## QUICK_LOG

Cada acción registra una sesión/evento.

Puede incluir:

-   timestamp;
-   note opcional.

## SIMPLE_COUNTER

-   current;
-   total opcional;
-   unit.

Ejemplos:

-   pages;
-   chapters;
-   volumes;
-   issues;
-   milestones.

## CHECKLIST

Lista de unidades marcables.

Ejemplos:

-   episodes;
-   chapters;
-   volumes;
-   custom checkpoints.

## JOURNAL

Entradas con:

-   title;
-   date/time;
-   note;
-   optional image.

Ideal para milestones, experiencias y sesiones de juegos.

------------------------------------------------------------------------

# 20. ENJOYING

Enjoying NO es una lista mantenida manualmente.

Debe derivarse del tracking activo.

La UI debe mostrar información específica útil:

TV: - progress; - Up Next; - acción rápida para marcar episodio.

Book/Manga: - progress; - unidad actual.

Game: - playing; - tracking mode; - último log/milestone cuando proceda.

Debe poder agrupar visualmente por:

-   Today
-   This Week
-   Earlier

si la experiencia final lo justifica.

------------------------------------------------------------------------

# 21. TV TRACKING --- CONTRATO VISUAL

TV necesita una experiencia especializada.

La UI objetivo debe incluir:

## Show tracking overview

-   poster;
-   seasons count;
-   episodes count;
-   progress ring;
-   watched/total;
-   started date;
-   tracking type;
-   Include Specials;
-   after-watching behavior.

## Seasons

Cada season:

-   artwork;
-   season number/name;
-   date;
-   episode count;
-   completion control.

## Episodes

Cada episode:

-   still;
-   `Season X · Episode Y`;
-   title;
-   air date;
-   runtime;
-   watched state;
-   watched date cuando exista;
-   overview;
-   Add Note.

## Navigation

Debe poder saltar entre temporadas cómodamente.

Inspirarse en el selector inferior y menú de seasons de las screenshots,
adaptado a Android.

## Acciones

-   mark watched;
-   mark unwatched;
-   mark season watched;
-   mark all up to here;
-   set watched date;
-   next episode;
-   include/exclude specials.

## Up Next

Debe existir una representación visual clara del siguiente episodio no
visto.

------------------------------------------------------------------------

# 22. BOOK / MANGA / COMIC TRACKING --- CONTRATO VISUAL

## Books

Tracking posible por:

-   pages;
-   chapters;
-   checklist;
-   simple completion.

## Manga

-   chapters;
-   volumes;
-   checklist;
-   counter.

## Comics

-   issues;
-   volumes;
-   checklist;
-   counter.

El total puede ser desconocido.

La UI no debe romperse cuando `total == null`.

------------------------------------------------------------------------

# 23. VIDEO GAMES --- CONTRATO VISUAL

Estados posibles:

-   BACKLOG
-   PLAYING
-   PAUSED
-   COMPLETED
-   DROPPED

Tracking especialmente útil:

-   Just Enjoying
-   Quick Log
-   Simple Counter
-   Journal

Mostrar plataforma cuando esté disponible.

Diseñar sin bloquear futuros múltiples playthroughs.

------------------------------------------------------------------------

# 24. ANIME

Anime debe tener semántica propia aunque comparta componentes con TV.

Estados objetivo:

-   Plan to Watch
-   Watching
-   Completed
-   Paused
-   Dropped

Metadata futura:

-   episodes;
-   season/year;
-   studio;
-   genres;
-   provider id.

No convertir Anime internamente en "TMDB TV" solo por conveniencia.

------------------------------------------------------------------------

# 25. LOGBOOK --- CONTRATO VISUAL

El Logbook es historial.

NO es fuente de verdad del estado actual.

Pantalla raíz inspirada en referencias:

-   Recent
-   All
-   Stats

Vistas de actividad:

-   Today
-   Yesterday
-   This Week
-   Earlier

Eventos futuros:

-   STARTED
-   COMPLETED
-   EPISODE_WATCHED
-   PROGRESS_CHANGED
-   QUICK_LOG
-   JOURNAL_ENTRY
-   REWATCHED
-   REREAD
-   REPLAYED

Filtros por categoría.

El estado vacío debe estar diseñado, no ser texto provisional.

------------------------------------------------------------------------

# 26. PLANNER --- CONTRATO VISUAL

Crear primero como experiencia UI con sample data.

Secciones:

-   Upcoming
-   Today
-   Past
-   Someday

Tipos:

-   Events
-   Notes
-   Tasks

Upcoming Releases:

-   All Releases
-   Apps
-   Audiobooks
-   Books
-   Movies
-   Music
-   TV Episodes
-   Video Games

La implementación lógica real llegará después del prototipo visual.

No integrar múltiples APIs de releases durante la fase UI.

------------------------------------------------------------------------

# 27. INGREDIENTS / CUSTOM FIELDS --- CONTRATO VISUAL

Implementar el concepto de Ingredients de forma genérica.

Modelo conceptual futuro:

-   `CustomFieldDefinition`
-   `CustomFieldValue`

Tipos:

-   TEXT
-   NUMBER
-   DATE
-   URL
-   BOOLEAN
-   SINGLE_SELECT
-   MULTI_SELECT
-   RATING

Ejemplos:

Rating: `❤️ / 👍 / 👎`

Recommended By: `John`

Tags: `Family / Relaxing / ...`

Priority: `High / Medium / Low`

Owned: `true/false`

La UI debe permitir:

-   crear Ingredient;
-   editarlo;
-   definir opciones;
-   limitarlo a categorías;
-   mostrarlo en Detail;
-   editar valores del item.

------------------------------------------------------------------------

# 28. SMART LISTS --- CONTRATO VISUAL

Una Smart List almacena reglas, no items manuales.

La UI debe soportar conceptualmente:

-   Regular List / Smart List;
-   templates;
-   start from scratch;
-   filtros generales;
-   filtros por categoría;
-   filtros de Ingredients;
-   tracking status;
-   progress tracking;
-   genre.

No implementar AI Smart Lists como requisito inicial.

Primero debe existir un motor determinista fiable.

Operadores futuros:

-   EQUALS
-   NOT_EQUALS
-   CONTAINS
-   NOT_CONTAINS
-   GREATER_THAN
-   LESS_THAN
-   BEFORE
-   AFTER
-   IS_EMPTY
-   IS_NOT_EMPTY

------------------------------------------------------------------------

# 29. NOTES, JOURNAL Y LOGBOOK

Mantener conceptos separados.

## Item Notes

Notas personales asociadas al item.

## Journal

Parte de un tracking mode.

## Logbook

Historial de acciones/eventos.

No fusionarlos en una sola tabla/concepto solo porque todos contienen
texto.

------------------------------------------------------------------------

# 30. CUSTOM ITEMS

Debe ser posible añadir contenido no disponible en APIs.

Campos básicos:

-   title;
-   category;
-   description;
-   image;
-   URL;
-   custom fields.

La app nunca debe depender completamente de proveedores externos.

------------------------------------------------------------------------

# 31. PROTOTIPO NAVEGABLE OBLIGATORIO

Antes de comenzar la etapa de conexión completa con datos reales debe
existir un prototipo Compose navegable que demuestre al menos:

``` text
Lists
  → The Pile
  → Item Detail
  → Start Tracking
  → Choose Tracking Mode
  → Enjoying
```

``` text
Lists
  → TV Shows
  → TV Show Detail
  → Tracking
  → Season
  → Episode
  → Mark Watched
  → Up Next
```

``` text
Lists
  → Books
  → Book Detail
  → Simple Counter / Checklist
```

``` text
Lists
  → Games
  → Game Detail
  → Just Enjoying / Quick Log / Journal
```

``` text
Planner
  → Upcoming / Today / Past / Someday
  → Releases
```

``` text
Logbook
  → Recent / All / Stats
```

``` text
Create List
  → Regular / Smart
  → Filters
```

``` text
Item Detail
  → Ingredients
```

Durante esta etapa se permiten sample data y fake repositories.

------------------------------------------------------------------------

# 32. SAMPLE DATA

Crear un conjunto coherente de datos de demostración.

Debe cubrir:

-   películas;
-   TV;
-   libros;
-   juegos;
-   anime;
-   manga;
-   comics.

Incluir casos:

-   sin progreso;
-   progreso parcial;
-   completado;
-   total desconocido;
-   artwork ausente;
-   descripción larga;
-   título largo;
-   muchos episodios;
-   empty list;
-   lista grande;
-   errores simulados;
-   loading.

No depender de red para revisar el prototipo visual.

------------------------------------------------------------------------

# 33. DOMAIN MODEL --- DIRECCIÓN FUTURA

TMDB es un provider.

NO es el dominio.

Conceptualmente:

``` text
LibraryItem
  id
  category
  source
  externalId
  title
  subtitle
  description
  artwork
  backdrop
  releaseDate
  dateAdded
  metadata
  userData
```

Categorías:

-   MOVIE
-   TV_SHOW
-   ANIME
-   BOOK
-   MANGA
-   COMIC
-   VIDEO_GAME
-   PODCAST
-   AUDIOBOOK
-   MUSIC
-   CUSTOM

Los nombres concretos pueden cambiar después de inspeccionar el código.

No crear una mega tabla con decenas de columnas nullable.

Separar:

-   propiedades comunes;
-   metadata específica;
-   tracking;
-   listas;
-   custom fields;
-   historial;
-   provider identity.

------------------------------------------------------------------------

# 34. PROVIDERS

Arquitectura preparada para:

Movies / TV: - TMDB.

Anime / Manga: - AniList u otro proveedor apropiado.

Books: - Google Books / Open Library u otro proveedor.

Games: - IGDB / RAWG u otro proveedor.

Comics: - provider apropiado cuando se implemente.

No integrar todas las APIs a la vez.

Las API keys:

-   nunca hardcoded;
-   nunca committed;
-   nunca impresas en logs.

------------------------------------------------------------------------

# 35. OFFLINE-FIRST

La app final debe funcionar localmente para operaciones principales.

Sin internet debe ser posible:

-   abrir Library;
-   abrir Lists;
-   abrir items guardados;
-   consultar metadata cacheada;
-   modificar tracking;
-   marcar episodios;
-   escribir notes/journal;
-   consultar Logbook;
-   reorganizar listas.

Internet será necesario principalmente para:

-   remote search;
-   metadata nueva;
-   refresh;
-   imágenes no cacheadas;
-   servicios externos.

Diseñar mutaciones para permitir cloud sync futuro, sin implementarlo
prematuramente.

------------------------------------------------------------------------

# 36. ARQUITECTURA

Mantener como dirección:

``` text
UI
↓
ViewModel
↓
Domain repository
↓
Repository implementation
↓
Room / Remote provider
```

Durante UI Prototype se permite:

``` text
UI
↓
ViewModel / UI state
↓
Fake repository / SampleData
```

siempre que pueda reemplazarse limpiamente por repositorios reales.

Mantener cuando sea útil:

-   StateFlow;
-   immutable UI state;
-   one-shot effects;
-   Hilt;
-   coroutines.

No crear Clean Architecture ceremonial con cientos de clases sin valor.

No meter lógica de negocio en Composables.

No llamar Retrofit directamente desde ViewModels.

------------------------------------------------------------------------

# 37. ROOM --- ESQUEMA FINAL

Cuando llegue la etapa de lógica/data:

Diseñar Room a partir del dominio y del contrato UI ya estabilizado.

No diseñar tablas solo para imitar componentes visuales.

No duplicar `LibraryItem` por lista.

No usar una mega tabla multimedia.

El esquema debe soportar conceptualmente:

-   Library items;
-   provider identity;
-   category metadata;
-   lists;
-   groups;
-   list membership;
-   The Pile;
-   tracking sessions;
-   progress;
-   checklist units;
-   TV seasons/episodes;
-   notes;
-   journal;
-   logbook;
-   custom fields;
-   smart-list rules;
-   planner cuando corresponda.

Como la BD pre-release es desechable, si el esquema actual no es
apropiado:

**reemplazarlo limpiamente.**

------------------------------------------------------------------------

# 38. ACCESIBILIDAD

Desde la fase UI, no dejar accesibilidad para el final.

Requisitos:

-   touch targets \>= 48dp cuando corresponda;
-   content descriptions útiles;
-   semántica Compose;
-   font scaling;
-   contraste;
-   no depender solo de color;
-   estados seleccionados comprensibles;
-   TalkBack razonable.

------------------------------------------------------------------------

# 39. PERFORMANCE

Usar:

-   LazyColumn;
-   LazyVerticalGrid;
-   stable keys;
-   Coil;
-   flows observables cuando conectemos Room.

Evitar:

-   recomposiciones innecesarias;
-   Bitmaps en Room;
-   listas gigantes cargadas sin necesidad;
-   parsing pesado dentro de Composables.

Durante prototipo, sample data debe seguir patrones razonables para no
ocultar problemas de layout/performance.

------------------------------------------------------------------------

# 40. TESTING

## Durante UI Prototype

Priorizar:

-   compilación;
-   navegación;
-   ViewModel/UI-state tests cuando aporten valor;
-   component previews;
-   estados representativos;
-   pruebas manuales visuales.

No invertir enormes cantidades de tiempo en snapshot tests de un diseño
que todavía está cambiando.

## Durante Data/Logic

Añadir tests para:

-   list membership;
-   Smart List rules;
-   progress;
-   counters;
-   status transitions;
-   duplicate detection;
-   next TV episode;
-   season completion;
-   checklist behavior;
-   Logbook generation;
-   provider mapping.

## Migraciones

NO son prioridad durante pre-release con DB desechable.

Después de `DATABASE STABILITY BASELINE`, añadir migration tests
obligatorios.

------------------------------------------------------------------------

# 41. REGLAS PARA EL AGENTE

Para cada fase:

1.  inspeccionar implementación existente;
2.  revisar `git status` y diff;
3.  identificar qué se puede reutilizar;
4.  explicar brevemente el plan;
5.  modificar en pasos razonables;
6.  compilar frecuentemente;
7.  ejecutar tests relevantes;
8.  corregir errores;
9.  revisar el diff final;
10. actualizar documentación cuando proceda.

No declarar una fase terminada si no compila.

Comandos mínimos al final de una fase:

``` bash
./gradlew test
./gradlew assembleDebug
```

Ejecutar lint adicional cuando esté configurado y sea razonable.

No hacer commit ni push salvo instrucción explícita del usuario.

------------------------------------------------------------------------

# 42. REGLAS ESPECÍFICAS UI-FIRST

Durante Phases 4--10:

-   NO bloquear una pantalla esperando backend;
-   NO ampliar Room solo para mostrar una maqueta;
-   NO integrar una API porque falte sample data;
-   NO conservar una pantalla mediocre por evitar un refactor;
-   SÍ utilizar fake repositories;
-   SÍ utilizar sample data;
-   SÍ reemplazar UI existente;
-   SÍ crear previews;
-   SÍ probar light/dark;
-   SÍ comprobar teléfonos pequeños y grandes;
-   SÍ mantener componentes reutilizables.

Una feature visual puede considerarse terminada aunque todavía use fake
data SI:

-   el flujo es navegable;
-   los estados están representados;
-   la interacción está definida;
-   el contrato de UI está claro;
-   compila;
-   no introduce deuda estructural grave.

------------------------------------------------------------------------

# 43. NO HACER

NO:

-   convertir la app en WebView;
-   usar Flutter;
-   usar React Native;
-   abandonar Jetpack Compose;
-   meter lógica de negocio en Composables;
-   meter Retrofit en ViewModels;
-   hardcodear API keys;
-   hacer commit de secretos;
-   crear una mega tabla Media con decenas de nullable;
-   duplicar items por cada lista;
-   inferir tracking únicamente desde pertenencia a una lista;
-   usar Logbook como source of truth;
-   hacer una pantalla completamente separada para cada categoría cuando
    se pueda componer;
-   copiar iOS pixel por pixel;
-   copiar assets/branding propietarios de Sofa;
-   introducir dependencias sin razón;
-   integrar cinco providers simultáneamente;
-   construir lógica compleja que la UI aún no ha validado;
-   invertir tiempo en migraciones de bases pre-release desechables;
-   buscar o depender de `AGENTS.md`.

SÍ está permitido:

-   reconstruir toda la capa UI si mejora el resultado;
-   eliminar Composables obsoletos;
-   rediseñar navegación;
-   reemplazar Room schema durante pre-release;
-   borrar datos locales de desarrollo;
-   reemplazar implementaciones incompletas de fases anteriores si
    existe una razón clara.

------------------------------------------------------------------------

# 44. ROADMAP NUEVO

## PHASE 0 --- HISTÓRICA --- COMPLETADA

Audit / planning inicial.

No repetir salvo que sea necesario para entender el estado actual.

------------------------------------------------------------------------

## PHASE 1 --- HISTÓRICA --- COMPLETADA

Generic Library domain.

Desacoplamiento inicial de TMDB.

Trabajo previo de Room/domain.

No rehacer automáticamente.

------------------------------------------------------------------------

## PHASE 2 --- HISTÓRICA --- COMPLETADA

Lists.

Groups.

The Pile.

Trabajo previo de Home/Library.

No rehacer automáticamente, pero la UI puede reemplazarse durante el UI
Reset.

------------------------------------------------------------------------

## PHASE 3 --- HISTÓRICA --- COMPLETADA

Generic Tracking.

Enjoying.

Progress.

No continuar directamente con el antiguo Phase 4.

La estrategia cambia aquí.

------------------------------------------------------------------------

# STAGE A --- UI FOUNDATION & COMPLETE APP PROTOTYPE

## PHASE 4 --- UI RESET + DESIGN SYSTEM

### Objetivo

Establecer la identidad visual y el shell definitivo.

### Trabajo

-   auditar UI existente;
-   decidir keep/refactor/replace por pantalla;
-   theme;
-   typography;
-   spacing;
-   shapes;
-   surfaces;
-   artwork components;
-   progress ring;
-   reusable cards/rows;
-   loading/error/empty states;
-   app scaffold;
-   navigation shell;
-   sample-data infrastructure;
-   previews.

### Permitido

Rehacer desde cero los Composables necesarios.

### No hacer todavía

-   nuevas APIs;
-   TV backend complejo;
-   Planner backend;
-   Ingredients backend;
-   Smart Lists backend.

### Definition of Done

La app abre en el nuevo shell visual y los componentes base son
consistentes en light/dark.

------------------------------------------------------------------------

## PHASE 5 --- LISTS / HOME UI PROTOTYPE

Construir con sample data:

-   Lists root;
-   The Pile;
-   Enjoying preview;
-   pinned/favorite lists;
-   Library;
-   groups;
-   regular list detail;
-   TODO / Collection visual semantics;
-   list/grid/data-card presentations;
-   list menus;
-   create/edit list UI;
-   empty states;
-   multi-select visual flow.

### Definition of Done

Se puede navegar visualmente por la organización principal sin depender
de backend nuevo.

------------------------------------------------------------------------

## PHASE 6 --- SEARCH + ADD + DETAIL UI PROTOTYPE

Construir:

-   global search;
-   category chips;
-   search results;
-   Add flow;
-   Add to Pile;
-   Add to List;
-   Start Tracking CTA;
-   generic Media Detail;
-   Movie Detail sample;
-   TV Detail sample;
-   Book Detail sample;
-   Game Detail sample;
-   metadata sections;
-   Notes placeholder/flow;
-   Ingredients section placeholder.

### Definition of Done

El flujo:

`Search → Result → Detail → Add`

es navegable con sample data.

------------------------------------------------------------------------

## PHASE 7 --- GENERIC TRACKING UI PROTOTYPE

Construir UI completa para:

-   tracking setup;
-   mode recommendation;
-   JUST_ENJOYING;
-   QUICK_LOG;
-   SIMPLE_COUNTER;
-   CHECKLIST;
-   JOURNAL;
-   start date/time;
-   completion;
-   tracking summary;
-   progress editing;
-   history previews.

No rediseñar todavía la lógica central salvo lo mínimo necesario para
que el prototipo sea coherente.

### Definition of Done

Los cinco modos pueden demostrarse visualmente con ejemplos.

------------------------------------------------------------------------

## PHASE 8 --- TV UI PROTOTYPE

Construir la experiencia de TV inspirada en las screenshots:

-   Enjoying TV card;
-   Up Next;
-   show tracking detail;
-   season cards;
-   episode cards;
-   season navigation;
-   watched/unwatched;
-   watched date;
-   episode note UI;
-   mark season;
-   mark all up to here;
-   Include Specials;
-   progress;
-   empty/loading/error states.

Usar sample seasons/episodes cuando sea necesario.

### Definition of Done

Puede demostrarse:

`TV Show → Season → Episode → watched → next episode`

sin que el backend real limite la UI.

------------------------------------------------------------------------

## PHASE 9 --- LOGBOOK + PLANNER UI PROTOTYPE

### Logbook

-   root;
-   Recent;
-   All;
-   Stats;
-   activity timeline;
-   filters;
-   empty state.

### Planner

-   root;
-   Upcoming;
-   Today;
-   Past;
-   Someday;
-   Events;
-   Notes;
-   Tasks;
-   Upcoming Releases;
-   category release screens.

Sample data permitido.

### Definition of Done

Ambos destinos principales son navegables y visualmente coherentes.

------------------------------------------------------------------------

## PHASE 10 --- ADVANCED ORGANIZATION UI PROTOTYPE

Construir:

### Ingredients

-   list;
-   create;
-   edit;
-   types;
-   options;
-   category applicability;
-   values on Detail.

### Smart Lists

-   Regular / Smart selector;
-   create Smart List;
-   templates UI;
-   start from scratch;
-   rule editor;
-   category filters;
-   Ingredient filters.

### List customization

-   layout;
-   item shape;
-   sort;
-   group;
-   filters;
-   labels;
-   cover/header options.

### Gate obligatorio

Al finalizar Phase 10:

**NO empezar automáticamente Stage B.**

Primero revisar visualmente toda la aplicación instalada.

Corregir:

-   inconsistencias;
-   navegación;
-   densidad;
-   jerarquía;
-   spacing;
-   componentes duplicados;
-   flows confusos;
-   estados faltantes.

Solo cuando el prototipo represente claramente la aplicación objetivo se
congela el contrato UI inicial.

------------------------------------------------------------------------

# STAGE B --- DOMAIN / DATA IMPLEMENTATION

## PHASE 11 --- FINAL DOMAIN REVIEW + FRESH ROOM SCHEMA

Ahora sí diseñar/revisar el dominio según la UI validada.

-   LibraryItem;
-   category metadata;
-   providers;
-   lists/groups;
-   Pile;
-   tracking;
-   TV units;
-   Logbook;
-   Notes;
-   Ingredients;
-   Smart Lists.

Si Room actual no encaja:

**eliminar/recrear el esquema.**

No migrar datos experimentales.

Añadir fake → real repository boundaries limpias.

------------------------------------------------------------------------

## PHASE 12 --- LISTS / PILE PERSISTENCE

Conectar UI ya diseñada con:

-   Room;
-   repositories;
-   ViewModels;
-   list CRUD;
-   groups;
-   membership;
-   Pile;
-   ordering;
-   pinned;
-   list configuration.

Eliminar mocks de esta área cuando deje de necesitarlos.

------------------------------------------------------------------------

## PHASE 13 --- SEARCH / PROVIDERS / DETAIL

Conectar:

-   TMDB existente;
-   provider abstraction;
-   global SearchResult;
-   remote detail;
-   cache;
-   duplicate detection;
-   add flow;
-   Detail real para Movies/TV.

No introducir todavía todos los providers.

------------------------------------------------------------------------

## PHASE 14 --- GENERIC TRACKING ENGINE

Conectar la UI de tracking con lógica real.

Implementar correctamente:

-   sessions;
-   status;
-   JUST_ENJOYING;
-   QUICK_LOG;
-   SIMPLE_COUNTER;
-   CHECKLIST;
-   JOURNAL;
-   completion;
-   progress derivation;
-   Enjoying derivado.

Tracking debe ser source of truth de estado/progreso, no Logbook ni
Lists.

------------------------------------------------------------------------

## PHASE 15 --- TV ENGINE

Conectar UI de Phase 8.

Implementar:

-   TMDB seasons;
-   TMDB episodes;
-   local cache;
-   episode watched state;
-   watched dates;
-   season completion;
-   mark all up to here;
-   specials;
-   Up Next;
-   offline behavior.

------------------------------------------------------------------------

## PHASE 16 --- LOGBOOK ENGINE

Implementar historial real.

`LogEntry` conceptual:

-   id;
-   libraryItemId;
-   timestamp;
-   eventType;
-   trackingSessionId;
-   optionalNote;
-   metadata.

Generar eventos a partir de acciones reales sin convertir Logbook en
source of truth.

Conectar Recent / All / Stats.

------------------------------------------------------------------------

## PHASE 17 --- ANIME + MANGA

Elegir provider adecuado.

Implementar:

-   search;
-   metadata;
-   mapping;
-   cache;
-   tracking;
-   statuses;
-   Detail;
-   integración con Lists/Enjoying/Logbook.

Reutilizar UI existente.

------------------------------------------------------------------------

## PHASE 18 --- BOOKS

Integrar provider.

Implementar:

-   authors;
-   covers;
-   publisher;
-   pages;
-   chapters cuando estén disponibles;
-   counter/checklist;
-   search/detail/cache.

------------------------------------------------------------------------

## PHASE 19 --- VIDEO GAMES

Integrar provider.

Implementar:

-   metadata;
-   platforms;
-   release dates;
-   statuses;
-   tracking modes;
-   journal;
-   future-friendly playthrough model.

------------------------------------------------------------------------

## PHASE 20 --- COMICS

Integrar provider cuando exista una opción adecuada.

Implementar:

-   series/issues/volumes;
-   metadata;
-   tracking;
-   Detail;
-   Lists/Enjoying/Logbook.

------------------------------------------------------------------------

## PHASE 21 --- INGREDIENTS ENGINE

Implementar:

-   definitions;
-   values;
-   types;
-   options;
-   category scope;
-   Detail editing;
-   persistence.

Preparar consultas para Smart Lists.

------------------------------------------------------------------------

## PHASE 22 --- SMART LISTS ENGINE

Implementar motor determinista.

-   rules;
-   operators;
-   AND/OR si el diseño lo requiere;
-   category fields;
-   tracking fields;
-   Ingredients;
-   reactive results.

No AI como requisito.

------------------------------------------------------------------------

## PHASE 23 --- PLANNER / RELEASE DATA

Conectar Planner a datos reales donde aporte valor.

Definir claramente qué elementos son:

-   eventos del usuario;
-   tasks;
-   notes;
-   releases externas.

No mezclar releases con Logbook.

------------------------------------------------------------------------

# STAGE C --- ANDROID INTEGRATION & PRODUCTION HARDENING

## PHASE 24 --- ANDROID INTEGRATIONS

Evaluar/implementar:

-   Share Target;
-   Sharesheet;
-   widgets;
-   app shortcuts;
-   deep links;
-   notifications;
-   predictive back refinements.

Share Target debe poder aceptar:

-   URL;
-   text;

y resolver metadata o crear Custom Item.

------------------------------------------------------------------------

## PHASE 25 --- TABLET / FOLDABLE

Implementar adaptive UI real:

-   NavigationRail;
-   list-detail;
-   supporting panes;
-   master/detail;
-   responsive grids;
-   landscape.

No estirar la UI de teléfono.

------------------------------------------------------------------------

## PHASE 26 --- POLISH

-   animations;
-   transitions;
-   haptics cuando aporten valor;
-   accessibility;
-   performance;
-   error handling;
-   empty states;
-   loading states;
-   offline UX;
-   typography refinements;
-   visual consistency.

------------------------------------------------------------------------

## PHASE 27 --- DATABASE STABILITY BASELINE

Antes de beta/release con datos importantes:

1.  revisar esquema Room;
2.  eliminar restos experimentales;
3.  documentar versión baseline;
4.  desactivar política destructiva de producción;
5.  exigir migraciones explícitas futuras;
6.  añadir migration tests;
7.  validar backup/restore cuando corresponda.

A partir de aquí los datos del usuario son sagrados.

------------------------------------------------------------------------

## PHASE 28 --- RELEASE READINESS

-   secrets review;
-   ProGuard/R8;
-   crash handling;
-   analytics solo si se decide explícitamente;
-   privacy review;
-   provider attribution;
-   licenses;
-   performance;
-   startup;
-   offline testing;
-   accessibility pass;
-   release build;
-   store assets posteriormente.

------------------------------------------------------------------------

# 45. ORDEN DE IMPLEMENTACIÓN DENTRO DE UNA FASE UI

Para evitar enormes cambios no verificables:

1.  sample models;
2.  low-level components;
3.  screen layout;
4.  interactions;
5.  navigation;
6.  states;
7.  light/dark;
8.  previews;
9.  compile;
10. tests relevantes;
11. manual visual review;
12. cleanup.

No construir diez pantallas rotas simultáneamente.

------------------------------------------------------------------------

# 46. CRITERIOS DE REVISIÓN VISUAL

Para cada pantalla comparar con las referencias en términos de:

-   jerarquía;
-   densidad;
-   artwork;
-   spacing;
-   legibilidad;
-   agrupación;
-   affordances;
-   acciones principales;
-   scroll behavior;
-   empty state;
-   dark mode;
-   uso con una mano;
-   claridad.

No preguntar:

"¿Es idéntica a Sofa?"

Preguntar:

"¿Conserva las buenas ideas de Sofa y se siente como una excelente app
Android?"

------------------------------------------------------------------------

# 47. CRITERIO DE CALIDAD

La aplicación final debe sentirse como una versión Android diseñada
desde cero a partir de las mejores ideas de organización y tracking
vistas en Sofa.

No debe sentirse como:

-   un clon iOS;
-   una base de datos con UI;
-   una colección de pantallas independientes;
-   un proyecto demo;
-   una app centrada únicamente en TMDB.

Debe sentirse:

-   rápida;
-   coherente;
-   visual;
-   offline-first;
-   agradable;
-   simple en acciones frecuentes;
-   potente para usuarios avanzados.

------------------------------------------------------------------------

# 48. PRIMER OBJETIVO A PARTIR DEL ESTADO ACTUAL

Phases 1--3 están completadas.

Por tanto el siguiente trabajo es:

## PHASE 4 --- UI RESET + DESIGN SYSTEM

Antes de escribir código:

1.  inspeccionar el repositorio completo;
2.  inspeccionar específicamente toda la capa Compose;
3.  comprobar navegación actual;
4.  revisar qué features posteriores ya existen parcialmente;
5.  comparar pantallas actuales con las screenshots proporcionadas;
6.  clasificar cada pantalla/componente como:
    -   KEEP
    -   REFACTOR
    -   REPLACE
7.  proponer la estructura del nuevo design system;
8.  identificar mocks/sample data necesarios.

Después comenzar Phase 4.

No comenzar el antiguo "Phase 4 TV Tracking".

El roadmap anterior queda sustituido por este documento.

------------------------------------------------------------------------

# 49. PROMPT OPERATIVO PARA INICIAR PHASE 4

Al comenzar una nueva sesión de agente, usar este contexto:

``` text
Read EVOLUTION_PLAN.md completely before making changes.

Important project state:
- Historical Phases 0, 1, 2 and 3 are completed.
- AGENTS.md no longer exists. Do not look for it or recreate it.
- The roadmap has switched to UI-FIRST development.
- Do NOT continue the old Phase 4.
- Current local databases contain no production data and are disposable.
- Backwards-compatible Room migrations are NOT required during this pre-release stage.
- Existing Compose UI may be heavily refactored or replaced when needed.
- Preserve useful architecture, not obsolete UI.

Your task is ONLY the current Phase 4: UI RESET + DESIGN SYSTEM.

First inspect the complete repository and actual current implementation.
Do not trust the README or old phase assumptions blindly.

Before coding, report briefly:
CURRENT UI STATE
KEEP
REFACTOR
REPLACE
NAVIGATION CHANGES
DESIGN SYSTEM PLAN
SAMPLE DATA PLAN
FILES EXPECTED TO CHANGE

Then implement Phase 4 incrementally.

Do not implement Phase 5+.
Do not integrate new external providers.
Do not build Planner/Ingredients/Smart Lists backend.
Do not commit or push.

At the end run:
./gradlew test
./gradlew assembleDebug

Then report:
PHASE
CHANGED
UI COMPONENTS
NAVIGATION CHANGES
DATABASE CHANGES
TESTS
BUILD RESULT
KNOWN ISSUES
NEXT
```

------------------------------------------------------------------------

# 50. FORMATO DE REPORTE AL FINAL DE CADA FASE

``` text
PHASE

CHANGED

UI COMPONENTS

NAVIGATION CHANGES

DATABASE CHANGES

TESTS

BUILD RESULT

KNOWN ISSUES

NEXT
```

Si una categoría no aplica, indicar `None`.

No declarar `BUILD RESULT: PASS` sin haber ejecutado realmente el build
correspondiente.

------------------------------------------------------------------------

# 51. PRINCIPIO FINAL

Durante esta etapa temprana tenemos una ventaja importante:

**podemos cambiar las cosas correctamente antes de tener usuarios y
datos que mantener.**

Aprovéchala.

No conservar decisiones débiles solo por compatibilidad con versiones
que nadie utiliza.

Al mismo tiempo, no reescribir infraestructura sólida por impulso.

La prioridad es:

``` text
EXPERIENCIA OBJETIVO
        ↓
UI NAVEGABLE
        ↓
CONTRATO VISUAL ESTABLE
        ↓
DOMINIO / ROOM
        ↓
PROVIDERS
        ↓
HARDENING
        ↓
RELEASE
```

Primero construir la aplicación que queremos usar.

Después hacer que todos sus datos y servicios la alimenten
correctamente.
