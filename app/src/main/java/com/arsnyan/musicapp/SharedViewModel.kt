package com.arsnyan.musicapp

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arsnyan.tracklist.network.model.Track
import com.arsnyan.tracklist.network.model.TrackSource
import com.arsnyan.tracklist.network.repository.DeezerTracks
import com.arsnyan.tracklist.network.repository.LocalTracks
import com.arsnyan.tracklist.network.repository.TrackDataSource
import com.arsnyan.tracklist.network.repository.TrackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SharedViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    @DeezerTracks private val apiDataSource: TrackDataSource,
    @LocalTracks private val localDataSource: TrackDataSource
) : ViewModel() {
    private val apiRepository = TrackRepository(apiDataSource)
    private val localRepository = TrackRepository(localDataSource)
    private val _uiState = MutableStateFlow<TrackUiState>(TrackUiState.Loading)
    val uiState: StateFlow<TrackUiState> = _uiState.asStateFlow()

    private var currentTrackId = savedStateHandle.get<Long>("current_track_id") ?: -1
    private var currentTrackSource = savedStateHandle.get<TrackSource>("current_track_source") ?: TrackSource.NONE

    private val _trackQueue = MutableStateFlow<List<Track>>(emptyList())
    val trackQueue: StateFlow<List<Track>> = _trackQueue.asStateFlow()
    private var currentQueueIndex = -1

    fun setQueue(queue: List<Track>) {
        queue.forEach {
            Log.d("SharedViewModel", "Setting new queue. $it")
        }
        _trackQueue.value = queue
        currentQueueIndex = -1
    }

    fun addToQueue(track: Track) {
        _trackQueue.value += track
    }

    fun moveToNextTrack() {
        if (_trackQueue.value.isNotEmpty() && currentQueueIndex < _trackQueue.value.size - 1) {
            currentQueueIndex++
            val nextTrack = _trackQueue.value[currentQueueIndex]
            setCurrentTrack(nextTrack.id, nextTrack.trackSource)
        }
    }

    fun moveToPreviousTrack() {
        if (_trackQueue.value.isNotEmpty() && currentQueueIndex > 0) {
            currentQueueIndex--
            val previousTrack = _trackQueue.value[currentQueueIndex]
            setCurrentTrack(previousTrack.id, previousTrack.trackSource)
        }
    }

    fun getCurrentTrack(): Pair<Long?, TrackSource?> {
        return Pair(currentTrackId, currentTrackSource)
    }

    fun setCurrentTrack(id: Long, source: TrackSource) {
        currentTrackId = id
        savedStateHandle["current_track_id"] = id
        currentTrackSource = source
        savedStateHandle["current_track_source"] = source
        val newIndex = _trackQueue.value.indexOfFirst { it.id == id && it.trackSource == source }
        if (newIndex != -1) {
            currentQueueIndex = newIndex
        }
        loadTrack()
    }

    fun clearCurrentTrack() {
        setCurrentTrack(-1, TrackSource.NONE)
        _uiState.value = TrackUiState.Empty
    }

    fun loadTrack() {
        if (currentTrackId == -1L) {
            _uiState.value = TrackUiState.Empty
            return
        }
        _uiState.value = TrackUiState.Loading
        viewModelScope.launch {
            when (currentTrackSource) {
                TrackSource.DEEZER -> {
                    apiRepository.getTrackById(currentTrackId)
                        .onSuccess { track ->
                            _uiState.value = TrackUiState.Success(track)
                        }
                        .onFailure { error ->
                            _uiState.value = TrackUiState.Error(error.message ?: "Unknown error")
                        }
                }
                TrackSource.LOCAL -> {
                    localRepository.getTrackById(currentTrackId)
                        .onSuccess { track ->
                            _uiState.value = TrackUiState.Success(track)
                        }
                        .onFailure { error ->
                            _uiState.value = TrackUiState.Error(error.message ?: "Unknown error")
                        }
                }
                else -> {
                    _uiState.value = TrackUiState.Empty
                }
            }
        }
    }

    sealed class TrackUiState {
        object Loading : TrackUiState()
        data class Success(val track: Track) : TrackUiState()
        data class Error(val message: String) : TrackUiState()
        object Empty : TrackUiState()
    }
}