package com.deevil.mymusicplayer

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder

class PlayerService : Service() {

    var context: Context? = null

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        context = this



        return START_STICKY
    }

    override fun onStart(intent: Intent?, startId: Int) {
        super.onStart(intent, startId)
    }
}
