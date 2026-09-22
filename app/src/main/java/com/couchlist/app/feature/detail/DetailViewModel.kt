package com.couchlist.app.feature.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.couchlist.app.core.common.networkErrorMessage
import com.couchlist.app.core.domain.model.MediaDetail
import com.couchlist.app.core.domain.model.MediaItem
import com.couchlist.app.core.domain.model.MediaListSummary
import com.couchlist.app.core.domain.model.TrackingMode
import com.couchlist.app.core.domain.model.TrackingSession
import com.couchlist.app.core.domain.model.TrackingState
import com.couchlist.app.core.domain.model.defaultTrackingMode
import com.couchlist.app.core.domain.model.defaultTrackingUnit
import com.couchlist.app.core.domain.model.TvEpisode
import com.couchlist.app.core.domain.model.TvProgress
import com.couchlist.app.core.domain.model.TvSeason
import com.couchlist.app.core.domain.model.WatchProvider
import com.couchlist.app.core.domain.repository.CatalogRepository
import com.couchlist.app.core.domain.repository.LibraryRepository
import com.couchlist.app.core.domain.repository.LogbookRepository
import com.couchlist.app.core.domain.repository.TvRepository
import com.couchlist.app.core.domain.repository.TrackingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val catalogRepository: CatalogRepository,
    private val libraryRepository: LibraryRepository,
    private val logbookRepository: LogbookRepository,
    private val tvRepository: TvRepository,
    private val trackingRepository: TrackingRepository,
) : ViewModel() {

    private val mediaId: Long = savedStateHandle.get<Long>("mediaId") ?: -1L

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private val _events = Channel<DetailEvent>(Channel.BUFFERED)
    val events: Flow<DetailEvent> = _events.receiveAsFlow()

    private val providers = MutableStateFlow<List<WatchProvider>>(emptyList())
    private val selectedSeasonNumber = MutableStateFlow<Int?>(null)
    private var seasonRefreshJob: Job? = null

    init {
        if (mediaId < 0L) {
            _uiState.update {
                it.copy(isLoading = false, errorMessage = "Couldn't load this title.")
            }
        } else {
            observeDetail()
            observeListMemberships()
            observeSeasons()
            observeEpisodes()
            observeProgress()
            observeNextUnwatched()
            observeTracking()
            refresh()
        }
    }

    fun onRetry() {
        refresh()
        selectedSeasonNumber.value?.let(::refreshSeason)
    }

    fun onSeasonSelected(seasonNumber: Int) {
        if (selectedSeasonNumber.value == seasonNumber) return
        selectedSeasonNumber.value = seasonNumber
        _uiState.update {
            it.copy(
                selectedSeasonNumber = seasonNumber,
                episodes = emptyList(),
                isSeasonLoading = true,
                seasonErrorMessage = null,
            )
        }
        refreshSeason(seasonNumber)
    }

    fun onRetrySeason() {
        selectedSeasonNumber.value?.let(::refreshSeason)
    }

    fun onEpisodeWatchedChange(episode: TvEpisode, watched: Boolean) {
        if (_uiState.value.entryId == null) {
            viewModelScope.launch {
                _events.send(DetailEvent.ShowMessage("Add this show to your Watchlist to track episodes"))
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(updatingEpisodeIds = it.updatingEpisodeIds + episode.id) }
            tvRepository.setEpisodeWatched(episode.id, watched)
                .onFailure { throwable ->
                    _events.send(DetailEvent.ShowMessage(networkErrorMessage(throwable)))
                }
            _uiState.update { it.copy(updatingEpisodeIds = it.updatingEpisodeIds - episode.id) }
        }
    }

    fun onAddToWatchlist() {
        viewModelScope.launch {
            val state = _uiState.value
            val detail = state.detail ?: return@launch
            if (state.entryId != null) {
                _events.send(DetailEvent.ShowMessage("${detail.title} is already in your Watchlist"))
                return@launch
            }
            libraryRepository.addToWatchlist(mediaId)
            _events.send(DetailEvent.ShowMessage("Added ${detail.title} to your Watchlist"))
        }
    }

    fun onAddToPile() {
        viewModelScope.launch {
            val detail = _uiState.value.detail ?: return@launch
            libraryRepository.addToPile(mediaId)
            _events.send(DetailEvent.ShowMessage("Added ${detail.title} to The Pile"))
        }
    }

    fun onListMembershipChange(listId: Long, isMember: Boolean) {
        viewModelScope.launch {
            if (isMember) {
                _uiState.value.entryId?.let { libraryRepository.removeFromList(listId, it) }
            } else {
                libraryRepository.addToList(listId, mediaId)
            }
        }
    }

    fun onStartTracking(mode: TrackingMode, total: Double? = null, unit: String? = null) {
        viewModelScope.launch {
            val state = _uiState.value
            state.detail ?: return@launch
            trackingRepository.start(mediaId, mode, total, unit)
            _events.send(DetailEvent.ShowMessage("Started ${mode.displayName.lowercase()}"))
        }
    }

    fun onStartDefaultTracking() {
        val detail = _uiState.value.detail ?: return
        onStartTracking(
            mode = detail.category.defaultTrackingMode,
            unit = detail.category.defaultTrackingUnit,
        )
    }

    fun onPauseTracking() {
        mutateTracking("Tracking paused") { trackingRepository.pause(it) }
    }

    fun onResumeTracking() {
        mutateTracking("Tracking resumed") { trackingRepository.resume(it) }
    }

    fun onCompleteTracking() {
        mutateTracking("Marked complete") { trackingRepository.complete(it) }
    }

    fun onAbandonTracking() {
        mutateTracking("Marked abandoned") { trackingRepository.abandon(it) }
    }

    fun onUpdateCounter(current: Double, total: Double?, unit: String?) {
        mutateTracking("Progress updated") {
            trackingRepository.updateCounter(it, current, total, unit)
        }
    }

    fun onAddCheckpoint(label: String) {
        mutateTracking("Checkpoint added") {
            trackingRepository.addCheckpoint(it, label)
        }
    }

    fun onCheckpointCompleted(checkpointId: Long, completed: Boolean) {
        viewModelScope.launch {
            trackingRepository.setCheckpointCompleted(checkpointId, completed)
        }
    }

    fun onAddQuickLog(note: String?) {
        mutateTracking("Session logged") { trackingRepository.addQuickLog(it, note) }
    }

    fun onAddJournalEntry(title: String, notes: String?) {
        mutateTracking("Journal entry added") {
            trackingRepository.addJournalEntry(it, title, notes)
        }
    }

    fun onRemoveFromWatchlist() {
        viewModelScope.launch {
            val entryId = _uiState.value.entryId
            val title = _uiState.value.detail?.title ?: "title"
            if (entryId != null) {
                libraryRepository.removeItem(entryId)
            }
            _events.send(DetailEvent.ShowMessage("Removed $title from your lists"))
        }
    }

    fun onMarkWatched(rating: Int?, notes: String?) {
        viewModelScope.launch {
            val detail = _uiState.value.detail ?: return@launch
            logbookRepository.logWatched(
                mediaId = mediaId,
                rating = rating,
                notes = notes,
            )
            val state = _uiState.value
            val tracking = state.tracking
            val libraryItemId = if (
                tracking == null ||
                tracking.state == TrackingState.COMPLETED ||
                tracking.state == TrackingState.ABANDONED
            ) {
                trackingRepository.start(mediaId, detail.category.defaultTrackingMode)
            } else {
                tracking.libraryItemId
            }
            trackingRepository.complete(libraryItemId)
            _events.send(DetailEvent.ShowMessage("Logged ${detail.title} as watched"))
        }
    }

    fun onToggleFavorite() {
        viewModelScope.launch {
            val newFavorite = !_uiState.value.favorite
            libraryRepository.updateFavorite(mediaId, newFavorite)
            _events.send(
                DetailEvent.ShowMessage(
                    if (newFavorite) "Added to favorites" else "Removed from favorites",
                ),
            )
        }
    }

    fun onSetRating(rating: Int?) {
        viewModelScope.launch {
            libraryRepository.updateRating(mediaId, rating)
            _events.send(
                DetailEvent.ShowMessage(
                    if (rating != null) "Rating set to $rating / 10" else "Rating removed",
                ),
            )
        }
    }

    fun onSetNotes(notes: String?) {
        viewModelScope.launch {
            libraryRepository.updateNotes(mediaId, notes)
            _events.send(DetailEvent.ShowMessage("Notes updated"))
        }
    }

    fun onRewatch() {
        viewModelScope.launch {
            val detail = _uiState.value.detail ?: return@launch
            logbookRepository.logWatched(mediaId = mediaId)
            _events.send(DetailEvent.ShowMessage("Logged rewatch for ${detail.title}"))
        }
    }

    private fun observeDetail() {
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                catalogRepository.observeMedia(mediaId),
                libraryRepository.observeEntry(mediaId),
                providers,
            ) { media, entry, providers -> Triple(media, entry, providers) }
                .collect { (media, entry, currentProviders) ->
                    _uiState.update { state ->
                        state.copy(
                            detail = media?.toDetail(currentProviders),
                            entryId = entry?.id,
                            favorite = entry?.favorite ?: false,
                            personalRating = entry?.personalRating,
                            notes = entry?.notes,
                            isLoading = media == null && state.isLoading,
                            errorMessage = if (media != null) null else state.errorMessage,
                        )
                    }
                }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeListMemberships() {
        viewModelScope.launch {
            libraryRepository.observeLists().collect { lists ->
                _uiState.update { it.copy(lists = lists) }
            }
        }
        viewModelScope.launch {
            libraryRepository.observeEntry(mediaId)
                .flatMapLatest { entry ->
                    if (entry == null) flowOf(emptySet())
                    else libraryRepository.observeMembershipListIds(entry.id)
                }
                .collect { memberships ->
                    _uiState.update { it.copy(listMembershipIds = memberships) }
                }
        }
    }

    private fun observeSeasons() {
        viewModelScope.launch {
            tvRepository.observeSeasons(mediaId).collect { seasons ->
                val currentSelection = selectedSeasonNumber.value
                val selection = currentSelection.takeIf { selected ->
                    seasons.any { it.seasonNumber == selected }
                } ?: seasons.defaultSeasonNumber()
                _uiState.update {
                    it.copy(
                        seasons = seasons,
                        selectedSeasonNumber = selection,
                    )
                }
                if (selection != null && selection != currentSelection) {
                    selectedSeasonNumber.value = selection
                    refreshSeason(selection)
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeEpisodes() {
        viewModelScope.launch {
            selectedSeasonNumber
                .filterNotNull()
                .flatMapLatest { seasonNumber ->
                    tvRepository.observeEpisodes(mediaId, seasonNumber)
                }
                .collect { episodes ->
                    _uiState.update { it.copy(episodes = episodes) }
                }
        }
    }

    private fun observeProgress() {
        viewModelScope.launch {
            tvRepository.observeProgress(mediaId).collect { progress ->
                _uiState.update { it.copy(tvProgress = progress) }
            }
        }
    }

    private fun observeNextUnwatched() {
        viewModelScope.launch {
            tvRepository.observeNextUnwatched(mediaId).collect { episode ->
                _uiState.update { it.copy(nextUnwatchedEpisode = episode) }
            }
        }
    }

    private fun observeTracking() {
        viewModelScope.launch {
            trackingRepository.observeCurrentByMediaId(mediaId).collect { tracking ->
                _uiState.update { it.copy(tracking = tracking) }
            }
        }
    }

    private fun mutateTracking(message: String, mutation: suspend (Long) -> Unit) {
        val state = _uiState.value
        val libraryItemId = state.tracking?.libraryItemId ?: state.entryId ?: return
        viewModelScope.launch {
            mutation(libraryItemId)
            _events.send(DetailEvent.ShowMessage(message))
        }
    }

    private fun refresh() {
        if (mediaId < 0L) return
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isLoading = state.detail == null,
                    errorMessage = null,
                    isOffline = false,
                )
            }
            val result = try {
                catalogRepository.refresh(mediaId)
            } catch (e: CancellationException) {
                throw e
            }
            result.onSuccess { refreshedProviders ->
                providers.value = refreshedProviders
                _uiState.update { it.copy(isLoading = false, isOffline = false) }
            }.onFailure { throwable ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        isOffline = state.detail != null,
                        errorMessage = if (state.detail == null) {
                            networkErrorMessage(throwable)
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }

    private fun refreshSeason(seasonNumber: Int) {
        seasonRefreshJob?.cancel()
        seasonRefreshJob = viewModelScope.launch {
            _uiState.update {
                it.copy(isSeasonLoading = true, seasonErrorMessage = null)
            }
            tvRepository.refreshSeason(mediaId, seasonNumber)
                .onSuccess {
                    _uiState.update { it.copy(isSeasonLoading = false) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isSeasonLoading = false,
                            seasonErrorMessage = networkErrorMessage(throwable),
                        )
                    }
                }
        }
    }
}

data class DetailUiState(
    val detail: MediaDetail? = null,
    val entryId: Long? = null,
    val tracking: TrackingSession? = null,
    val favorite: Boolean = false,
    val personalRating: Int? = null,
    val notes: String? = null,
    val lists: List<MediaListSummary> = emptyList(),
    val listMembershipIds: Set<Long> = emptySet(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isOffline: Boolean = false,
    val seasons: List<TvSeason> = emptyList(),
    val selectedSeasonNumber: Int? = null,
    val episodes: List<TvEpisode> = emptyList(),
    val tvProgress: TvProgress = TvProgress(0, 0),
    val nextUnwatchedEpisode: TvEpisode? = null,
    val isSeasonLoading: Boolean = false,
    val seasonErrorMessage: String? = null,
    val updatingEpisodeIds: Set<Long> = emptySet(),
)

internal fun List<TvSeason>.defaultSeasonNumber(): Int? =
    firstOrNull { it.seasonNumber == 1 }?.seasonNumber
        ?: firstOrNull { it.seasonNumber > 0 }?.seasonNumber
        ?: firstOrNull()?.seasonNumber

private fun MediaItem.toDetail(providers: List<WatchProvider>) = MediaDetail(
    reference = reference,
    title = title,
    originalTitle = originalTitle,
    description = description,
    releaseDate = releaseDate,
    originalLanguage = originalLanguage,
    artworkUri = artworkUri,
    backdropUri = backdropUri,
    voteAverage = externalRating,
    voteCount = externalVoteCount,
    genres = genres,
    providers = providers,
    metadata = metadata,
)

sealed interface DetailEvent {
    data class ShowMessage(val message: String) : DetailEvent
}
