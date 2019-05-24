package com.deevil.mymusicplayer

import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import com.google.android.exoplayer2.ControlDispatcher
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.upstream.DataSource

class AudioPlayerPreparer(private val exoPlayer: ExoPlayer, private val dataSourceFactory: DataSource.Factory) : MediaSessionConnector.PlaybackPreparer {


    override fun onCommand(player: Player?, controlDispatcher: ControlDispatcher?,
        command: String?,
        extras: Bundle?,
        cb: ResultReceiver?
    ): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    override fun onPrepareFromSearch(query: String?, extras: Bundle?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getSupportedPrepareActions(): Long {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onPrepare() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onCommand(
        player: Player?,
        command: String?,
        extras: Bundle?,
        cb: ResultReceiver?
    ) = Unit

    override fun onPrepareFromMediaId(mediaId: String?, extras: Bundle?) {
        musicSource.whenReady {
            val itemToPlay: MediaMetadataCompat? = musicSource.find { item ->
                item.id == mediaId
            }
            if (itemToPlay == null) {
                Log.w(TAG, "Content not found: MediaID=$mediaId")

                // TODO: Notify caller of the error.
            } else {
                val metadataList = buildPlaylist(itemToPlay)
                val mediaSource = metadataList.toMediaSource(dataSourceFactory)

                // Since the playlist was probably based on some ordering (such as tracks
                // on an album), find which window index to play first so that the song the
                // user actually wants to hear plays first.
                val initialWindowIndex = metadataList.indexOf(itemToPlay)

                exoPlayer.prepare(mediaSource)
                exoPlayer.seekTo(initialWindowIndex, 0)
            }
        }
    }

    override fun onPrepareFromUri(uri: Uri?, extras: Bundle? ) = Unit

    override fun getCommands(): Array<String>? = null



    /**
     * Builds a playlist based on a [MediaMetadataCompat].
     *
     * TODO: Support building a playlist by artist, genre, etc...
     *
     * @param item Item to base the playlist on.
     * @return a [List] of [MediaMetadataCompat] objects representing a playlist.
     */
    private fun buildPlaylist(item: MediaMetadataCompat): List<MediaMetadataCompat> =
        musicSource.filter { it.album == item.album }.sortedBy { it.trackNumber }
}

private const val TAG = "MediaSessionHelper"
