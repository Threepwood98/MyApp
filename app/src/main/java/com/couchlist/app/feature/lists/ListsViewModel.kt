package com.couchlist.app.feature.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.couchlist.app.core.domain.model.LibraryMedia
import com.couchlist.app.core.domain.model.ListGroup
import com.couchlist.app.core.domain.model.MediaList
import com.couchlist.app.core.domain.model.MediaListSummary
import com.couchlist.app.core.domain.model.MediaListType
import com.couchlist.app.core.domain.repository.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class ListsViewModel @Inject constructor(
    private val repository: LibraryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ListsUiState())
    val uiState: StateFlow<ListsUiState> = _uiState.asStateFlow()

    private val _events = Channel<ListsEvent>(Channel.BUFFERED)
    val events: Flow<ListsEvent> = _events.receiveAsFlow()

    init {
        val lists = repository.observeLists()
        viewModelScope.launch {
            combine(lists, repository.observeGroups(), ::buildListsUiState)
                .collect { state -> _uiState.update { state.copy(pileItems = it.pileItems, isLoading = false) } }
        }
        viewModelScope.launch {
            lists
                .map { summaries -> summaries.firstOrNull { it.list.type == MediaListType.PILE }?.list?.id }
                .distinctUntilChanged()
                .flatMapLatest { pileId ->
                    if (pileId == null) flowOf(emptyList()) else repository.observeListItems(pileId)
                }
                .collect { items -> _uiState.update { it.copy(pileItems = items) } }
        }
    }

    fun createList(name: String, description: String?, type: MediaListType, groupId: Long?) {
        viewModelScope.launch {
            repository.createList(name.trim(), description, type, groupId = groupId)
            _events.send(ListsEvent.ShowMessage("Created list \"${name.trim()}\""))
        }
    }

    fun updateList(
        list: MediaList,
        name: String,
        description: String?,
        type: MediaListType,
        groupId: Long?,
    ) {
        viewModelScope.launch {
            repository.updateList(list.id, name, description, type, groupId)
            _events.send(ListsEvent.ShowMessage("List updated"))
        }
    }

    fun deleteList(list: MediaList) {
        if (list.type == MediaListType.PILE) return
        viewModelScope.launch {
            repository.deleteList(list.id)
            _events.send(ListsEvent.ShowMessage("Deleted ${list.name}"))
        }
    }

    fun duplicateList(list: MediaList) {
        viewModelScope.launch {
            repository.duplicateList(list.id)
            _events.send(ListsEvent.ShowMessage("Duplicated ${list.name}"))
        }
    }

    fun togglePinned(list: MediaList) {
        viewModelScope.launch { repository.setListPinned(list.id, !list.isPinned) }
    }

    fun setListGroup(listId: Long, groupId: Long?) {
        viewModelScope.launch { repository.setListGroup(listId, groupId) }
    }

    fun moveList(list: MediaList, offset: Int) {
        val siblings = uiState.value.allLists
            .filter { summary ->
                summary.list.type != MediaListType.PILE &&
                    summary.list.isPinned == list.isPinned &&
                    (list.isPinned || summary.list.groupId == list.groupId)
            }
            .sortedBy { it.list.sortOrder }
        val current = siblings.indexOfFirst { it.list.id == list.id }
        val target = current + offset
        if (current < 0 || target !in siblings.indices) return
        val reordered = siblings.toMutableList().apply {
            add(target, removeAt(current))
        }
        viewModelScope.launch { repository.reorderLists(reordered.map { it.list.id }) }
    }

    fun createGroup(name: String) {
        viewModelScope.launch {
            repository.createGroup(name)
            _events.send(ListsEvent.ShowMessage("Created group \"${name.trim()}\""))
        }
    }

    fun renameGroup(group: ListGroup, name: String) {
        viewModelScope.launch { repository.renameGroup(group.id, name) }
    }

    fun deleteGroup(group: ListGroup) {
        viewModelScope.launch {
            repository.deleteGroup(group.id)
            _events.send(ListsEvent.ShowMessage("Deleted ${group.name}"))
        }
    }

    fun moveGroup(group: ListGroup, offset: Int) {
        val groups = uiState.value.groups.map { it.group }.sortedBy { it.sortOrder }
        val current = groups.indexOfFirst { it.id == group.id }
        val target = current + offset
        if (current < 0 || target !in groups.indices) return
        val reordered = groups.toMutableList().apply { add(target, removeAt(current)) }
        viewModelScope.launch { repository.reorderGroups(reordered.map { it.id }) }
    }

}

internal fun buildListsUiState(
    summaries: List<MediaListSummary>,
    groups: List<ListGroup>,
): ListsUiState {
    val pile = summaries.firstOrNull { it.list.type == MediaListType.PILE }
    val regular = summaries.filterNot { it.list.type == MediaListType.PILE }
    val pinned = regular.filter { it.list.isPinned }.sortedBy { it.list.sortOrder }
    val groupedIds = groups.mapTo(mutableSetOf()) { it.id }
    val sections = groups.sortedBy { it.sortOrder }.map { group ->
        ListGroupSection(
            group = group,
            lists = regular.filter { !it.list.isPinned && it.list.groupId == group.id }
                .sortedBy { it.list.sortOrder },
        )
    }
    val ungrouped = regular.filter {
        !it.list.isPinned && (it.list.groupId == null || it.list.groupId !in groupedIds)
    }.sortedBy { it.list.sortOrder }
    return ListsUiState(
        pile = pile,
        pinnedLists = pinned,
        groups = sections,
        ungroupedLists = ungrouped,
        allLists = summaries,
    )
}

data class ListsUiState(
    val pile: MediaListSummary? = null,
    val pileItems: List<LibraryMedia> = emptyList(),
    val pinnedLists: List<MediaListSummary> = emptyList(),
    val groups: List<ListGroupSection> = emptyList(),
    val ungroupedLists: List<MediaListSummary> = emptyList(),
    val allLists: List<MediaListSummary> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

data class ListGroupSection(
    val group: ListGroup,
    val lists: List<MediaListSummary>,
)

sealed interface ListsEvent {
    data class ShowMessage(val message: String) : ListsEvent
}
