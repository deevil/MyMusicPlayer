package com.deevil.mymusicplayer

import android.net.Uri

internal class MusicRepository {

    private var data = arrayOf(
        Track(
            "Triangle",
            "Jason Shaw",
            null,//R.drawable.image266680,
            Uri.parse("https://freepd.com/Ballad/Triangle.mp3"),
            ((3 * 60 + 41) * 1000).toLong()
        ),
        Track(
            "Rubix Cube",
            "Jason Shaw",
            null,//R.drawable.image396168,
            Uri.parse("https://freepd.com/Ballad/Rubix Cube.mp3"),
            ((3 * 60 + 44) * 1000).toLong()
        ),
        Track(
            "MC Ballad S Early Eighties",
            "Frank Nora",
            null,//R.drawable.image533998,
            Uri.parse("https://freepd.com/Ballad/MC Ballad S Early Eighties.mp3"),
            ((2 * 60 + 50) * 1000).toLong()
        ),
        Track(
            "Folk Song",
            "Brian Boyko",
            null,//R.drawable.image544064,
            Uri.parse("https://freepd.com/Acoustic/Folk Song.mp3"),
            ((3 * 60 + 5) * 1000).toLong()
        ),
        Track(
            "Morning Snowflake",
            "Kevin MacLeod",
            null,//R.drawable.image208815
            Uri.parse("https://freepd.com/Acoustic/Morning Snowflake.mp3"),
            ((2 * 60 + 0) * 1000).toLong()
        )
    )

    private var maxIndex = data.size - 1
    private var currentItemIndex = 0

    fun getNext(): Track {
        if (currentItemIndex == maxIndex)
            currentItemIndex = 0
        else
            currentItemIndex++
        return getCurrent()
    }

    fun getPrevious(): Track {
        if (currentItemIndex == 0)
            currentItemIndex = maxIndex
        else
            currentItemIndex--
        return getCurrent()

    }
    fun getCurrent(): Track  = data[currentItemIndex]

}