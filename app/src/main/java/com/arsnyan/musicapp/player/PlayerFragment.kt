package com.arsnyan.musicapp.player

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.arsnyan.musicapp.SharedViewModel
import com.arsnyan.musicapp.SharedViewModel.TrackUiState
import com.arsnyan.musicapp.databinding.ExpandedPlayerBinding
import com.arsnyan.tracklist.R
import kotlinx.coroutines.launch

class PlayerFragment : Fragment() {
    private var _binding: ExpandedPlayerBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SharedViewModel by activityViewModels<SharedViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = ExpandedPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    handleUiTrackState(uiState)
                }
            }
        }
    }

    private fun handleUiTrackState(uiState: TrackUiState) {
        when (uiState) {
            is TrackUiState.Loading -> {
                Log.d("PlayerFragment", "Loading track")
            }
            is TrackUiState.Success -> {
                binding.trackTitle.text = uiState.track.title
                binding.trackArtist.text = uiState.track.artist.name
                binding.trackDuration.text = secondsToMMSS(uiState.track.duration)
                binding.trackCover.load(uiState.track.album.coverUrl) {
                    placeholder(R.drawable.cover_placeholder)
                    error(R.drawable.cover_placeholder)
                    crossfade(true)
                }
            }
            is TrackUiState.Error -> {
                Log.e("PlayerFragment", "Error: ${uiState.message}")
                Toast.makeText(requireContext(), "Something went wrong", Toast.LENGTH_SHORT).show()
            }
            is TrackUiState.Empty -> {
                Toast.makeText(requireContext(), "No track is selected", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun secondsToMMSS(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return "%02d:%02d".format(minutes, remainingSeconds)
    }
}