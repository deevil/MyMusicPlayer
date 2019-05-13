package com.deevil.mymusicplayer

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.graphics.drawable.RoundedBitmapDrawable
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.metadata.id3.ApicFrame
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.PlayerControlView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.EventLogger
import com.google.android.exoplayer2.util.Util
import kotlinx.android.synthetic.main.activity_main.view.*
import kotlinx.android.synthetic.main.player_control.*
import kotlinx.android.synthetic.main.player_view.*
import com.deevil.mymusicplayer.CompListener as CompListener1

/*
if (useArtwork)
{
    for (i in 0 until selections.length) {
        val selection = selections.get(i)
        if (selection != null) {
            for (j in 0 until selection!!.length()) {
                val metadata = selection!!.getFormat(j).metadata
                if (metadata != null && setArtworkFromMetadata(metadata!!)) {
                    return
                }
            }
        }
    }
    if (setDrawableArtwork(defaultArtwork)) {
        return
    }
}*/
//
//private fun setArtworkFromMetadata(metadata: Metadata): Boolean {
//    for (i in 0 until metadata.length()) {
//        val metadataEntry = metadata.get(i)
//        if (metadataEntry is ApicFrame) {
//            val bitmapData = metadataEntry.pictureData
//            val bitmap = BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.size)
//            return setDrawableArtwork(BitmapDrawable(getResources(), bitmap))
//        }
//    }
//    return false
//}


class MainActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 1
    private val TAG = "DBG"


    lateinit var player: ExoPlayer
    lateinit var dataSourceFactory: DefaultDataSourceFactory
    lateinit var trackSelector:DefaultTrackSelector


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        trackSelector = DefaultTrackSelector()
        //var pc:PlayerControlView

        player = ExoPlayerFactory.newSimpleInstance(this, trackSelector)
        //player.debug
        dataSourceFactory = DefaultDataSourceFactory(this, Util.getUserAgent(this, "yourApplicationName"))


        //pc.player = player
        pcv.player = player
        pcv.controllerHideOnTouch = false
        var componentListener = com.deevil.mymusicplayer.CompListener()
        player.addListener(componentListener )

        //player.addListener()
        //.addAnalyticsListener(EventLogger(trackSelector))
        //player.liste(Player.onT)onTracksChanged

        button.setOnClickListener {

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    PERMISSION_REQUEST_CODE
                )
            } else {
                ChooseDirectory()
            }

        }



//        exo_play.setOnClickListener{
//            Log.w(TAG, "sadfasf")
//            it.hasOnClickListeners()
//        }
        //player.addListener{};
        player.addListener(object : Player.EventListener {
            override fun onLoadingChanged(isLoading:Boolean){
                if (!isLoading) {
                    Log.w(TAG, "aaaa")
                }

            }
            override fun onPlayerStateChanged(playWhenReady: Boolean,playbackState: Int) {

            }

            override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {
                for (i in 0 until (trackSelections?.length ?: 0)) {
                    val selection = trackSelections?.get(i)
                    if (selection != null) {
                        for (j in 0 until selection!!.length()) {
                            val metadata: Metadata? = selection!!.getFormat(j).metadata
                            if (metadata != null) {
                                metadata. .(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                                return
                            }
                        }
                    }
                }

            }

//            override fun onMetadata(metadata:Metadata ){
//
//            }
        })
        //player.addMetadataOutput {  }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    @SuppressLint("MissingSuperCall")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 9999 && resultCode == RESULT_OK && data != null && data.data != null) {
            Log.i(TAG, "Result URI " + data.data)

            //if (data.data != null) return

            val treeUri: Uri = data.data
            val lst = getAllAudioFromTree(treeUri)
            if (lst.size > 0) {

                var concatenatedSource = ConcatenatingMediaSource()
                for (i in lst) {
                    concatenatedSource.addMediaSource(ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(i))
                }

                player.prepare(concatenatedSource)
            }

        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Permission granted!")
                ChooseDirectory()
            } else {
                Log.i(TAG, "Permission denied")
                Toast.makeText(this, "Not permission to read files", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun ChooseDirectory() {
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)

        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        i.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        i.addCategory(Intent.CATEGORY_DEFAULT)
        //i.setType("audio/*");
        startActivityForResult(Intent.createChooser(i, "Choose directory"), 9999)
    }

    private fun getAllAudioFromTree(treeUri: Uri, inpParentDocumentId: String? = null): ArrayList<Uri> {

        var parentDocumentId = inpParentDocumentId ?: DocumentsContract.getTreeDocumentId(treeUri)

        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId)
        var children: Cursor? = null

        var res: ArrayList<Uri> = ArrayList<Uri>()

        try {
            children = contentResolver.query(
                childrenUri,
                arrayOf(Document.COLUMN_DOCUMENT_ID, Document.COLUMN_MIME_TYPE),
                null,
                null,
                null
            )
        } catch (e: NullPointerException) {
            Log.e(TAG, "Error reading $childrenUri", e)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Error reading $childrenUri", e)
        } catch (e: SecurityException) {
            Log.e(TAG, "Error reading $childrenUri", e)
        }

        if (children == null) {
            return res
        }


        while (children.moveToNext()) {
            val documentId = children.getString(children.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID))
            val mimeType = children.getString(children.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE))

            if (Document.MIME_TYPE_DIR == mimeType) {
                for (i in getAllAudioFromTree(treeUri, documentId)) {
                    res.add(i)
                }
            } else if (mimeType != null && mimeType.startsWith("audio/")) {
                res.add(DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId))
            }
        }

        children.close()

        return res
    }


}
