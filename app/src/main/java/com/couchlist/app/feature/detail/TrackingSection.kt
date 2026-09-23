package com.couchlist.app.feature.detail

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.couchlist.app.core.domain.model.TrackingCheckpointKind
import com.couchlist.app.core.domain.model.TrackingDetails
import com.couchlist.app.core.domain.model.TrackingMode
import com.couchlist.app.core.domain.model.TrackingSession
import com.couchlist.app.core.domain.model.TrackingState
import com.couchlist.app.core.ui.components.MediaProgressRing
import java.text.NumberFormat

@Composable
internal fun TrackingSection(
    session: TrackingSession?,
    defaultMode: TrackingMode,
    defaultUnit: String?,
    onStart: (TrackingMode, Double?, String?) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onComplete: () -> Unit,
    onAbandon: () -> Unit,
    onUpdateCounter: (Double, Double?, String?) -> Unit,
    onAddCheckpoint: (String) -> Unit,
    onCheckpointCompleted: (Long, Boolean) -> Unit,
    onAddQuickLog: (String?) -> Unit,
    onAddJournalEntry: (String, String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showModeDialog by remember { mutableStateOf(false) }
    var showCounterDialog by remember { mutableStateOf(false) }
    var showCheckpointDialog by remember { mutableStateOf(false) }
    var showQuickLogDialog by remember { mutableStateOf(false) }
    var showJournalDialog by remember { mutableStateOf(false) }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Tracking", style = MaterialTheme.typography.titleLarge)
            if (session == null) {
                Text(
                    "Keep this separate from your lists and choose as much detail as you want.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = { onStart(defaultMode, null, defaultUnit) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Start tracking")
                }
                TextButton(onClick = { showModeDialog = true }) {
                    Text("Choose tracking mode")
                }
            } else {
                TrackingHeader(session)
                TrackingModeContent(
                    session = session,
                    editable = session.state == TrackingState.ACTIVE || session.state == TrackingState.PAUSED,
                    onEditCounter = { showCounterDialog = true },
                    onAddCheckpoint = { showCheckpointDialog = true },
                    onCheckpointCompleted = onCheckpointCompleted,
                    onAddQuickLog = { showQuickLogDialog = true },
                    onAddJournalEntry = { showJournalDialog = true },
                )
                TrackingActions(
                    state = session.state,
                    onPause = onPause,
                    onResume = onResume,
                    onComplete = onComplete,
                    onAbandon = onAbandon,
                    onStartAgain = {
                        val counter = session.details as? TrackingDetails.SimpleCounter
                        onStart(session.mode, counter?.total, counter?.unit)
                    },
                )
            }
        }
    }

    if (showModeDialog) {
        TrackingModeDialog(
            selected = defaultMode,
            onDismiss = { showModeDialog = false },
            onSelect = { mode ->
                showModeDialog = false
                onStart(mode, null, if (mode == TrackingMode.SIMPLE_COUNTER) defaultUnit else null)
            },
        )
    }
    if (showCounterDialog) {
        val counter = session?.details as? TrackingDetails.SimpleCounter
        CounterDialog(
            counter = counter,
            onDismiss = { showCounterDialog = false },
            onConfirm = { current, total, unit ->
                showCounterDialog = false
                onUpdateCounter(current, total, unit)
            },
        )
    }
    if (showCheckpointDialog) {
        TextEntryDialog(
            title = "Add checkpoint",
            label = "Checkpoint",
            onDismiss = { showCheckpointDialog = false },
            onConfirm = {
                showCheckpointDialog = false
                onAddCheckpoint(it)
            },
        )
    }
    if (showQuickLogDialog) {
        TextEntryDialog(
            title = "Quick log",
            label = "Note (optional)",
            allowBlank = true,
            onDismiss = { showQuickLogDialog = false },
            onConfirm = {
                showQuickLogDialog = false
                onAddQuickLog(it.ifBlank { null })
            },
        )
    }
    if (showJournalDialog) {
        JournalDialog(
            onDismiss = { showJournalDialog = false },
            onConfirm = { title, notes ->
                showJournalDialog = false
                onAddJournalEntry(title, notes)
            },
        )
    }
}

