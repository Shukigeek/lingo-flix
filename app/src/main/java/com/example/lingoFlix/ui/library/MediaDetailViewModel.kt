package com.example.lingoFlix.ui.library

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lingoFlix.R
import com.example.lingoFlix.domain.model.SubtitleTrack
import com.example.lingoFlix.domain.repository.LibraryRepository
import com.example.lingoFlix.domain.repository.SubtitleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs the media detail screen.
 *
 * Reads the media item and its subtitle tracks from two independent flows so
 * attaching, re-syncing or deleting a track refreshes the list without
 * re-loading the item, and without any manual refresh call from the UI.
 */
@HiltViewModel
class MediaDetailViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val libraryRepository: LibraryRepository,
    private val subtitleRepository: SubtitleRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /** Matches the property name of `Route.MediaDetail.mediaId`. */
    private val mediaId: Long = savedStateHandle.get<Long>(ARG_MEDIA_ID) ?: INVALID_ID

    private val attaching = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<MediaDetailUiState> = combine(
        libraryRepository.observeItem(mediaId),
        subtitleRepository.observeTracks(mediaId),
        attaching,
        message,
    ) { item, tracks, isAttaching, message ->
        MediaDetailUiState(
            item = item,
            // Primary first, then longest: the track the user will actually
            // learn from should never be below a stray two-line sample file.
            tracks = tracks.sortedWith(
                compareByDescending<SubtitleTrack> { it.isPrimary }
                    .thenByDescending { it.sentenceCount },
            ),
            isLoading = false,
            isAttaching = isAttaching,
            message = message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = MediaDetailUiState(),
    )

    /**
     * Parses and stores a picked subtitle file.
     *
     * Made primary only when it is the first track, so attaching a second
     * language does not silently hijack what the player shows.
     */
    fun attachSubtitle(uri: String) {
        viewModelScope.launch {
            attaching.value = true
            val makePrimary = uiState.value.tracks.isEmpty()
            val result = subtitleRepository.attachSubtitle(
                mediaItemId = mediaId,
                uri = uri,
                makePrimary = makePrimary,
            )
            attaching.value = false
            message.value = context.getString(
                if (result.isSuccess) {
                    R.string.detail_subtitle_attached
                } else {
                    R.string.detail_subtitle_failed
                },
            )
        }
    }

    fun makePrimary(trackId: Long) {
        viewModelScope.launch { subtitleRepository.setPrimaryTrack(mediaId, trackId) }
    }

    fun setOffset(trackId: Long, offsetMs: Long) {
        viewModelScope.launch { subtitleRepository.setOffset(trackId, offsetMs) }
    }

    fun deleteTrack(trackId: Long) {
        viewModelScope.launch {
            subtitleRepository.deleteTrack(trackId)
            message.value = context.getString(R.string.detail_subtitle_deleted)
        }
    }

    fun rename(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch { libraryRepository.renameItem(mediaId, title) }
    }

    fun delete() {
        viewModelScope.launch { libraryRepository.deleteItem(mediaId) }
    }

    fun consumeMessage() {
        message.value = null
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val ARG_MEDIA_ID = "mediaId"

        /** No row can ever have this id, so the screen falls into its missing state. */
        const val INVALID_ID = -1L
    }
}
