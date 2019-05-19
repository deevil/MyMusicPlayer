package com.deevil.mymusicplayer

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util


class PlayerService : Service() {

    private lateinit var player: SimpleExoPlayer
    private lateinit var dataSourceFactory: DefaultDataSourceFactory
    private val TAG = "DBG"

    override fun onCreate() {

        Log.w(TAG, "onCreate")

        super.onCreate()
        val context = this

        player = ExoPlayerFactory.newSimpleInstance(context, DefaultTrackSelector())
        dataSourceFactory = DefaultDataSourceFactory(this, Util.getUserAgent(this, getString(R.string.app_name)))

    }

    override fun onDestroy() {
        Log.w(TAG, "onDestroy")
        player.release()

        super.onDestroy()
    }


    override fun onBind(intent: Intent): IBinder? {
        Log.w(TAG, "onBind")

        return PlayerServiceBinder()

    }

    inner class PlayerServiceBinder : Binder() {
        fun getPlayerInstance() = player
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.w(TAG, "onStartCommand")
        super.onStart(intent, startId)
        return START_STICKY
    }


}