@Composable
private fun TrackingHeader(session: TrackingSession) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        session.progress?.let { progress ->
            MediaProgressRing(progress = progress) {
                Text(
                    NumberFormat.getPercentInstance().format(progress.coerceIn(0.0, 1.0)),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(session.mode.displayName, style = MaterialTheme.typography.titleMedium)
            Text(
                session.state.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "Started ${DateUtils.getRelativeTimeSpanString(session.startedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TrackingModeContent(
    session: TrackingSession,
    editable: Boolean,
    onEditCounter: () -> Unit,
    onAddCheckpoint: () -> Unit,
    onCheckpointCompleted: (Long, Boolean) -> Unit,
    onAddQuickLog: () -> Unit,
    onAddJournalEntry: () -> Unit,
) {
    when (val details = session.details) {
        TrackingDetails.JustEnjoying -> Text(
            "Currently enjoying. No progress updates required.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        is TrackingDetails.SimpleCounter -> {
            val value = buildString {
                append(details.current.formatCounter())
                details.total?.let { append(" / ${it.formatCounter()}") }
                details.unit?.let { append(" $it") }
            }
            Text(value, style = MaterialTheme.typography.headlineSmall)
            if (editable) {
                OutlinedButton(onClick = onEditCounter) { Text("Update progress") }
            }
        }
        is TrackingDetails.Checklist -> {
            details.checkpoints.forEach { checkpoint ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (checkpoint.kind == TrackingCheckpointKind.ITEM) {
                        Checkbox(
                            checked = checkpoint.completedAt != null,
                            onCheckedChange = if (editable) {
                                { onCheckpointCompleted(checkpoint.id, it) }
                            } else {
                                null
                            },
                            enabled = editable,
                        )
                    }
                    Text(
                        checkpoint.label,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (details.checkpoints.isEmpty()) {
                Text(
                    "No checkpoints yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (editable) {
                OutlinedButton(onClick = onAddCheckpoint) { Text("Add checkpoint") }
            }
        }
        is TrackingDetails.QuickLog -> {
            Text(
                "${details.entries.size} ${if (details.entries.size == 1) "session" else "sessions"} logged",
            )
            details.entries.take(3).forEach { entry ->
                Text(
                    entry.note ?: DateUtils.getRelativeTimeSpanString(entry.occurredAt).toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (editable) {
                OutlinedButton(onClick = onAddQuickLog) { Text("Log now") }
            }
        }
        is TrackingDetails.Journal -> {
            details.entries.take(3).forEach { entry ->
                Column {
                    Text(entry.title, style = MaterialTheme.typography.bodyLarge)
                    entry.notes?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            if (details.entries.isEmpty()) {
                Text("No journal entries yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (editable) {
                OutlinedButton(onClick = onAddJournalEntry) { Text("Add journal entry") }
            }
        }
    }
}

@Composable
private fun TrackingActions(
    state: TrackingState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onComplete: () -> Unit,
    onAbandon: () -> Unit,
    onStartAgain: () -> Unit,
) {
    when (state) {
        TrackingState.ACTIVE, TrackingState.PAUSED -> {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state == TrackingState.ACTIVE) {
                    OutlinedButton(onClick = onPause, modifier = Modifier.weight(1f)) { Text("Pause") }
                } else {
                    Button(onClick = onResume, modifier = Modifier.weight(1f)) { Text("Resume") }
                }
                Button(onClick = onComplete, modifier = Modifier.weight(1f)) { Text("Complete") }
            }
            TextButton(onClick = onAbandon) { Text("Abandon") }
        }
        TrackingState.COMPLETED, TrackingState.ABANDONED -> Button(
            onClick = onStartAgain,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Start again")
        }
    }
}

@Composable
private fun TrackingModeDialog(
    selected: TrackingMode,
    onDismiss: () -> Unit,
    onSelect: (TrackingMode) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tracking mode") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TrackingMode.entries.forEach { mode ->
                    FilterChip(
                        selected = mode == selected,
                        onClick = { onSelect(mode) },
                        label = { Text(mode.displayName) },
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun CounterDialog(
    counter: TrackingDetails.SimpleCounter?,
    onDismiss: () -> Unit,
    onConfirm: (Double, Double?, String?) -> Unit,
) {
    var current by remember(counter) { mutableStateOf(counter?.current?.formatCounter().orEmpty()) }
    var total by remember(counter) { mutableStateOf(counter?.total?.formatCounter().orEmpty()) }
    var unit by remember(counter) { mutableStateOf(counter?.unit.orEmpty()) }
    val parsedCurrent = current.toDoubleOrNull()
    val parsedTotal = total.takeIf { it.isNotBlank() }?.toDoubleOrNull()
    val valid = parsedCurrent?.let { it.isFinite() && it >= 0.0 } == true &&
        (total.isBlank() || parsedTotal?.let { it.isFinite() && it >= 0.0 } == true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update progress") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(current, { current = it }, label = { Text("Current") }, singleLine = true)
                OutlinedTextField(total, { total = it }, label = { Text("Total (optional)") }, singleLine = true)
                OutlinedTextField(unit, { unit = it }, label = { Text("Unit (optional)") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                enabled = valid,
                onClick = { onConfirm(checkNotNull(parsedCurrent), parsedTotal, unit.ifBlank { null }) },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun TextEntryDialog(
    title: String,
    label: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    allowBlank: Boolean = false,
) {
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value, { value = it }, label = { Text(label) }, modifier = Modifier.fillMaxWidth()) },
        confirmButton = {
            Button(enabled = allowBlank || value.isNotBlank(), onClick = { onConfirm(value.trim()) }) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun JournalDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Journal entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true)
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes (optional)") }, minLines = 3)
            }
        },
        confirmButton = {
            Button(
                enabled = title.isNotBlank(),
                onClick = { onConfirm(title.trim(), notes.trim().ifBlank { null }) },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private val TrackingState.displayName: String
    get() = when (this) {
        TrackingState.ACTIVE -> "Active"
        TrackingState.PAUSED -> "Paused"
        TrackingState.COMPLETED -> "Completed"
        TrackingState.ABANDONED -> "Abandoned"
    }

private fun Double.formatCounter(): String =
    if (this % 1.0 == 0.0) toLong().toString() else toString()
