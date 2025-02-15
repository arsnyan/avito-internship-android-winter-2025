package com.arsnyan.musicapp.local

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.arsnyan.tracklist.databinding.FragmentTracksBinding
import com.arsnyan.tracklist.ui.TrackListAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.getValue

@AndroidEntryPoint
class LocalTracksFragment : Fragment() {
    private var _binding: FragmentTracksBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LocalTracksViewModel by activityViewModels()
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkPermission(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        with(binding) {
            adapter = TrackListAdapter { track ->

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

    private fun handleTracksUiState(uiState: LocalTracksViewModel.TracksUiState) {
        when (uiState) {
            is LocalTracksViewModel.TracksUiState.Loading -> {
                binding.progressBar.isVisible = true
                binding.trackList.isVisible = false
                binding.statusView.isVisible = false
            }
            is LocalTracksViewModel.TracksUiState.Success -> {
                binding.progressBar.isVisible = false
                binding.trackList.isVisible = true
                binding.statusView.isVisible = false
                adapter.submitList(uiState.tracks)
            }
            is LocalTracksViewModel.TracksUiState.Error -> {
                binding.progressBar.isVisible = false
                binding.statusView.isVisible = false
                binding.trackList.isVisible = false
                Toast.makeText(requireContext(), uiState.message, Toast.LENGTH_SHORT).show()
            }
            is LocalTracksViewModel.TracksUiState.Empty -> {
                binding.progressBar.isVisible = false
                binding.statusView.isVisible = true
                binding.trackList.isVisible = false
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (!isGranted) {
                Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()

                if (!shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", requireContext().packageName, null)
                    }
                    startActivity(intent)
                    Toast.makeText(
                        requireContext(),
                        "Please enable storage permission in settings",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    private fun checkPermission(permission: String) {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(permission)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}