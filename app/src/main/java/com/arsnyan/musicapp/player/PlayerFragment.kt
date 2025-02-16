package com.arsnyan.musicapp.player

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import coil.load
import com.arsnyan.musicapp.MainActivity
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

        binding.collapseBtn.setOnClickListener {
            requireActivity()
                .supportFragmentManager
                .findFragmentById(com.arsnyan.musicapp.R.id.nav_host_fragment_activity_main)
                ?.findNavController()
                ?.navigateUp()
        }

        binding.playPauseBtn.setOnClickListener {
            val mainActivity = requireActivity() as MainActivity
            val playbackService = mainActivity.getPlaybackService()

            when (playbackService?.playbackState?.value) {
                PlaybackService.PlaybackState.Playing -> {
                    playbackService.pause()
                }

                PlaybackService.PlaybackState.Paused,
                PlaybackService.PlaybackState.Completed,
                PlaybackService.PlaybackState.Idle -> {
                    playbackService.resume()
                }

                is PlaybackService.PlaybackState.Error -> {
                    playbackService.resume()
                }

                else -> {}
            }
        }

        binding.skipNextBtn.setOnClickListener {
            viewModel.moveToNextTrack()
        }

        binding.skipPreviousBtn.setOnClickListener {
            viewModel.moveToPreviousTrack()
        }

        binding.playbackSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val mainActivity = requireActivity() as MainActivity
                val playbackService = mainActivity.getPlaybackService()
                playbackService?.seekTo(value.toLong())
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    handleUiTrackState(uiState)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val mainActivity = requireActivity() as MainActivity
                val playbackService = mainActivity.getPlaybackService()
                playbackService?.playbackState?.collect { state ->
                    when (state) {
                        PlaybackService.PlaybackState.Paused -> {
                            binding.playPauseBtn.icon = AppCompatResources
                                .getDrawable(
                                    requireContext(),
                                    com.arsnyan.musicapp.R.drawable.play_arrow_24px
                                )
                        }

                        PlaybackService.PlaybackState.Playing -> {
                            binding.playPauseBtn.icon = AppCompatResources
                                .getDrawable(
                                    requireContext(),
                                    com.arsnyan.musicapp.R.drawable.pause_24px
                                )
                        }

                        else -> {}
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val mainActivity = requireActivity() as MainActivity
                val playbackService = mainActivity.getPlaybackService()
                playbackService?.mediaDuration?.collect { duration ->
                    Log.d("PlayerFragment", "$duration")
                    if (duration > 0) {
                        binding.trackDuration.text = secondsToMMSS((duration / 1000).toInt())
                    }
                    binding.playbackSlider.valueTo = duration.toFloat()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val mainActivity = requireActivity() as MainActivity
                val playbackService = mainActivity.getPlaybackService()
                playbackService?.currentPosition?.collect { position ->
                    binding.passedDuration.text = secondsToMMSS((position / 1000).toInt())
                    binding.playbackSlider.value = ((position / 1000).toInt() * 1000).toFloat()
                    binding.playbackSlider.setLabelFormatter { value ->
                        secondsToMMSS((value / 1000).toInt())
                    }
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
                binding.apply {
                    albumTitle.text = uiState.track.album.title
                    trackTitle.text = uiState.track.title
                    trackArtist.text = uiState.track.artist.name
                    passedDuration.text = secondsToMMSS(0)
                    // trackDuration.text = secondsToMMSS(uiState.track.duration)
                    trackCover.load(uiState.track.album.coverXlUrl) {
                        placeholder(R.drawable.cover_placeholder)
                        error(R.drawable.cover_placeholder)
                        crossfade(true)
                        crossfade(300)
                    }
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