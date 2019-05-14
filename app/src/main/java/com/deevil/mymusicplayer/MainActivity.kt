package com.deevil.mymusicplayer

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.player_control.*
import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.database.Cursor
import android.os.Build
import android.widget.Toast.makeText
import androidx.annotation.RequiresApi
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.metadata.id3.Id3Frame
import com.google.android.exoplayer2.metadata.id3.TextInformationFrame
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util


class MainActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 1
    private val TAG = "DBG"


    lateinit var player: ExoPlayer
    lateinit var dataSourceFactory: DefaultDataSourceFactory
    lateinit var trackSelector: DefaultTrackSelector


    @SuppressLint("WrongConstant")
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.w(TAG, "onCreate")
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

        btn_settings.setOnClickListener {

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

        pcv.setOnTouchListener(object : SwipeListener(this){

            override fun onSwipeTop() {
                Log.w(TAG, "onSwipeTop")
            }

            override fun onSwipeRight() {
                Log.w(TAG, "onSwipeRight")
                if (player.hasPrevious()) player.previous()
            }

            override fun onSwipeLeft() {
                Log.w(TAG, "onSwipeLeft")
                if (player.hasNext()) player.next()
            }

            override fun onSwipeBottom() {
                Log.w(TAG, "onSwipeBottom")
               // hom()
            }

        })


        btn_repeat.setOnClickListener {
            btn_repeat.isSelected = !btn_repeat.isSelected
            player.repeatMode = if (btn_repeat.isSelected) 2 else 0

        }

        btn_shuffle.setOnClickListener {
            btn_shuffle.isSelected = !btn_shuffle.isSelected
            player.shuffleModeEnabled = btn_shuffle.isSelected

        }
        player.addListener(object : Player.EventListener {
            override fun onLoadingChanged(isLoading: Boolean) {
                if (!isLoading) {
                    Log.w(TAG, "aaaa")
                }
            }

            override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {
                for (i in 0 until (trackSelections?.length ?: 0)) {
                    val selection = trackSelections?.get(i)
                    for (j in 0 until (selection?.length() ?: 0)) {
                        val metadata: Metadata? = selection?.getFormat(j)?.metadata
                        for (z in 0 until (metadata?.length() ?: 0)) {
                            val metadataEntry = metadata?.get(z)
                            if (metadataEntry is Id3Frame) {
                                when (metadataEntry.id) {
                                    "TPE1" -> Artist.text = (metadataEntry as TextInformationFrame).value
                                    "TIT2" -> Title.text = (metadataEntry as TextInformationFrame).value
                                }
                            }
                        }
                    }
                }
            }
        })

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
                    concatenatedSource.addMediaSource(
                        ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(
                            i
                        )
                    )
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


    override fun onStart() {
        Log.w(TAG, "onStart")
        super.onStart()
    }
    override fun onStop() {
        Log.w(TAG, "onStop")
        super.onStop()
    }

    override fun onRestart() {
        Log.w(TAG, "onRestart")
        super.onRestart()
    }

    override fun onResume() {
        Log.w(TAG, "onResume")
        super.onResume()
    }

    override fun onDestroy() {
        Log.w(TAG, "onDestroy")
        super.onDestroy()
    }

    override fun onPause() {
        Log.w(TAG, "onPause")
        super.onPause()

    }
}
