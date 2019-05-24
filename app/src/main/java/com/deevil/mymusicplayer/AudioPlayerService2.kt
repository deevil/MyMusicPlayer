//package com.deevil.mymusicplayer
//
//import android.app.*
//import android.content.BroadcastReceiver
//import android.content.Context
//import android.content.Intent
//import android.content.IntentFilter
//import android.graphics.BitmapFactory
//import android.media.AudioAttributes
//import android.media.AudioFocusRequest
//import android.media.AudioManager
//import android.net.Uri
//import android.os.Binder
//import android.os.Build
//import android.os.IBinder
//import android.support.v4.media.MediaMetadataCompat
//import android.support.v4.media.session.MediaSessionCompat
//import android.support.v4.media.session.PlaybackStateCompat
//import com.google.android.exoplayer2.*
//import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
//import com.google.android.exoplayer2.extractor.ExtractorsFactory
//import com.google.android.exoplayer2.source.ExtractorMediaSource
//import com.google.android.exoplayer2.source.TrackGroupArray
//import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
//import com.google.android.exoplayer2.trackselection.TrackSelectionArray
//import com.google.android.exoplayer2.upstream.DataSource
//import com.google.android.exoplayer2.upstream.cache.CacheDataSource
//import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory
//import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
//import com.google.android.exoplayer2.upstream.cache.SimpleCache
//import com.google.android.exoplayer2.util.Util
//import java.io.File
//
//class PlayerService : Service() {
//
//    private val NOTIFICATION_ID = 404
//    private val NOTIFICATION_DEFAULT_CHANNEL_ID = "default_channel"
//
//    private val metadataBuilder = MediaMetadataCompat.Builder()
//
//    private val stateBuilder = PlaybackStateCompat.Builder().setActions(
//        PlaybackStateCompat.ACTION_PLAY
//                or PlaybackStateCompat.ACTION_STOP
//                or PlaybackStateCompat.ACTION_PAUSE
//                or PlaybackStateCompat.ACTION_PLAY_PAUSE
//                or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
//                or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
//    )
//
//    private var mediaSession: MediaSessionCompat? = null
//
//    private var audioManager: AudioManager? = null
//    private var audioFocusRequest: AudioFocusRequest? = null
//    private var audioFocusRequested = false
//
//    private var exoPlayer: SimpleExoPlayer? = null
//    private var extractorsFactory: ExtractorsFactory? = null
//    private var dataSourceFactory: DataSource.Factory? = null
//
//    private val musicRepository = MusicRepository()
//
//    private val mediaSessionCallback = object : MediaSessionCompat.Callback() {
//
//        private var currentUri: Uri? = null
//        internal var currentState = PlaybackStateCompat.STATE_STOPPED
//
//        override fun onPlay() {
//            if (!exoPlayer!!.playWhenReady) {
//                startService(Intent(applicationContext, PlayerServiceOld::class.java))
//
//                val track = musicRepository.getCurrent()
//                updateMetadataFromTrack(track)
//
//                prepareToPlay(track.getUri())
//
//                if (!audioFocusRequested) {
//                    audioFocusRequested = true
//
//                    val audioFocusResult: Int
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                        audioFocusResult = audioManager!!.requestAudioFocus(audioFocusRequest!!)
//                    } else {
//                        audioFocusResult = audioManager!!.requestAudioFocus(
//                            audioFocusChangeListener,
//                            AudioManager.STREAM_MUSIC,
//                            AudioManager.AUDIOFOCUS_GAIN
//                        )
//                    }
//                    if (audioFocusResult != AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
//                        return
//                }
//
//                mediaSession!!.isActive = true // Сразу после получения фокуса
//
//                registerReceiver(becomingNoisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
//
//                exoPlayer!!.playWhenReady = true
//            }
//
//            mediaSession!!.setPlaybackState(
//                stateBuilder.setState(
//                    PlaybackStateCompat.STATE_PLAYING,
//                    PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
//                    1f
//                ).build()
//            )
//            currentState = PlaybackStateCompat.STATE_PLAYING
//
//            refreshNotificationAndForegroundStatus(currentState)
//        }
//
//        override fun onPause() {
//            if (exoPlayer!!.playWhenReady) {
//                exoPlayer!!.playWhenReady = false
//                unregisterReceiver(becomingNoisyReceiver)
//            }
//
//            mediaSession!!.setPlaybackState(
//                stateBuilder.setState(
//                    PlaybackStateCompat.STATE_PAUSED,
//                    PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
//                    1f
//                ).build()
//            )
//            currentState = PlaybackStateCompat.STATE_PAUSED
//
//            refreshNotificationAndForegroundStatus(currentState)
//        }
//
//        override fun onStop() {
//            if (exoPlayer!!.playWhenReady) {
//                exoPlayer!!.playWhenReady = false
//                unregisterReceiver(becomingNoisyReceiver)
//            }
//
//            if (audioFocusRequested) {
//                audioFocusRequested = false
//
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                    audioManager!!.abandonAudioFocusRequest(audioFocusRequest!!)
//                } else {
//                    audioManager!!.abandonAudioFocus(audioFocusChangeListener)
//                }
//            }
//
//            mediaSession!!.isActive = false
//
//            mediaSession!!.setPlaybackState(
//                stateBuilder.setState(
//                    PlaybackStateCompat.STATE_STOPPED,
//                    PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
//                    1f
//                ).build()
//            )
//            currentState = PlaybackStateCompat.STATE_STOPPED
//
//            refreshNotificationAndForegroundStatus(currentState)
//
//            stopSelf()
//        }
//
//        override fun onSkipToNext() {
//            val track = musicRepository.getNext()
//            updateMetadataFromTrack(track)
//
//            refreshNotificationAndForegroundStatus(currentState)
//
//            prepareToPlay(track.getUri())
//        }
//
//        override fun onSkipToPrevious() {
//            val track = musicRepository.getPrevious()
//            updateMetadataFromTrack(track)
//
//            refreshNotificationAndForegroundStatus(currentState)
//
//            prepareToPlay(track.getUri())
//        }
//
//        private fun prepareToPlay(uri: Uri) {
//            if (uri != currentUri) {
//                currentUri = uri
//                val mediaSource = ExtractorMediaSource(uri, dataSourceFactory, extractorsFactory, null, null)
//                exoPlayer!!.prepare(mediaSource)
//            }
//        }
//
//        private fun updateMetadataFromTrack(track: MusicRepository.Track) {
//            metadataBuilder.putBitmap(
//                MediaMetadataCompat.METADATA_KEY_ART,
//                BitmapFactory.decodeResource(resources, track.getBitmapResId())
//            )
//            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.getTitle())
//            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, track.getArtist())
//            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.getArtist())
//            metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, track.getDuration())
//            mediaSession!!.setMetadata(metadataBuilder.build())
//        }
//    }
//
//    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
//        when (focusChange) {
//            AudioManager.AUDIOFOCUS_GAIN -> mediaSessionCallback.onPlay() // Не очень красиво
//            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> mediaSessionCallback.onPause()
//            else -> mediaSessionCallback.onPause()
//        }
//    }
//
//    private val becomingNoisyReceiver = object : BroadcastReceiver() {
//        override fun onReceive(context: Context, intent: Intent) {
//            // Disconnecting headphones - stop playback
//            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent.action) {
//                mediaSessionCallback.onPause()
//            }
//        }
//    }
//
//    private val exoPlayerListener = object : ExoPlayer.EventListener {
//        fun onTimelineChanged(timeline: Timeline, manifest: Any) {}
//
//        override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {}
//
//        override fun onLoadingChanged(isLoading: Boolean) {}
//
//        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
//            if (playWhenReady && playbackState == ExoPlayer.STATE_ENDED) {
//                mediaSessionCallback.onSkipToNext()
//            }
//        }
//
//        override fun onPlayerError(error: ExoPlaybackException?) {}
//
//        fun onPositionDiscontinuity() {}
//
//        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {}
//    }
//
//    override fun onCreate() {
//        super.onCreate()
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val notificationChannel = NotificationChannel(
//                NOTIFICATION_DEFAULT_CHANNEL_ID,
//                getString(R.string.notification_channel_name),
//                NotificationManagerCompat.IMPORTANCE_DEFAULT
//            )
//            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//            notificationManager.createNotificationChannel(notificationChannel)
//
//            val audioAttributes = AudioAttributes.Builder()
//                .setUsage(AudioAttributes.USAGE_MEDIA)
//                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
//                .build()
//            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
//                .setOnAudioFocusChangeListener(audioFocusChangeListener)
//                .setAcceptsDelayedFocusGain(false)
//                .setWillPauseWhenDucked(true)
//                .setAudioAttributes(audioAttributes)
//                .build()
//        }
//
//        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
//
//        mediaSession = MediaSessionCompat(this, "PlayerServiceOld")
//        mediaSession!!.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
//        mediaSession!!.setCallback(mediaSessionCallback)
//
//        val appContext = applicationContext
//
//        val activityIntent = Intent(appContext, MainActivity::class.java)
//        mediaSession!!.setSessionActivity(PendingIntent.getActivity(appContext, 0, activityIntent, 0))
//
//        val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON, null, appContext, MediaButtonReceiver::class.java)
//        mediaSession!!.setMediaButtonReceiver(PendingIntent.getBroadcast(appContext, 0, mediaButtonIntent, 0))
//
//        exoPlayer = ExoPlayerFactory.newSimpleInstance(
//            DefaultRenderersFactory(this),
//            DefaultTrackSelector(),
//            DefaultLoadControl()
//        )
//        exoPlayer!!.addListener(exoPlayerListener)
//        val httpDataSourceFactory =
//            OkHttpDataSourceFactory(OkHttpClient(), Util.getUserAgent(this, getString(R.string.app_name)), null)
//        val cache = SimpleCache(
//            File(this.cacheDir.absolutePath + "/exoplayer"),
//            LeastRecentlyUsedCacheEvictor((1024 * 1024 * 100).toLong())
//        ) // 100 Mb max
//        this.dataSourceFactory = CacheDataSourceFactory(
//            cache,
//            httpDataSourceFactory,
//            CacheDataSource.FLAG_BLOCK_ON_CACHE or CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR
//        )
//        this.extractorsFactory = DefaultExtractorsFactory()
//    }
//
//    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
//        MediaButtonReceiver.handleIntent(mediaSession, intent)
//        return super.onStartCommand(intent, flags, startId)
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        mediaSession!!.release()
//        exoPlayer!!.release()
//    }
//
//    @Nullable
//    override fun onBind(intent: Intent): IBinder {
//        return PlayerServiceBinder()
//    }
//
//    inner class PlayerServiceBinder : Binder() {
//        val mediaSessionToken: MediaSessionCompat.Token
//            get() = mediaSession!!.sessionToken
//    }
//
//    private fun refreshNotificationAndForegroundStatus(playbackState: Int) {
//        when (playbackState) {
//            PlaybackStateCompat.STATE_PLAYING -> {
//                startForeground(NOTIFICATION_ID, getNotification(playbackState))
//            }
//            PlaybackStateCompat.STATE_PAUSED -> {
//                NotificationManagerCompat.from(this@PlayerServiceOld)
//                    .notify(NOTIFICATION_ID, getNotification(playbackState))
//                stopForeground(false)
//            }
//            else -> {
//                stopForeground(true)
//            }
//        }
//    }
//
//    private fun getNotification(playbackState: Int): Notification {
//        val builder = MediaStyleHelper.from(this, mediaSession)
//        builder.addAction(
//            NotificationCompat.Action(
//                android.R.drawable.ic_media_previous,
//                getString(R.string.previous),
//                MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
//            )
//        )
//
//        if (playbackState == PlaybackStateCompat.STATE_PLAYING)
//            builder.addAction(
//                NotificationCompat.Action(
//                    android.R.drawable.ic_media_pause,
//                    getString(R.string.pause),
//                    MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE)
//                )
//            )
//        else
//            builder.addAction(
//                NotificationCompat.Action(
//                    android.R.drawable.ic_media_play,
//                    getString(R.string.play),
//                    MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE)
//                )
//            )
//
//        builder.addAction(
//            NotificationCompat.Action(
//                android.R.drawable.ic_media_next,
//                getString(R.string.next),
//                MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
//            )
//        )
//        builder.setStyle(
//            MediaStyle()
//                .setShowActionsInCompactView(1)
//                .setShowCancelButton(true)
//                .setCancelButtonIntent(
//                    MediaButtonReceiver.buildMediaButtonPendingIntent(
//                        this,
//                        PlaybackStateCompat.ACTION_STOP
//                    )
//                )
//                .setMediaSession(mediaSession!!.sessionToken)
//        ) // setMediaSession требуется для Android Wear
//        builder.setSmallIcon(R.mipmap.ic_launcher)
//        builder.setColor(
//            ContextCompat.getColor(
//                this,
//                R.color.colorPrimaryDark
//            )
//        ) // The whole background (in MediaStyle), not just icon background
//        builder.setShowWhen(false)
//        builder.setPriority(NotificationCompat.PRIORITY_HIGH)
//        builder.setOnlyAlertOnce(true)
//        builder.setChannelId(NOTIFICATION_DEFAULT_CHANNEL_ID)
//
//        return builder.build()
//    }
//}
