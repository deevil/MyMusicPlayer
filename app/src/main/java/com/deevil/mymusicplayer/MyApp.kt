package com.deevil.mymusicplayer

import android.app.Application

class MyApp : Application() {

    companion object {

        private val TAG = "MyApp"

        private lateinit var mInstance: MyApp

        val instance: MyApp
            get() {
                if (mInstance == null) {
                    mInstance = MyApp()
                }
                return mInstance
            }
    }
}














