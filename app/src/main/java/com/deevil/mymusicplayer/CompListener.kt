package com.deevil.mymusicplayer

import android.util.Log
import android.view.View
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.TimeBar
import com.google.android.exoplayer2.util.RepeatModeUtil
import com.google.android.exoplayer2.util.Util

class CompListener : Player.EventListener, View.OnClickListener {
    override fun onClick(v: View?) {
    }


    override fun onRepeatModeChanged(repeatMode: Int) {
//        if (!isVisible() || !isAttachedToWindow || repeatToggleButton == null) {
//            return
//        }
//        if (repeatToggleModes == RepeatModeUtil.REPEAT_TOGGLE_MODE_NONE) {
//            repeatToggleButton.setVisibility(View.GONE)
//            return
//        }
//        if (player == null) {
//            setButtonEnabled(false, repeatToggleButton)
//            return
//        }
//        setButtonEnabled(true, repeatToggleButton)
//        when (player!!.getRepeatMode()) {
//            Player.REPEAT_MODE_OFF -> {
//                repeatToggleButton.setImageDrawable(repeatOffButtonDrawable)
//                repeatToggleButton.setContentDescription(repeatOffButtonContentDescription)
//            }
//            Player.REPEAT_MODE_ONE -> {
//                repeatToggleButton.setImageDrawable(repeatOneButtonDrawable)
//                repeatToggleButton.setContentDescription(repeatOneButtonContentDescription)
//            }
//            Player.REPEAT_MODE_ALL -> {
//                repeatToggleButton.setImageDrawable(repeatAllButtonDrawable)
//                repeatToggleButton.setContentDescription(repeatAllButtonContentDescription)
//            }
//        }// Never happens.
//        repeatToggleButton.setVisibility(View.VISIBLE)
        Log.w("DEBUG", "AAAAA")
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        //updateShuffleButton()
        //updateNavigation()
        Log.w("DEBUG", "BBBB")
    }

    override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {

        Log.w("DEBUG", "CCCC")
        //super.onTracksChanged(trackGroups, trackSelections)
    }


}