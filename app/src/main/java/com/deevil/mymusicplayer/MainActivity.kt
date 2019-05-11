package com.deevil.mymusicplayer

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_main.*
import android.provider.MediaStore
import androidx.core.net.toUri
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager




class MainActivity : AppCompatActivity() {

    var tracks:ArrayList<Track> = ArrayList<Track>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        var ta:TrackAdapter = TrackAdapter(context = this, tracks = tracks, clickListener = {
            Log.w("TEST", it.artist)
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




        //R.id.a







        tracks.add(Track("a1", "b1",1, "/faaf.aaa".toUri(), 1))
        tracks.add(Track("a2", "b2",1, "/faaf.aaa".toUri(), 1))
        tracks.add(Track("a3", "b3",1, "/faaf.aaa".toUri(), 1))
        tracks.add(Track("a4", "b4",1, "/faaf.aaa".toUri(), 1))

        ta.notifyDataSetChanged()
    }

}
