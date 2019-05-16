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
import android.view.View
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.metadata.id3.Id3Frame
import com.google.android.exoplayer2.metadata.id3.TextInformationFrame
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import kotlinx.android.synthetic.main.select.*


class MainActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 9998
    private val DIRECTORY_REQUEST_CODE = 9999
    private val TAG = "DBG"


    lateinit var player: ExoPlayer
    lateinit var dataSourceFactory: DefaultDataSourceFactory


    @SuppressLint("WrongConstant")
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        chengeView(true)
        // INIT PLAYER
        player = ExoPlayerFactory.newSimpleInstance(this)
        dataSourceFactory = DefaultDataSourceFactory(this, Util.getUserAgent(this, "yourApplicationName"))
        pcv.player = player
        pcv.controllerHideOnTouch = false


        // SWIPE
        pcv.setOnTouchListener(object : SwipeListener(this){

            override fun onSwipeTop() {
                Log.i(TAG, "onSwipeTop")
            }

            override fun onSwipeRight() {
                Log.i(TAG, "onSwipeRight")
                if (player.hasPrevious()) player.previous()
            }

            override fun onSwipeLeft() {
                Log.i(TAG, "onSwipeLeft")
                if (player.hasNext()) player.next()
            }

            override fun onSwipeBottom() {
                Log.i(TAG, "onSwipeBottom")
                // Hide app
                val startMain = Intent(Intent.ACTION_MAIN)
                startMain.addCategory(Intent.CATEGORY_HOME)
                startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(startMain)
            }

        })


        // BUTTON CLICKS
        btn_settings.setOnClickListener {selectDir()}
        btn_add.setOnClickListener {selectDir()}
        btn_select.setOnClickListener {selectDir()}

        btn_repeat.setOnClickListener {
            btn_repeat.isSelected = !btn_repeat.isSelected
            player.repeatMode = if (btn_repeat.isSelected) 2 else 0
        }
        btn_shuffle.setOnClickListener {
            btn_shuffle.isSelected = !btn_shuffle.isSelected
            player.shuffleModeEnabled = btn_shuffle.isSelected

        }

        // PLAYER TAGS (Title, Artist)
        player.addListener(object : Player.EventListener {
            override fun onLoadingChanged(isLoading: Boolean) {
                if (!isLoading) {
                    Log.i(TAG, "onLoadingChanged")
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


    /**
     * Check, ask permissions and choose dir
     */
    private fun selectDir() {

        // Check permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)
        } else {
            val i = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)

            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            i.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            i.addCategory(Intent.CATEGORY_DEFAULT)
            //i.setType("audio/*");
            startActivityForResult(Intent.createChooser(i, "Choose directory"), DIRECTORY_REQUEST_CODE)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == DIRECTORY_REQUEST_CODE && resultCode == RESULT_OK && data != null && data.data != null) {
            Log.i(TAG, "Result URI " + data.data)

            val treeUri: Uri = data.data
            val lst = getAllAudioFromTree(treeUri)
            if (lst.size > 0) {

                var concatenatedSource = ConcatenatingMediaSource()
                for (i in lst) {
                    concatenatedSource.addMediaSource(ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(i))
                }

                player.prepare(concatenatedSource)
                chengeView(false)
            } else {
                Toast.makeText(this, "В выбранной директории нет аудио файлов", Toast.LENGTH_LONG).show()
            }

        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Permission granted!")
                selectDir()
            } else {
                Log.i(TAG, "Permission denied")
                Toast.makeText(this, "Нет прав на чтение файловой системы", Toast.LENGTH_SHORT).show()
            }
        }
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
        Log.i(TAG, "onStart")
        super.onStart()
    }
    override fun onStop() {
        Log.i(TAG, "onStop")
        super.onStop()
    }

    override fun onRestart() {
        Log.i(TAG, "onRestart")
        super.onRestart()
    }

    override fun onResume() {
        Log.i(TAG, "onResume")
        super.onResume()
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy")
        super.onDestroy()
    }

    override fun onPause() {
        Log.i(TAG, "onPause")
        super.onPause()

    }

    fun chengeView(select_view : Boolean = true) {
        if (select_view) {
            pcv.visibility = View.GONE
            sel_lay.visibility = View.VISIBLE
        } else {
            pcv.visibility = View.VISIBLE
            sel_lay.visibility = View.GONE
        }
    }
}
