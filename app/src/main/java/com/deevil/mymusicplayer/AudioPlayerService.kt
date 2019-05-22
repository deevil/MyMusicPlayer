package com.deevil.mymusicplayer

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.os.IBinder
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import com.deevil.mymusicplayer.Samples.SAMPLES
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory
import com.google.android.exoplayer2.util.Util

class AudioPlayerService : Service() {

    private lateinit var player: SimpleExoPlayer
    private lateinit var playerNotificationManager: PlayerNotificationManager
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector


    override fun onCreate() {
        super.onCreate()

        val context = this

        player = ExoPlayerFactory.newSimpleInstance(context, DefaultTrackSelector())

        val dataSourceFactory = DefaultDataSourceFactory(context, Util.getUserAgent(context, getString(R.string.app_name)))

        val concatenatingMediaSource = ConcatenatingMediaSource()

        for (sample in SAMPLES) {
            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(sample.uri)
            concatenatingMediaSource.addMediaSource(mediaSource)
        }
        player.prepare(concatenatingMediaSource)

        player.playWhenReady = true


        playerNotificationManager = PlayerNotificationManager.createWithNotificationChannel(
            context,
            C.PLAYBACK_CHANNEL_ID,
            R.string.playback_channel_name,
            C.PLAYBACK_NOTIFICATION_ID,
            object : PlayerNotificationManager.MediaDescriptionAdapter {
                override fun getCurrentContentTitle(player: Player): String {
                    return SAMPLES[player.currentWindowIndex].title
                }

                override fun createCurrentContentIntent(player: Player): PendingIntent? {
                    return null
                }

                override fun getCurrentContentText(player: Player): String? {
                    return SAMPLES[player.currentWindowIndex].description
                }

                override fun getCurrentLargeIcon(player: Player, callback: PlayerNotificationManager.BitmapCallback): Bitmap? {
                    return SAMPLES[player.currentWindowIndex].bitmapResource?.let {
                        Samples.getBitmap(
                            context, it
                        )
                    }
                }
            }
        )


        playerNotificationManager.setNotificationListener(object : PlayerNotificationManager.NotificationListener {
            override fun onNotificationStarted(notificationId: Int, notification: Notification?) {
                startForeground(notificationId, notification)
            }

            override fun onNotificationCancelled(notificationId: Int) {
                stopSelf()
            }
        })

        playerNotificationManager.setPlayer(player)

        mediaSession = MediaSessionCompat(context, C.MEDIA_SESSION_TAG)
        mediaSession.setActive(true)
        playerNotificationManager.setMediaSessionToken(mediaSession.getSessionToken())

        mediaSessionConnector = MediaSessionConnector(mediaSession)
        mediaSessionConnector.setQueueNavigator(object : TimelineQueueNavigator(mediaSession) {
            override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
                return Samples.getMediaDescription(context, SAMPLES[windowIndex])
            }
        })
        mediaSessionConnector.setPlayer(player)
    }

    override fun onDestroy() {

        mediaSession.release()
        mediaSessionConnector.setPlayer(null)
        playerNotificationManager.setPlayer(null)
        player.release()
        //player = null

        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
        //return super.onStartCommand(intent, flags, startId)
    }
}
