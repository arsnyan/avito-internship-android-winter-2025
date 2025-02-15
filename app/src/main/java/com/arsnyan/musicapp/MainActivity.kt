package com.arsnyan.musicapp

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.opengl.Visibility
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import coil.load
import com.arsnyan.musicapp.databinding.ActivityMainBinding
import com.arsnyan.musicapp.api.ApiTracksViewModel
import com.arsnyan.musicapp.local.LocalTracksViewModel
import com.arsnyan.musicapp.player.PlaybackService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
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
        navController.addOnDestinationChangedListener { _, _, _ ->
            apiViewModel
            localViewModel
            sharedViewModel
        }
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_api_tracks, R.id.navigation_local_tracks
            )
        )
        navView.setupWithNavController(navController)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkPermission(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        binding.collapsedPlayer.collapsedPlayer.visibility = View.GONE
        observeViewModel()
    }

    fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                sharedViewModel.uiState.collect { uiState ->
                    handleUiTrackState(uiState)
                }
            }
        }
    }

    private fun handleUiTrackState(uiState: SharedViewModel.TrackUiState) {// Handle error state
        if (uiState is SharedViewModel.TrackUiState.Success) {
            with (binding.collapsedPlayer) {
                Log.d("MainActivity", "Track: ${uiState.track}")
                collapsedPlayer.visibility = View.VISIBLE
                trackTitle.text = uiState.track.title
                trackArtist.text = uiState.track.artist.name
                trackCover.load(uiState.track.album.coverUrl) {
                    placeholder(com.arsnyan.tracklist.R.drawable.cover_placeholder)
                    error(com.arsnyan.tracklist.R.drawable.cover_placeholder)
                    crossfade(true)
                }
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (!isGranted) {
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
        }

    private fun checkPermission(permission: String) {
        if (ContextCompat.checkSelfPermission(
                baseContext,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(permission)
        }
    }
}