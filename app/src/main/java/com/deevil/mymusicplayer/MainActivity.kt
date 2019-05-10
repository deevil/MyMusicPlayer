package com.deevil.mymusicplayer

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    var tracks:ArrayList<Track> = ArrayList<Track>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        var ta:TrackAdapter = TrackAdapter(context = this, tracks = tracks, clickListener = {
            Log.w("TEST", it.artist)
        })

        //rvTrackList


    }

}
