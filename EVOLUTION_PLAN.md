# MISIÓN

Actúa como un Senior Android Engineer + Software Architect + Product Designer
especializado en Kotlin, Jetpack Compose, Material 3, Room, arquitectura
offline-first y aplicaciones de catalogación multimedia.

Vas a trabajar SOBRE MI PROYECTO EXISTENTE.

Repositorio:
https://github.com/Threepwood98/MyApp

El proyecto actualmente se llama Couchlist y comenzó como una aplicación para
gestionar películas y series mediante TMDB.

NO quiero crear un proyecto Android nuevo.
NO quiero tirar el código existente y empezar desde cero.
Quiero evolucionar progresivamente la arquitectura y UI existentes.

============================================================
OBJETIVO DEL PRODUCTO
============================================================

Quiero transformar Couchlist en una aplicación Android para organizar TODO mi
entretenimiento y ocio personal.

Debe permitirme registrar, organizar y hacer seguimiento de:

- Películas
- Series de TV
- Anime
- Libros
- Manga
- Comics / novelas gráficas
- Videojuegos

La arquitectura debe permitir añadir posteriormente:

- Podcasts
- Música / álbumes
- YouTube
- Artículos
- Board games
- Apps
- Elementos personalizados

Quiero poder responder fácilmente preguntas como:

"¿Qué tengo pendiente?"
"¿Qué estoy viendo?"
"¿Qué estoy leyendo?"
"¿Qué estoy jugando?"
"¿Por qué episodio voy?"
"¿Qué terminé este año?"
"¿Qué juegos he abandonado?"
"¿Qué manga estoy leyendo?"
"¿Qué quiero ver después?"
"¿Quién me recomendó esto?"
"¿Qué valoración le puse?"
"¿Cuándo terminé este juego?"

La inspiración principal de UX y producto es Sofa:

https://www.sofahq.com/
https://www.sofahq.com/organize
https://www.sofahq.com/track
https://www.sofahq.com/features

Las screenshots de Sofa proporcionadas junto con esta tarea son REFERENCIAS
VISUALES IMPORTANTES.

Estúdialas antes de modificar la UI.

IMPORTANTE:

No quiero una copia ciega de iOS.

Quiero:

SOFA COMO MODELO DE PRODUCTO
+
MATERIAL 3 COMO LENGUAJE NATIVO DE ANDROID.

Conserva la filosofía, organización, densidad visual, cards, carátulas,
progreso, listas y facilidad de uso de Sofa, pero implementadas siguiendo
patrones Android modernos.

No copies assets, iconos propietarios, branding, textos distintivos ni código
de Sofa.

============================================================
0. ANTES DE ESCRIBIR CÓDIGO
   ============================================================

NO empieces modificando archivos inmediatamente.

Primero inspecciona TODO el repositorio.

Analiza como mínimo:

- settings.gradle.kts
- build.gradle.kts
- app/build.gradle.kts
- AndroidManifest.xml
- core/
- core/data/
- core/data/local/
- core/data/remote/
- core/data/repository/
- core/domain/
- core/ui/
- feature/home/
- feature/search/
- feature/detail/
- feature/settings/
- navegación
- modelos Room
- DAOs
- repositorios
- ViewModels
- componentes Compose
- Theme
- integración TMDB

Identifica qué existe realmente.

NO supongas que el README está completamente actualizado.

Después crea:

docs/SOFA_EVOLUTION_PLAN.md

Debe contener:

1. arquitectura actual
2. funcionalidades existentes
3. deuda técnica relevante
4. componentes reutilizables
5. componentes que necesitan generalización
6. nuevo modelo de dominio
7. cambios Room/migraciones
8. nueva navegación
9. fuentes de datos necesarias
10. fases de implementación
11. riesgos
12. decisiones técnicas

Después comienza la implementación.

NO esperes confirmación entre pequeños pasos salvo que encuentres una decisión
que pueda provocar pérdida de datos o requiera secretos/API keys que no están
disponibles.

