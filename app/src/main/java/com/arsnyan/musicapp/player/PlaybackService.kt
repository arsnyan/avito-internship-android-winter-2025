package com.arsnyan.musicapp.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.arsnyan.musicapp.MainActivity
import com.arsnyan.musicapp.SharedViewModel
import com.arsnyan.tracklist.network.model.Track
import com.arsnyan.tracklist.network.model.TrackSource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackService : Service(), MediaPlayer.OnPreparedListener,
    MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener{
    private var mediaPlayer: MediaPlayer? = null
    private val binder = LocalBinder()
    private val channelId = "PlaybackServiceChannel"
    private val notificationId = 1
    private var currentTrack: Track? = null

    fun getCurrentTrack(): Track? {
        Log.d("PlaybackService", "Is track source available? ${currentTrack?.trackSource}")
        return currentTrack
    }

    private var onTrackCompletionListener: (() -> Unit)? = null

    fun setOnTrackCompletionListener(listener: () -> Unit) {
        onTrackCompletionListener = listener
    }

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition

    companion object {
        private const val PREF_TRACK_ID = "track_id"
        private const val PREF_CURRENT_POSITION = "current_position"
    }

    inner class LocalBinder : Binder() {
        fun getService(): PlaybackService = this@PlaybackService
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    sealed class PlaybackState {
        object Idle : PlaybackState()
        object Loading : PlaybackState()
        object Playing : PlaybackState()
        object Paused : PlaybackState()
        object Completed : PlaybackState()
        data class Error(val message: String) : PlaybackState()
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        serviceScope.launch {
            while (true) {
                if (_playbackState.value == PlaybackState.Playing) {
                    mediaPlayer?.currentPosition?.toLong()?.let {
                        _currentPosition.value = it
                        savePositionToSharedPrefs(it)
                    }
                }
                delay(500)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    fun play(track: Track) {
        currentTrack = track
        releaseMediaPlayer()
        _playbackState.value = PlaybackState.Playing

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setOnPreparedListener(this@PlaybackService)
            setOnErrorListener(this@PlaybackService)
            setOnCompletionListener(this@PlaybackService)

            try {
                if (track.trackSource == TrackSource.LOCAL) {
                    setDataSource(applicationContext, Uri.parse(track.srcUrl))
                } else {
                    setDataSource(track.srcUrl)
                }
                startForeground(notificationId, createNotification())
                prepareAsync()
            } catch (e: Exception) {
                handleMediaError(e)
            }
        }
    }

    fun resume() {
        if (mediaPlayer != null && _playbackState.value == PlaybackState.Paused) {
            mediaPlayer?.start()
            _playbackState.value = PlaybackState.Playing
            startForeground(notificationId, createNotification())
        }
    }

    fun pause() {
        if (mediaPlayer != null && _playbackState.value == PlaybackState.Playing) {
            mediaPlayer?.pause()
            _playbackState.value = PlaybackState.Paused
            stopForeground(false)
        }
    }

    fun stop() {
        mediaPlayer?.run {
            stop()
            reset()
        }
        _playbackState.value = PlaybackState.Idle
        stopForeground(true)
        _currentPosition.value = 0L
        savePositionToSharedPrefs(0L)
    }

    fun seekTo(position: Long) {
        mediaPlayer?.seekTo(position.toInt())
        _currentPosition.value = position
        savePositionToSharedPrefs(position)
    }

    private fun handleMediaError(e: Exception) {
        Log.e("MusicService", "Error setting data source", e)
        Toast.makeText(this@PlaybackService, "Error playing music", Toast.LENGTH_SHORT).show()
        releaseMediaPlayer()
        stopForeground(true)
        _playbackState.value = PlaybackState.Error("Error playing music: ${e.message}")
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onPrepared(mp: MediaPlayer) {
        serviceScope.launch {
            val savedPosition = getSavedPosition()
            if (savedPosition > 0) {
                mediaPlayer?.seekTo(savedPosition.toInt())
            }
            mp.start()
            _playbackState.value = PlaybackState.Playing
        }
    }

    override fun onError(
        mp: MediaPlayer?,
        what: Int,
        extra: Int
    ): Boolean {
        Log.e("MusicService", "MediaPlayer error: what=$what, extra=$extra")
        Toast.makeText(this, "MediaPlayer error", Toast.LENGTH_SHORT).show()
        releaseMediaPlayer()
        stopForeground(true)
        _playbackState.value = PlaybackState.Error("MediaPlayer error: $what")
        return true
    }

    override fun onCompletion(mp: MediaPlayer?) {
        Log.d("PlaybackService", "Track completed, sending broadcast")
        _playbackState.value = PlaybackState.Completed
        releaseMediaPlayer()
        onTrackCompletionListener?.invoke()
        stopForeground(true)
    }

    private fun createNotificationChannel() {
        val name = "Music Player"
        val descriptionText = "Notifications for music playback"
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(channelId, name, importance).apply {
            description = descriptionText
            setSound(null, null)
        }
        val notificationManager: NotificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("track_id", currentTrack?.id ?: -1)
            putExtra("track_source", currentTrack?.trackSource?.name)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(this, channelId)
            .setContentTitle(currentTrack?.title ?: "Music Player")
            .setContentText(currentTrack?.artist?.name ?: "Unknown artist")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun savePositionToSharedPrefs(position: Long) {
        sharedPreferences.edit().apply {
            currentTrack?.id?.let { putLong(PREF_TRACK_ID, it) }
            putLong(PREF_CURRENT_POSITION, position)
            apply()
        }
    }

    fun getSavedPosition(): Long {
        val savedTrackId = sharedPreferences.getLong(PREF_TRACK_ID, -1)
        val savedPosition = sharedPreferences.getLong(PREF_CURRENT_POSITION, 0L)

        // Check if the saved track matches the current track
        return if (savedTrackId == currentTrack?.id) {
            savedPosition
        } else {
            0L // Return 0 if the saved track is different or invalid
        }
    }
}