package com.deevil.mymusicplayer

import android.net.Uri

internal class MusicRepository {

    private val data = arrayOf(
        Track(
            "Triangle",
            "Jason Shaw",
            null,
            Uri.parse("http://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Kangaroo_MusiQue_-_The_Neverwritten_Role_Playing_Game.mp3"),
            ((3 * 60 + 41) * 1000).toLong()
        ),
        Track(
            "Rubix Cube",
            "Jason Shaw",
            null,
            Uri.parse("http://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__nbsp_.mp3"),
            ((3 * 60 + 44) * 1000).toLong()
        ),
        Track(
            "MC Ballad S Early Eighties",
            "Frank Nora",
            null,
            Uri.parse("http://commondatastorage.googleapis.com/codeskulptor-assets/Epoq-Lepidoptera.ogg"),
            ((2 * 60 + 50) * 1000).toLong()
        ),
        Track(
            "Folk Song",
            "Brian Boyko",
            null,
            Uri.parse("http://commondatastorage.googleapis.com/codeskulptor-assets/week7-button.m4a"),
            ((3 * 60 + 5) * 1000).toLong()
        ),
        Track(
            "Morning Snowflake",
            "Kevin MacLeod",
            null,
            Uri.parse("http://commondatastorage.googleapis.com/codeskulptor-assets/Collision8-Bit.ogg"),
            ((2 * 60 + 0) * 1000).toLong()
        )
    )

    private val maxIndex = data.size - 1
    private var currentItemIndex = 0

    val next: Track
        get() {
            if (currentItemIndex == maxIndex)
                currentItemIndex = 0
            else
                currentItemIndex++
            return current
        }

    val previous: Track
        get() {
            if (currentItemIndex == 0)
                currentItemIndex = maxIndex
            else
                currentItemIndex--
            return current
        }

    val current: Track
        get() = data[currentItemIndex]

    internal class Track(
        val title: String, val artist: String, val bitmapResId: Int?, val uri: Uri, val duration: Long // in ms
    )
}