============================================================
1. PRINCIPIO FUNDAMENTAL DEL DOMINIO
   ============================================================

El error que debemos evitar es modelar toda la aplicación alrededor de TMDB.

TMDB es UNA fuente de metadatos.

NO es nuestro dominio.

Actualmente MediaItem/MediaDetail/WatchStatus probablemente están demasiado
ligados a Movie/TV.

Refactoriza progresivamente hacia un dominio genérico.

Conceptualmente quiero algo parecido a:

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

MediaCategory:
MOVIE
TV_SHOW
ANIME
BOOK
MANGA
COMIC
VIDEO_GAME
PODCAST
MUSIC
CUSTOM

NO es obligatorio usar exactamente estos nombres.

Diseña el modelo adecuado después de estudiar el código.

Muy importante:

No crees una mega tabla absurda con 50 columnas nullable para acomodar todas
las categorías.

Usa una arquitectura que permita:

- propiedades comunes
- metadata específica por categoría
- tracking independiente
- proveedores externos independientes

La UI nunca debería necesitar saber si un Movie vino de TMDB o de otra API.

============================================================
2. SOURCE ADAPTERS / PROVIDERS
   ============================================================

Crea una abstracción de fuentes externas.

Por ejemplo conceptualmente:

MediaMetadataProvider
SearchProvider
MetadataProvider

o una solución equivalente mejor adaptada al proyecto.

Queremos poder conectar progresivamente:

Movies / TV:
TMDB (ya existe)

Anime / Manga:
AniList como primera opción

Books:
Google Books u Open Library

Games:
IGDB, RAWG u otra fuente apropiada

Comics:
arquitectura preparada para ComicVine u otro proveedor

NO integres cinco APIs simultáneamente si eso pone en peligro el proyecto.

Primero desacopla TMDB del dominio.

Después incorpora categorías gradualmente.

Las API keys nunca deben estar hardcoded ni committed.

============================================================
3. MODELO MENTAL DE LA APP
   ============================================================

La aplicación debe dejar de ser:

Watchlist -> Watching -> Watched

como estructura principal.

Eso debe convertirse en ESTADO/PROGRESO del item.

La estructura principal será:

LIBRARY
|
+-- Lists
|     +-- The Pile
|     +-- Watching
|     +-- Reading
|     +-- Playing
|     +-- Movies To Watch
|     +-- Books To Read
|     +-- etc.
|
+-- Smart Lists
|
+-- Tracking
|
+-- Logbook

Un mismo item puede estar asociado a listas y además tener un estado de
tracking.

No acoples lista = estado.

============================================================
4. NAVEGACIÓN PRINCIPAL ANDROID
   ============================================================

En teléfonos usa NavigationBar Material 3.

Destinos iniciales:

LISTS
SEARCH
LOGBOOK

Deja arquitectura preparada para:

PLANNER
PROFILE / SETTINGS

No copies literalmente la TabBar de iOS.

En tablets/foldables usa diseño adaptativo:

NavigationRail o panel lateral
+
contenido maestro/detalle cuando sea apropiado.

Usa WindowSizeClass / adaptive navigation cuando tenga sentido.

============================================================
5. HOME / LISTS
   ============================================================

La pantalla principal debe inspirarse fuertemente en las screenshots de Sofa.

Quiero una experiencia visual basada en CARÁTULAS.

Parte superior:

"The Pile"

Debe ser un inbox rápido donde guardar algo antes de decidir en qué lista
organizarlo.

Debajo:

"Enjoying" / "En progreso"

Debe mostrar automáticamente cosas que estoy:

- viendo
- leyendo
- jugando

Ejemplos:

TV:
poster + progreso + siguiente episodio

Book/Manga:
cover + progreso

Game:
cover + estado "Playing"

Después:

listas fijadas / favoritas

y finalmente:

Library / resto de listas.

