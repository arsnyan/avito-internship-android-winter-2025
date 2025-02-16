package com.arsnyan.musicapp

import android.Manifest
import android.app.ComponentCaller
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import coil.load
import com.arsnyan.musicapp.api.ApiTracksViewModel
import com.arsnyan.musicapp.databinding.ActivityMainBinding
import com.arsnyan.musicapp.local.LocalTracksViewModel
import com.arsnyan.musicapp.player.PlaybackService
import com.arsnyan.tracklist.network.model.TrackSource
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var playbackService: PlaybackService? = null
    private var isServiceBound = false

    private val apiViewModel: ApiTracksViewModel by viewModels()
    private val localViewModel: LocalTracksViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by viewModels()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(
            name: ComponentName?,
            service: IBinder?
        ) {
            val binder = service as PlaybackService.LocalBinder
            playbackService = binder.getService()
            isServiceBound = true
            observePlaybackState()

            playbackService?.setOnTrackCompletionListener {
                sharedViewModel.moveToNextTrack()
            }
            playbackService?.getCurrentTrack()?.let { track ->
                if (
                    playbackService?.playbackState?.value == PlaybackService.PlaybackState.Playing ||
                    playbackService?.playbackState?.value == PlaybackService.PlaybackState.Paused
                ) {
                    Log.d("MainActivity", "Setting track onServiceConnected. Track: ${track.id}, ${track.trackSource}")
                    sharedViewModel.setCurrentTrack(track.id, track.trackSource)
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isServiceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        val navController = navHostFragment.navController
        navController.addOnDestinationChangedListener { _, destination, _ ->
            apiViewModel
            localViewModel
            sharedViewModel

            if (destination.id == R.id.navigation_player) {
                navView.visibility = View.GONE
                binding.collapsedPlayer.collapsedPlayer.visibility = View.GONE
            } else {
                navView.visibility = View.VISIBLE
                if (sharedViewModel.uiState.value is SharedViewModel.TrackUiState.Success) {
                    binding.collapsedPlayer.collapsedPlayer.visibility = View.VISIBLE
                }
            }
        }
        navView.setupWithNavController(navController)

        observeViewModel()

        // If activity is launched from a notification
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        Intent(this, PlaybackService::class.java).also { intent ->
            bindService(intent, serviceConnection, BIND_AUTO_CREATE)
            startService(intent)
        }
    }

    override fun onStop() {
        super.onStop()
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
    }

    private fun handleIntent(intent: Intent?) {
        intent?.let {
            val trackId = it.getLongExtra("track_id", -1)
            val trackSourceStr = it.getStringExtra("track_source")

            if (trackId != -1L && !trackSourceStr.isNullOrEmpty()) {
                try {
                    val trackSource = TrackSource.valueOf(trackSourceStr)
                    if (trackId != sharedViewModel.getCurrentTrack().first ||
                        trackSource != sharedViewModel.getCurrentTrack().second) {
                        sharedViewModel.setCurrentTrack(trackId, trackSource)
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Invalid track source: $trackSourceStr")
                }
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                sharedViewModel.uiState.collect { uiState ->
                    handleUiTrackState(uiState)
                }
            }
        }
    }

    private fun observePlaybackState() {
        if (!isServiceBound) return

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                playbackService?.playbackState?.collect { state ->
                    when (state) {
                        PlaybackService.PlaybackState.Completed -> {
                            sharedViewModel.clearCurrentTrack()
                        }
                        PlaybackService.PlaybackState.Paused -> {
                            binding.collapsedPlayer.playPauseBtn.icon =
                                AppCompatResources.getDrawable(baseContext, R.drawable.play_arrow_24px)
                        }
                        PlaybackService.PlaybackState.Playing -> {
                            binding.collapsedPlayer.playPauseBtn.icon =
                                AppCompatResources.getDrawable(baseContext, R.drawable.pause_24px)
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun handleUiTrackState(uiState: SharedViewModel.TrackUiState) {// Handle error state
        when (uiState) {
            is SharedViewModel.TrackUiState.Success -> {
                with (binding.collapsedPlayer) {
                    Log.d("MainActivity", "Track: ${uiState.track}")
                    // collapsedPlayer.visibility = View.VISIBLE

                    val navController = supportFragmentManager
                        .findFragmentById(R.id.nav_host_fragment_activity_main)
                        ?.findNavController()
                    val currentDestination = navController?.currentDestination
                    if (currentDestination?.id != R.id.navigation_player) {
                        collapsedPlayer.visibility = View.VISIBLE
                    } else {
                        collapsedPlayer.visibility = View.GONE
                    }

                    trackTitle.text = uiState.track.title
                    trackArtist.text = uiState.track.artist.name
                    trackCover.load(uiState.track.album.coverUrl) {
                        placeholder(com.arsnyan.tracklist.R.drawable.cover_placeholder)
                        error(com.arsnyan.tracklist.R.drawable.cover_placeholder)
                        crossfade(true)
                    }

                    collapsedPlayer.setOnClickListener {
//                        val navController = supportFragmentManager
//                            .findFragmentById(R.id.nav_host_fragment_activity_main)
//                            ?.findNavController()
                        navController?.navigate(R.id.navigation_player)
                    }
                    playPauseBtn.setOnClickListener {
                        when (playbackService?.playbackState?.value) {
                            PlaybackService.PlaybackState.Playing -> {
                                playbackService?.pause()
                            }
                            PlaybackService.PlaybackState.Paused,
                            PlaybackService.PlaybackState.Completed,
                            PlaybackService.PlaybackState.Idle -> {
                                playbackService?.resume()
                            }
                            PlaybackService.PlaybackState.Loading -> {
                                // Optionally handle loading state - probably best to do nothing while loading
                            }
                            is PlaybackService.PlaybackState.Error -> {
                                // Optionally handle error state - maybe try to resume or show an error message
                                playbackService?.resume()
                            }
                            null -> {
                                // Handle case when playbackState is null (shouldn't happen in practice)
                            }
                        }
                    }

                    skipNextBtn.setOnClickListener {
                        sharedViewModel.moveToNextTrack()
                    }

                    skipPreviousBtn.setOnClickListener {
                        sharedViewModel.moveToPreviousTrack()
                    }

                    if (isServiceBound) {
                        playbackService?.play(uiState.track)
                    }
                }
            }

            is SharedViewModel.TrackUiState.Empty, is SharedViewModel.TrackUiState.Loading, is SharedViewModel.TrackUiState.Error -> {
                binding.collapsedPlayer.collapsedPlayer.visibility = View.GONE
            }
        }
    }

    fun getPlaybackService(): PlaybackService? {
        return playbackService
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val isReadGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions[Manifest.permission.READ_MEDIA_AUDIO] == true
            } else {
                permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
            }
            val isPostGranted = permissions[Manifest.permission.POST_NOTIFICATIONS] == true

            if (!isReadGranted) {
                Toast.makeText(baseContext, "Permission denied", Toast.LENGTH_SHORT).show()

                if (!shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", baseContext.packageName, null)
                    }
                    startActivity(intent)
                    Toast.makeText(
                        baseContext,
                        "Please enable storage permission in settings",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            if (!isPostGranted) {
                Toast.makeText(baseContext, "Notification Permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    private fun checkPermission() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS) // Request POST_NOTIFICATIONS
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(baseContext, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray() //Convert to array

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest)
        }
    }
}