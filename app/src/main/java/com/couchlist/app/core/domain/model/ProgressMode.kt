package com.couchlist.app.core.domain.model

/**
 * How progress is tracked for a media type. SIMPLE for movies (unused), CHECKLIST
 * for TV (episodes watched / total). Derivable from MediaCategory today; kept as a
 * first-class type so future media kinds can override it.
 */
enum class ProgressMode {
    SIMPLE,
    CHECKLIST,
}