Las listas deben soportar grupos.

============================================================
6. LISTAS
   ============================================================

Implementa entidades reales para:

MediaList
ListGroup
MediaListItem / relación equivalente

Una lista NO debe duplicar el objeto multimedia.

Debe guardar relaciones hacia LibraryItem.

Tipos:

TODO
COLLECTION

TODO:
elementos terminados pueden ocultarse automáticamente.

COLLECTION:
elementos terminados permanecen visibles.

Ejemplos:

Movies To Watch
Books To Read
Anime Backlog
Playing
Favorites
Best Horror Movies
Comics
etc.

Añadir:

- crear
- editar
- borrar
- duplicar
- reordenar
- pin/unpin
- mover entre grupos
- multi-select
- mover items
- copiar items
- quitar items

============================================================
7. LAYOUTS DE LISTAS
   ============================================================

Inspirándonos en Sofa, soportar progresivamente:

LIST
SMALL_GRID
LARGE_GRID
DATA_CARDS

Los layouts deben reutilizar componentes.

No dupliques toda la pantalla para cada layout.

Persistir configuración por lista:

layout
sort
group
filters
showLabels
etc.

============================================================
8. DETALLE DEL ITEM
   ============================================================

Crear una pantalla MediaDetail reutilizable.

Estructura aproximada:

TopAppBar
Hero artwork / backdrop
Título
Metadata básica
Progreso
Acciones principales

Después secciones:

Tracking
Description
Metadata
Personal fields / Ingredients
Where to watch (cuando aplique)
Notes
History / Logbook

La información cambia según categoría.

MOVIE:
year, runtime, genres, director, providers

TV:
seasons, episodes, network, status, providers

BOOK:
author, pages, publisher

MANGA:
chapters, volumes, status

GAME:
platforms, developer, release date

etc.

No hagas una DetailScreen gigante llena de:

if (type == MOVIE)
else if (type == BOOK)
...

Crea componentes específicos de categoría y componentes comunes.

============================================================
9. PROGRESS TRACKING
   ============================================================

Esta es una funcionalidad CENTRAL.

Implementa un sistema genérico inspirado en los cinco tracking modes de Sofa.

TrackingMode:

JUST_ENJOYING
QUICK_LOG
SIMPLE_COUNTER
CHECKLIST
JOURNAL

A) JUST_ENJOYING

Solo:

Started date
Started time
"Currently enjoying"
Mark as Done

Perfecto para juegos o películas cuando no quiero microgestionar progreso.

B) QUICK_LOG

Cada pulsación crea:

timestamp
opcionalmente note

Ideal para registrar sesiones/replays.

C) SIMPLE_COUNTER

current
total
unit

Ejemplos:

Book:
234 / 600 pages

Manga:
57 / 120 chapters

Comic:
14 / 30 issues

Game:
32 / 50 milestones

Mostrar progress ring.

D) CHECKLIST

Lista de checkpoints marcables.

TV:
Season -> Episodes

Book:
chapters

Manga:
chapters/volumes

Custom:
checkpoints creados por usuario

E) JOURNAL

Entradas:

title
date/time
notes
opcional imageUri

Ideal para juegos.

Ejemplo:

"Reached Act II"
"Defeated boss X"
"Finished main story"

============================================================
10. TV TRACKING
    ============================================================

TV necesita tratamiento especial.

Al comenzar una serie:

obtener seasons/episodes desde TMDB.

Guardar localmente lo necesario para permitir tracking offline.

Mostrar:

Season 1
Season 2
...

Dentro:

Episode thumbnail
SxEy
title
air date
runtime
watched checkbox

Funciones:

Mark episode watched
Mark episode unwatched
Mark entire season watched
Mark all up to this episode
Set watched date

Calcular:

watchedEpisodes / totalEpisodes

y mostrar ProgressRing.

HOME debe mostrar:

UP NEXT

con el siguiente episodio no visto.

