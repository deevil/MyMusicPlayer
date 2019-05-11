package com.deevil.mymusicplayer

import android.net.Uri


class Track(
    val title: String, val artist: String, val bitmapResId: Int?, val uri: Uri, val duration: Long, val haveEmbeddedCover:Boolean = false, val havePrimaryCover:Boolean = false) {

    //fun getUri():Uri = this.uri
}