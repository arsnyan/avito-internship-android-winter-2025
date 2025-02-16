package com.arsnyan.musicapp.api

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arsnyan.tracklist.network.model.Track
import com.arsnyan.tracklist.network.repository.DeezerTracks
import com.arsnyan.tracklist.network.repository.TrackDataSource
import com.arsnyan.tracklist.network.repository.TrackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ApiTracksViewModel @Inject constructor(
    @DeezerTracks private val dataSource: TrackDataSource,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val repository = TrackRepository(dataSource)
    private val _uiState = MutableStateFlow<TracksUiState>(TracksUiState.Loading)
    val uiState: StateFlow<TracksUiState> = _uiState.asStateFlow()

    private var currentQuery = savedStateHandle.get<String>("current_query") ?: ""
        set(value) {
            field = value
            savedStateHandle["current_query"] = value
            if (value.isEmpty()) {
                loadInitialTracks()
            } else {
                performSearch(value)
            }
        }

    init {
        Log.d("ApiTracksViewModel", "Initializing with query: $currentQuery")
        if (currentQuery.isNotEmpty()) {
            performSearch(currentQuery)
        } else {
            loadInitialTracks()
        }
    }

    fun getCurrentQuery(): String = currentQuery

    fun loadInitialTracks() {
        _uiState.value = TracksUiState.Loading
        viewModelScope.launch {
            repository.getTracks()
                .onSuccess { trackList ->
                    _uiState.value = TracksUiState.Success(trackList)
                }
                .onFailure { error ->
                    _uiState.value = TracksUiState.Error(error.message ?: "Unknown error")
                }
        }
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            _uiState.value = TracksUiState.Loading
            repository.searchTracks(query)
                .onSuccess { resultList ->
                    if (resultList.isNullOrEmpty()) {
                        _uiState.value = TracksUiState.Empty
                    } else {
                        _uiState.value = TracksUiState.Success(resultList)
                    }
                }
                .onFailure { error ->
                    _uiState.value = TracksUiState.Error(error.message ?: "Unknown error")
                }
        }
    }

    fun searchTracks(query: String) {
        Log.d("ApiTracksViewModel", "Searching tracks with query: $query")
        currentQuery = query
    }

    sealed class TracksUiState {
        object Loading : TracksUiState()
        data class Success(val tracks: List<Track>) : TracksUiState()
        data class Error(val message: String) : TracksUiState()
        object Empty : TracksUiState()
    }
}