No contar specials por defecto.

Añadir setting:

Include specials.

============================================================
11. ANIME
    ============================================================

Anime debe tener semántica propia aunque pueda compartir UI con TV.

Preparar:

episodes
status
season/year
studio
genres
AniList id

El usuario debe poder tener:

Plan to Watch
Watching
Completed
Paused
Dropped

NO obligues a Anime a convertirse en TMDB TV internamente.

============================================================
12. BOOK / MANGA / COMIC TRACKING
    ============================================================

BOOK:

tracking configurable por:

pages
chapters
simple completion

MANGA:

chapters
volumes

COMIC:

issues
volumes

No asumir que todos los proveedores conocen el total.

Permitir total = null.

============================================================
13. VIDEO GAMES
    ============================================================

Estados:

BACKLOG
PLAYING
PAUSED
COMPLETED
DROPPED

Tracking modes especialmente útiles:

Just Enjoying
Quick Log
Simple Counter
Journal

Permitir plataforma:

PC
PS5
Xbox
Switch
etc.

Un mismo juego podría eventualmente tener múltiples playthroughs.

No bloquees esa posibilidad en el esquema.

============================================================
14. LOGBOOK
    ============================================================

Crear historial central.

LogEntry:

id
libraryItemId
timestamp
eventType
trackingSessionId
optionalNote
metadata

Ejemplos:

STARTED
COMPLETED
EPISODE_WATCHED
PROGRESS_CHANGED
QUICK_LOG
JOURNAL_ENTRY
REWATCHED
REREAD
REPLAYED

Pantalla:

Today
Yesterday
This Week
Earlier

y filtros:

Movies
TV
Anime
Books
Manga
Comics
Games

El Logbook NO debe ser la fuente de verdad del estado actual.

Debe ser historial.

============================================================
15. INGREDIENTS / CUSTOM FIELDS
    ============================================================

Implementa una versión Android/genérica del concepto "Ingredients".

Nombre interno sugerido:

CustomFieldDefinition
CustomFieldValue

Tipos:

TEXT
NUMBER
DATE
URL
BOOLEAN
SINGLE_SELECT
MULTI_SELECT
RATING

Ejemplos:

Rating:
❤️ / 👍 / 👎

Recommended By:
John

Tags:
Family
Kimmy
Relaxing

Priority:
High / Medium / Low

Owned:
true/false

Custom fields pueden asociarse a:

todas las categorías
o categorías concretas.

Esto debe integrarse con Smart Lists.

============================================================
16. SMART LISTS
    ============================================================

Una Smart List no guarda manualmente sus items.

Guarda reglas.

Ejemplos:

"Anime que estoy viendo"

category == ANIME
AND trackingStatus == IN_PROGRESS

"Juegos pendientes de Switch"

category == GAME
AND platform contains SWITCH
AND status == BACKLOG

"Películas pendientes recomendadas por John"

category == MOVIE
AND completed == false
AND RecommendedBy == John

Crear:

SmartList
SmartListRule
RuleOperator

Operadores:

EQUALS
NOT_EQUALS
CONTAINS
NOT_CONTAINS
GREATER_THAN
LESS_THAN
BEFORE
AFTER
IS_EMPTY
IS_NOT_EMPTY

Las Smart Lists deben actualizarse automáticamente cuando cambien los datos.

No implementes inicialmente AI Smart Lists.
Primero haz un motor determinista sólido.

============================================================
17. SEARCH / DISCOVERY
    ============================================================

La búsqueda debe ser GLOBAL.

SearchBar Material 3.

Filtros/chips:

All
Movies
TV
Anime
Books
Manga
Comics
Games

Los resultados de distintas APIs deben convertirse a un modelo común:

SearchResult

pero conservar:

source
externalId

Al seleccionar un resultado:

1. obtener detalle
2. mostrar preview/detail
3. elegir:
    - Add to The Pile
    - Add to list
    - Start Tracking

