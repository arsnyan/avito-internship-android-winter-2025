package com.arsnyan.musicapp.player

import android.app.Service
import android.content.Intent
import android.media.MediaSession2
import android.media.MediaSession2Service
import android.os.IBinder

class PlaybackService : MediaSession2Service() {
    override fun onGetSession(controllerInfo: MediaSession2.ControllerInfo): MediaSession2? {
        TODO("Not yet implemented")
    }

    override fun onUpdateNotification(session: MediaSession2): MediaNotification? {
        TODO("Not yet implemented")
    }
}