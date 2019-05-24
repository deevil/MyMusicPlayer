package com.deevil.mymusicplayer

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.media.session.MediaButtonReceiver
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector

import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.extractor.ExtractorsFactory
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.google.android.exoplayer2.util.Util



import java.io.File


class AudioPlayerService : Service() {

    private val TAG = "DBG-SRC"

    private val NOTIFICATION_ID = 404
    private val NOTIFICATION_DEFAULT_CHANNEL_ID = "default_channel"

    private val metadataBuilder = MediaMetadataCompat.Builder()

    private val stateBuilder = PlaybackStateCompat.Builder().setActions(
        PlaybackStateCompat.ACTION_PLAY
                or PlaybackStateCompat.ACTION_STOP
                or PlaybackStateCompat.ACTION_PAUSE
                or PlaybackStateCompat.ACTION_PLAY_PAUSE
                or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
    )

    private lateinit var mediaSession: MediaSessionCompat

    private lateinit var audioManager: AudioManager
    //private lateinit var audioFocusRequest: AudioFocusRequest
    //private var audioFocusRequested = false

    private lateinit var exoPlayer: SimpleExoPlayer
    private lateinit var extractorsFactory: ExtractorsFactory
    private lateinit var dataSourceFactory: DataSource.Factory
    private lateinit var mediaSessionConnector: MediaSessionConnector

    private val musicRepository = MusicRepository()

    private val exoPlayerListener = object : Player.EventListener {
        override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {}

        override fun onLoadingChanged(isLoading: Boolean) {}

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            if (playWhenReady && playbackState == ExoPlayer.STATE_ENDED) {
                mediaSessionCallback.onSkipToNext()
            }
        }

        override fun onPlayerError(error: ExoPlaybackException?) {}

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {}


        fun onTimelineChanged(timeline: Timeline, manifest: Any) {}

        fun onPositionDiscontinuity() {}


    }


//
//    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
//        when (focusChange) {
//            AudioManager.AUDIOFOCUS_GAIN -> mediaSessionCallback.onPlay() // Не очень красиво
//            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> mediaSessionCallback.onPause()
//            else -> mediaSessionCallback.onPause()
//        }
//    }
//


    override fun onCreate() {
        Log.w(TAG, "onCreate")
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                NOTIFICATION_DEFAULT_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
//            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
//                .setOnAudioFocusChangeListener(audioFocusChangeListener)
//                .setAcceptsDelayedFocusGain(false)
//                .setWillPauseWhenDucked(true)
//                .setAudioAttributes(audioAttributes)
//                .build()
        }

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        mediaSession = MediaSessionCompat(this, "PlayerService")
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        mediaSession.setCallback(mediaSessionCallback)



        val appContext = applicationContext

        val activityIntent = Intent(appContext, MainActivity::class.java)
        mediaSession.setSessionActivity(PendingIntent.getActivity(appContext, 0, activityIntent, 0))

        val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON, null, appContext, MediaButtonReceiver::class.java)
        mediaSession.setMediaButtonReceiver(PendingIntent.getBroadcast(appContext, 0, mediaButtonIntent, 0))

        exoPlayer = ExoPlayerFactory.newSimpleInstance(this, DefaultTrackSelector())
        exoPlayer.addListener(exoPlayerListener)

        this.dataSourceFactory = DefaultDataSourceFactory(this, Util.getUserAgent(this, getString(R.string.app_name)))
        this.extractorsFactory = DefaultExtractorsFactory()

        mediaSessionConnector = MediaSessionConnector(mediaSession)
        mediaSessionConnector.setPlayer(exoPlayer)