Evitar duplicados usando:

source + externalId

y heurísticas secundarias solo cuando sea necesario.

============================================================
18. OFFLINE FIRST
    ============================================================

Mantén y amplía el principio offline-first existente.

Room es la fuente local principal.

El usuario debe poder SIN INTERNET:

- abrir Library
- abrir Lists
- ver items guardados
- ver metadata cacheada
- cambiar progreso
- marcar episodios
- escribir journal entries
- consultar Logbook
- reorganizar listas

Internet solo debe ser imprescindible para:

- búsqueda remota
- metadata nueva
- refresh
- imágenes no cacheadas

Diseña las mutaciones para poder añadir cloud sync posteriormente.

============================================================
19. ROOM Y MIGRACIONES
    ============================================================

MUY IMPORTANTE:

NO uses fallbackToDestructiveMigration.

NO destruyas los datos existentes.

Crea migraciones Room explícitas.

Los elementos existentes de Couchlist deben convertirse a LibraryItem.

Mapear:

Watchlist -> lista "Movies & TV To Watch" o estado backlog
Watching -> tracking IN_PROGRESS
Watched -> completed + Logbook

Preserva:

TMDB id
title
poster
backdrop
overview
status
timestamps existentes

Escribe tests de migración cuando sea viable.

============================================================
20. UI / DESIGN SYSTEM
    ============================================================

Objetivo visual:

cozy
media-first
clean
playful
calm
high information density
excellent cover art

Inspiración: screenshots de Sofa.

Pero Android-native.

Usar:

MaterialTheme
Material 3
dynamic color
system dark/light
edge-to-edge
predictive back
NavigationBar
NavigationRail
TopAppBar
SearchBar
ModalBottomSheet
DropdownMenu
FilterChip
AssistChip
FloatingActionButton
Snackbar
pull-to-refresh cuando corresponda

Evitar copiar:

- Liquid Glass
- barras flotantes idénticas a iOS
- navigation chrome de iPhone
- controles Cupertino
- sheets diseñados exactamente como iOS

============================================================
21. DESIGN TOKENS
    ============================================================

Centraliza:

spacing
corner radius
elevation
poster aspect ratios
card dimensions

Ejemplo conceptual:

Spacing:
xs 4
sm 8
md 12
lg 16
xl 24
xxl 32

Usa shapes suaves/redondeados.

Cards de Sofa son referencia visual,
pero implementa Surface/Card Material 3.

Poster ratio aproximado:
2:3

Backdrop:
16:9

No esparzas números mágicos por Composables.

============================================================
22. PROGRESS RING
    ============================================================

Crear componente reusable:

MediaProgressRing()

Debe soportar:

progress 0..1
compact
normal
optional center content

Usarlo:

- covers
- cards
- detail
- Enjoying
- list rows

Animación suave al actualizar.

============================================================
23. THE PILE
    ============================================================

Implementa The Pile como inbox especial.

No es una categoría.

Puede contener cualquier LibraryItem.

Objetivo:

veo algo interesante -> guardar inmediatamente -> organizar después.

Desde The Pile:

Move to list
Start tracking
Remove
Mark completed

============================================================
24. ENJOYING
    ============================================================

Enjoying no debe ser una lista mantenida manualmente.

Debe derivarse de tracking activo.

TV:
mostrar siguiente episodio.

Books/Manga:
mostrar progreso.

Games:
mostrar estado Playing.

Movies:
mostrar Just Enjoying cuando esté activo.

Esto replica la idea útil de Sofa sin acoplarla a una lista.

============================================================
25. ADD FLOW
    ============================================================

El botón + debe abrir un ModalBottomSheet Android.

Opciones:

Search media
Add custom item
Create list
Create group

Search media abre búsqueda global.

Tras seleccionar:

Add to The Pile
Add to List
Start Tracking

Minimizar número de taps.

============================================================
26. CUSTOM ITEMS
    ============================================================

