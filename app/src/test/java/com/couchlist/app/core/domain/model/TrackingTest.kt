package com.couchlist.app.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrackingTest {
    @Test
    fun `simple counter allows progress beyond completion`() {
        val details = TrackingDetails.SimpleCounter(current = 75.0, total = 50.0, unit = "pages")

        assertEquals(1.5, details.progress ?: 0.0, 0.0)
    }

    @Test
    fun `counter without a positive total is indeterminate`() {
        assertNull(TrackingDetails.SimpleCounter(10.0, null, "pages").progress)
        assertNull(TrackingDetails.SimpleCounter(10.0, 0.0, "pages").progress)
    }

    @Test
    fun `checklist progress counts leaf checkpoints only`() {
        val details = TrackingDetails.Checklist(
            listOf(
                checkpoint(id = 1, kind = TrackingCheckpointKind.GROUP, completedAt = null),
                checkpoint(id = 2, kind = TrackingCheckpointKind.ITEM, completedAt = 10),
                checkpoint(id = 3, kind = TrackingCheckpointKind.ITEM, completedAt = null),
            ),
        )

        assertEquals(0.5, details.progress ?: 0.0, 0.0)
    }

    @Test
    fun `tracking state derives compatibility status`() {
        assertEquals(MediaStatus.WATCHING, summary(TrackingState.ACTIVE).status)
        assertEquals(MediaStatus.BACKLOG, summary(TrackingState.PAUSED).status)
        assertEquals(MediaStatus.COMPLETED, summary(TrackingState.COMPLETED).status)
        assertEquals(MediaStatus.ABANDONED, summary(TrackingState.ABANDONED).status)
    }

    private fun checkpoint(
        id: Long,
        kind: TrackingCheckpointKind,
        completedAt: Long?,
    ) = TrackingCheckpoint(
        id = id,
        sessionId = 1,
        stableKey = id.toString(),
        parentId = null,
        kind = kind,
        origin = TrackingCheckpointOrigin.USER,
        label = id.toString(),
        sortOrder = id.toInt(),
        completedAt = completedAt,
        createdAt = 1,
        updatedAt = 1,
    )

    private fun summary(state: TrackingState) = TrackingSummary(
        sessionId = 1,
        mode = TrackingMode.JUST_ENJOYING,
        state = state,
        progress = null,
        startedAt = 1,
        endedAt = null,
        updatedAt = 1,
    )
}