//        playbackPreparer = new YourPlaybackPreparer();
//mediaSessionConnector.setPlayer([(player, playbackPreparer, null);


    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.w(TAG, "onStartCommand")
        MediaButtonReceiver.handleIntent(mediaSession, intent)

        val extras = intent.extras
        if (extras != null) {
            Log.d("Service", "not null");
            val lst = extras.getParcelableArrayList<Uri>("lst")
            if (lst != null){
                if (lst.size != 0) {
                    Log.w(TAG, "lst.size != 0")
                    val concatenatedSource = ConcatenatingMediaSource()
                    for (i in lst) {
                        concatenatedSource.addMediaSource(ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(i))
                    }
                    exoPlayer.prepare(concatenatedSource)

                    exoPlayer.playWhenReady = true
                }
            }
        }


        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession!!.release()
        exoPlayer!!.release()
    }

    override fun onBind(intent: Intent): IBinder {
        Log.w(TAG, "onBind")
        return AudioPlayerServiceBinder()
    }

    inner class AudioPlayerServiceBinder : Binder() {
        val mediaSessionToken: MediaSessionCompat.Token
            get() = mediaSession.sessionToken
    }

    private fun refreshNotificationAndForegroundStatus(playbackState: Int) {
        when (playbackState) {
            PlaybackStateCompat.STATE_PLAYING -> {
                startForeground(NOTIFICATION_ID, getNotification(playbackState))
            }
            PlaybackStateCompat.STATE_PAUSED -> {
                NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, getNotification(playbackState))
                stopForeground(false)
            }
            else -> {
                stopForeground(true)
            }
        }
    }

    private fun getNotification(playbackState: Int): Notification {
        val builder = MediaStyleHelper.from(this, mediaSession)
        builder.addAction(
            NotificationCompat.Action(
                android.R.drawable.ic_media_previous,
                getString(R.string.previous),
                MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
            )
        )

        if (playbackState == PlaybackStateCompat.STATE_PLAYING)
            builder.addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_media_pause,
                    getString(R.string.pause),
                    MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE)
                )
            )
        else
            builder.addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_media_play,
                    getString(R.string.play),
                    MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE)
                )
            )

        builder.addAction(
            NotificationCompat.Action(
                android.R.drawable.ic_media_next,
                getString(R.string.next),
                MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
            )
        )
        builder.setStyle(
            androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(1)
                .setShowCancelButton(true)
                .setCancelButtonIntent(
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        this,
                        PlaybackStateCompat.ACTION_STOP
                    )
                )
                .setMediaSession(mediaSession!!.sessionToken)
        ) // setMediaSession требуется для Android Wear
        builder.setSmallIcon(R.drawable.ic_launcher_icon)
        builder.color = ContextCompat.getColor(
            this,
            R.color.colorPrimaryDark
        ) // The whole background (in MediaStyle), not just icon background
        builder.setShowWhen(false)
        builder.priority = NotificationCompat.PRIORITY_HIGH
        builder.setOnlyAlertOnce(true)
        builder.setChannelId(NOTIFICATION_DEFAULT_CHANNEL_ID)

        return builder.build()
    }


    private val mediaSessionCallback = object : MediaSessionCompat.Callback() {

        private lateinit var currentUri: Uri
        internal var currentState = PlaybackStateCompat.STATE_STOPPED

        override fun onPlay() {

            if (!exoPlayer.playWhenReady) {
                startService(Intent(applicationContext, AudioPlayerServiceBinder::class.java))

                val track = musicRepository.current
                updateMetadataFromTrack(track)

                prepareToPlay(track.uri)

//                if (!audioFocusRequested) {
//                    audioFocusRequested = true
//
//                    val audioFocusResult: Int
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                        audioFocusResult = audioManager.requestAudioFocus(audioFocusRequest)
//                    } else {
//                        audioFocusResult = audioManager.requestAudioFocus(
//                            audioFocusChangeListener,
//                            AudioManager.STREAM_MUSIC,
//                            AudioManager.AUDIOFOCUS_GAIN
//                        )
//                    }
//                    if (audioFocusResult != AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
//                        return
//                }

                mediaSession.isActive = true // Сразу после получения фокуса
                exoPlayer.playWhenReady = true
            }

            mediaSession.setPlaybackState(
                stateBuilder.setState(
                    PlaybackStateCompat.STATE_PLAYING,
                    PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                    1f
                ).build()
            )
            currentState = PlaybackStateCompat.STATE_PLAYING

            refreshNotificationAndForegroundStatus(currentState)
        }

//        override fun onAddQueueItem(description: MediaDescriptionCompat?) {
//            super.onAddQueueItem(description)
//        }

        override fun onPause() {
            if (exoPlayer.playWhenReady) {
                exoPlayer.playWhenReady = false
            }

            mediaSession.setPlaybackState(
                stateBuilder.setState(
                    PlaybackStateCompat.STATE_PAUSED,
                    PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                    1f
                ).build()
            )
            currentState = PlaybackStateCompat.STATE_PAUSED

            refreshNotificationAndForegroundStatus(currentState)
        }

        override fun onStop() {
            if (exoPlayer.playWhenReady) {
                exoPlayer.playWhenReady = false
            }

//            if (audioFocusRequested) {
//                audioFocusRequested = false
//
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                    audioManager.abandonAudioFocusRequest(audioFocusRequest)
//                } else {
//                    audioManager.abandonAudioFocus(audioFocusChangeListener)
//                }
//            }

            mediaSession.isActive = false

            mediaSession.setPlaybackState(
                stateBuilder.setState(
                    PlaybackStateCompat.STATE_STOPPED,
                    PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                    1f
                ).build()
            )
            currentState = PlaybackStateCompat.STATE_STOPPED

            refreshNotificationAndForegroundStatus(currentState)

            stopSelf()
        }

        override fun onSkipToNext() {
            val track = musicRepository.next
            updateMetadataFromTrack(track)

            refreshNotificationAndForegroundStatus(currentState)

            prepareToPlay(track.uri)
        }

        override fun onSkipToPrevious() {
            val track = musicRepository.previous
            updateMetadataFromTrack(track)

            refreshNotificationAndForegroundStatus(currentState)

            prepareToPlay(track.uri)
        }

        private fun prepareToPlay(uri: Uri) {
            if (uri != currentUri) {
                currentUri = uri
                val mediaSource = ExtractorMediaSource(uri, dataSourceFactory, extractorsFactory, null, null)
                exoPlayer.prepare(mediaSource)
            }
        }

        private fun updateMetadataFromTrack(track: MusicRepository.Track) {
//            metadataBuilder.putBitmap(
//                MediaMetadataCompat.METADATA_KEY_ART,
//                BitmapFactory.decodeResource(resources, track.bitmapResId)
//            )
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.title)
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, track.artist)
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.artist)
            metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, track.duration)
            mediaSession.setMetadata(metadataBuilder.build())
        }
    }




}




internal object MediaStyleHelper {
    /**
     * Build a notification using the information from the given media session. Makes heavy use
     * of [MediaMetadataCompat.getDescription] to extract the appropriate information.
     *
     * @param context      Context used to construct the notification.
     * @param mediaSession Media session to get information.
     * @return A pre-built notification with information from the given media session.
     */
    fun from(context: Context, mediaSession: MediaSessionCompat): NotificationCompat.Builder {
        val controller = mediaSession.controller
        val mediaMetadata = controller.metadata
        val description = mediaMetadata.description

        val builder = NotificationCompat.Builder(context)
        builder
            .setContentTitle(description.title)
            .setContentText(description.subtitle)
            .setSubText(description.description)
            .setLargeIcon(description.iconBitmap)
            .setContentIntent(controller.sessionActivity)
            .setDeleteIntent(
                MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP)
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        return builder
    }
}