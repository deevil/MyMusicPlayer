package com.deevil.mymusicplayer

import android.content.Context
import android.icu.util.TimeUnit
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.track_row.view.*

class TrackAdapter(var tracks: ArrayList<Track>, val context: Context, val clickListener: (Track) -> Unit ):RecyclerView.Adapter<TrackAdapter.TrackHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackHolder = TrackHolder(LayoutInflater.from(parent.context).inflate(R.layout.track_row, parent, false))

    override fun onBindViewHolder(holder: TrackHolder, position: Int) = holder.bind(tracks[position], position, clickListener)


    override fun getItemCount(): Int = tracks.size


    class TrackHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(track: Track, position:Int, clickListener: (Track) -> Unit) {
            itemView.tvTrackName.text = track.title
            itemView.tvArtist.text = track.artist
            itemView.tvDuration.text = String.format("%02d:%02d", track.duration/ 1000 / 60 , track.duration / 1000 % 60)
            itemView.setOnClickListener{ clickListener(track) }
        }
    }
}

