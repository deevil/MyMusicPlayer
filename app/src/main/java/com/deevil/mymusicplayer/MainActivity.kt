package com.deevil.mymusicplayer

import android.Manifest
import android.annotation.SuppressLint
import android.content.CursorLoader
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import android.provider.MediaStore
import androidx.core.net.toUri
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.database.Cursor
import android.media.MediaMetadataRetriever
import java.io.IOException
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util


class MainActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 1
    private val TAG = "DBG"

    lateinit var ta: TrackAdapter
    lateinit var player: SimpleExoPlayer
    lateinit var dataSourceFactory: DefaultDataSourceFactory
    var tracks: ArrayList<Track> = ArrayList<Track>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        player = ExoPlayerFactory.newSimpleInstance(this)
        dataSourceFactory = DefaultDataSourceFactory(this, Util.getUserAgent(this, "yourApplicationName"))


        ta = TrackAdapter(context = this, tracks = tracks, clickListener = {
            Log.w("TEST", it.artist)
            Log.w("TEST", it.uri.toString())

            player.playWhenReady = true
            //if (player.)
//
//            player.prepare(ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(it.uri))
//            player.playWhenReady = true
        })


        //rvTrackList

        rvTrackList.setAdapter(ta)
        val linearLayoutManager = LinearLayoutManager(this)
        val dividerItemDecoration = DividerItemDecoration(
            rvTrackList.getContext(),
            linearLayoutManager.orientation
        )
        rvTrackList.setLayoutManager(linearLayoutManager)
        rvTrackList.addItemDecoration(dividerItemDecoration)


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


        //R.id.a


        tracks.add(Track("a1", "b1", 1, "/faaf.aaa".toUri(), 1))
        tracks.add(Track("a2", "b2", 1, "/faaf.aaa".toUri(), 1))
        tracks.add(Track("a3", "b3", 1, "/faaf.aaa".toUri(), 1))
        tracks.add(Track("a4", "b4", 1, "/faaf.aaa".toUri(), 1))

        ta.notifyDataSetChanged()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    @SuppressLint("MissingSuperCall")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 9999 && resultCode == RESULT_OK && data != null && data.data != null) {
            Log.i(TAG, "Result URI " + data.data)


            val treeUri: Uri = data.data
            var lst = getAllAudioFromTree(treeUri)
            if (lst.size > 0) {
                tracks.clear()



                for (i in lst) {
                    val mmr = MediaMetadataRetriever()
                    mmr.setDataSource(this, i)

                    val title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    val artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    val duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toLong()
                    val haveEmbeddedCover = try {
                        (mmr.embeddedPicture != null)
                    } catch (e: java.lang.Exception) {
                        false
                    }
                    val havePrimaryCover = try {
                        (mmr.getPrimaryImage() != null)
                    } catch (e: java.lang.Exception) {
                        false
                    }

                    tracks.add(Track(title, artist, 1, i, duration, haveEmbeddedCover, havePrimaryCover))
                }


                var concatenatedSource = ConcatenatingMediaSource()
                for (i in tracks) {
                    concatenatedSource.addMediaSource(ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(i.uri))
                }

                player.prepare(concatenatedSource)
                player.playWhenReady = true

                ta.notifyDataSetChanged()

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