El usuario debe poder añadir algo que no exista en APIs.

Campos:

title
category
description
image
URL
custom fields

Esto es importante para que la app nunca dependa completamente de servicios
externos.

============================================================
27. NOTES
    ============================================================

Cada LibraryItem puede tener notas personales.

No mezclar:

Item notes
Journal entries
Logbook

Son conceptos distintos.

============================================================
28. ANDROID-SPECIFIC FEATURES
    ============================================================

Una vez estable el core, aprovechar Android:

Home screen widgets
Share Target / Android Sharesheet
App shortcuts
Deep links
Notifications
Predictive back
Dynamic Color

Share Target debe permitir compartir:

URL
texto

hacia Couchlist y después:

resolver metadata cuando sea posible
o crear Custom Item.

============================================================
29. TABLET / FOLDABLE
    ============================================================

Las screenshots de Sofa para iPad sirven como referencia conceptual.

Para Android grande:

List-detail layout.

Izquierda:

Lists / Groups

Derecha:

contenido de la lista seleccionada.

En Detail:

list -> detail side-by-side cuando haya espacio.

NO escales simplemente la UI de teléfono.

============================================================
30. ACCESIBILIDAD
    ============================================================

Todos los controles deben tener:

contentDescription cuando sea necesario
touch targets >= 48dp
contraste apropiado
soporte font scaling
semántica Compose adecuada

No codificar información exclusivamente mediante color.

============================================================
31. PERFORMANCE
    ============================================================

Usar LazyColumn/LazyVerticalGrid.

Keys estables.

Evitar recomposiciones innecesarias.

No cargar listas gigantes completas en memoria si Room puede observarlas.

Usar Flow desde DAO -> Repository -> ViewModel.

Imágenes mediante Coil.

No almacenar Bitmaps en Room.

============================================================
32. ARQUITECTURA
    ============================================================

Conserva la filosofía actual:

UI
↓
ViewModel
↓
Domain repository
↓
Repository implementation
↓
Room / Remote providers

UI no debe hablar directamente con Retrofit/Room.

Mantener:

StateFlow
immutable UI state
one-shot effects donde sea apropiado
Hilt
coroutines

Evita abstracciones ceremoniales que no aporten valor.

No introduzcas una mega Clean Architecture con cientos de clases vacías.

============================================================
33. PACKAGE STRUCTURE
    ============================================================

Adapta la estructura existente progresivamente hacia algo parecido a:

core/
data/
local/
remote/
repository/
domain/
model/
repository/
ui/
components/
theme/
navigation/

feature/
lists/
search/
detail/
tracking/
logbook/
smartlists/
ingredients/
settings/

No reorganices todo de golpe si genera un diff gigantesco.

============================================================
34. TESTS
    ============================================================

Añade tests prioritariamente para lógica de negocio:

Room migration
Smart List rules
Progress calculations
Next TV episode
Season completion
Simple Counter
status transitions
duplicate detection

No pierdas tiempo haciendo snapshot tests de cada componente visual antes de
tener estable el dominio.

============================================================
35. REGLAS PARA TRABAJAR COMO AGENTE
    ============================================================

Trabaja iterativamente.

Para cada fase:

1. inspecciona implementación existente
2. explica brevemente qué vas a modificar
3. realiza cambios
4. ejecuta formatter/lint cuando esté configurado
5. compila
6. ejecuta tests
7. corrige errores
8. revisa el diff
9. actualiza documentación
10. continúa

NO declares una fase terminada si el proyecto no compila.

Comando mínimo:

./gradlew assembleDebug

Y tests relevantes:

./gradlew test

Si falla:

investiga y corrige.

No elimines una feature funcional simplemente porque sea más fácil reconstruirla.

============================================================
36. NO HACER
    ============================================================

NO:

- reescribir todo desde cero
- convertir la app en una WebView
- usar Flutter
- usar React Native
- abandonar Jetpack Compose
- eliminar Room
- eliminar TMDB
- meter lógica de negocio dentro de Composables
- meter Retrofit dentro de ViewModels
- hardcodear API keys
- usar fallbackToDestructiveMigration
- hacer una única tabla Media con decenas de columnas nullable
- duplicar entidades para cada lista
- crear una pantalla completamente distinta para cada media type
- copiar controles iOS pixel por pixel
- copiar branding/assets propietarios de Sofa
- introducir dependencias sin una razón clara
- romper funcionalidad offline existente
- hacer commits de secretos

============================================================
37. FASES
    ============================================================

Implementa en este orden salvo que el análisis del repo revele una dependencia
que justifique cambiarlo.

PHASE 0
Audit + documentation

PHASE 1
Generic Library domain
Room migrations
Desacoplar TMDB del dominio

PHASE 2
Lists + Groups + The Pile
Nueva Home Android inspirada en Sofa

PHASE 3
Generic tracking engine
Enjoying / In Progress
ProgressRing

PHASE 4
TV episode tracking
Seasons
Episodes
Up Next

PHASE 5
Logbook

PHASE 6
Anime/Manga provider + models

PHASE 7
Books

PHASE 8
Video games

PHASE 9
Comics

PHASE 10
Ingredients / Custom Fields

PHASE 11
Smart Lists

PHASE 12
Layouts / sorting / grouping / filtering / multi-select

PHASE 13
Android integration:
Share Target
Widgets
Deep links
Notifications

PHASE 14
Tablet/foldable adaptive UI

PHASE 15
Polish:
animations
accessibility
performance
empty states
error handling

============================================================
38. PRIMER OBJETIVO FUNCIONAL
    ============================================================

Antes de intentar implementar toda la aplicación, quiero alcanzar este
vertical slice:

La aplicación abre en Lists.

Veo:

THE PILE

ENJOYING
- TV actualmente viendo
- libro actualmente leyendo
- juego actualmente jugando

MY LISTS
- Movies To Watch
- TV Shows
- Anime
- Books To Read
- Manga
- Comics
- Games

Puedo:

buscar película/serie mediante el TMDB existente
añadirla a The Pile o una lista
abrir Detail
Start Tracking
marcar progreso
terminarla
ver la acción reflejada en Logbook

TV además debe mostrar el siguiente episodio.

Una vez que ESTE flujo funcione de extremo a extremo y compile correctamente,
continúa incorporando categorías externas.

============================================================
39. CRITERIO DE CALIDAD
    ============================================================

La app final debe sentirse como si alguien hubiera tomado las mejores ideas de
Sofa y hubiera diseñado la versión Android desde cero.

No como:

"un clon iOS corriendo en Android".

Debe sentirse:

rápida
coherente
offline-first
visual
agradable
simple para acciones comunes
potente cuando quiero organizar mucho contenido.

La complejidad debe aparecer progresivamente.

Un usuario nuevo debería poder simplemente:

buscar -> añadir -> disfrutar -> terminar

sin entender Smart Lists, Ingredients ni tracking modes.

Un usuario avanzado podrá después configurar todo.

============================================================
40. EMPIEZA AHORA
    ============================================================

Empieza inspeccionando el repositorio completo.

No escribas código todavía.

Primero produce:

docs/SOFA_EVOLUTION_PLAN.md

y muéstrame un resumen con:

CURRENT STATE
REUSABLE COMPONENTS
ARCHITECTURAL PROBLEMS
TARGET DOMAIN MODEL
DATABASE MIGRATION STRATEGY
TARGET NAVIGATION
PHASE PLAN
FILES EXPECTED TO CHANGE

Después comienza PHASE 1.

Al terminar cada fase informa únicamente:

PHASE
CHANGED
DATABASE CHANGES
TESTS
BUILD RESULT
NEXT

No me pidas que tome decisiones triviales de implementación.
Tómalas tú siguiendo las reglas anteriores.