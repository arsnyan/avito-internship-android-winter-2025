package com.arsnyan.musicapp.api

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.arsnyan.musicapp.SharedViewModel
import com.arsnyan.tracklist.databinding.FragmentTracksBinding
import com.arsnyan.tracklist.network.model.TrackSource
import com.arsnyan.tracklist.ui.TrackListAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ApiTracksFragment : Fragment() {
    private var _binding: FragmentTracksBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ApiTracksViewModel by activityViewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private lateinit var adapter: TrackListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTracksBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            adapter = TrackListAdapter { track ->
                Log.e("ApiTracksFragment", "Track clicked: $track")
                sharedViewModel.setCurrentTrack(track.id, TrackSource.DEEZER)
            }
            trackList.layoutManager = LinearLayoutManager(requireContext())
            trackList.adapter = adapter

            searchBar.editText?.setText(viewModel.getCurrentQuery())

            var searchJob: Job? = null
            searchBar.editText?.doOnTextChanged { inputText, _, _, _ ->
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(300)
                    viewModel.searchTracks(inputText?.toString() ?: "")
                }
            }

            observeViewModel()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    handleTracksUiState(uiState)
                }
            }
        }
    }

    private fun handleTracksUiState(uiState: ApiTracksViewModel.TracksUiState) {
        when (uiState) {
            is ApiTracksViewModel.TracksUiState.Loading -> {
                binding.progressBar.isVisible = true
                binding.trackList.isVisible = false
                binding.statusView.isVisible = false
            }
            is ApiTracksViewModel.TracksUiState.Success -> {
                binding.progressBar.isVisible = false
                binding.trackList.isVisible = true
                binding.statusView.isVisible = false
                adapter.submitList(uiState.tracks)
            }
            is ApiTracksViewModel.TracksUiState.Error -> {
                binding.progressBar.isVisible = false
                binding.statusView.isVisible = false
                binding.trackList.isVisible = false
                Toast.makeText(requireContext(), uiState.message, Toast.LENGTH_SHORT).show()
            }
            is ApiTracksViewModel.TracksUiState.Empty -> {
                binding.progressBar.isVisible = false
                binding.statusView.isVisible = true
                binding.trackList.isVisible = false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}