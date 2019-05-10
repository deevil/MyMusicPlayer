package com.deevil.mymusicplayer

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_main.*
import android.provider.MediaStore
import androidx.core.net.toUri


class MainActivity : AppCompatActivity() {

    var tracks:ArrayList<Track> = ArrayList<Track>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        var ta:TrackAdapter = TrackAdapter(context = this, tracks = tracks, clickListener = {
            Log.w("TEST", it.artist)
        })



        tracks.add(Track("aaa", "bbb",1, "/faaf.aaa".toUri(), 1))
        tracks.add(Track("aaa", "bbb",1, "/faaf.aaa".toUri(), 1))
        tracks.add(Track("aaa", "bbb",1, "/faaf.aaa".toUri(), 1))
        tracks.add(Track("aaa", "bbb",1, "/faaf.aaa".toUri(), 1))
        //rvTrackList

        ta.notifyDataSetChanged()
    }

}
