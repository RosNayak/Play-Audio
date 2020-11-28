package com.roshan.playaudio.handler

import android.content.Context

object PreferenceHandler {

    const val FIRST_TIME_PERMISSION : String = "First time permission"
    const val PREFERENCE_NAME : String = "My Preference"
    const val CURRENT_AUDIO_INDEX : String = "Current index"

    fun isFirstTimePermission(context : Context) : Boolean {
        val sharedPreferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean(FIRST_TIME_PERMISSION, true)
    }

    fun setIsFirstTimePermission(context: Context) {
        val editor = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE).edit()
        editor.putBoolean(FIRST_TIME_PERMISSION, false).apply()
    }

    fun getCurrentIndex(context: Context) : Int {
        val sharedPreferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getInt(CURRENT_AUDIO_INDEX, 0)
    }

    fun setCurrentIndex(context: Context, index : Int) {
        val editor = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE).edit()
        editor.putInt(CURRENT_AUDIO_INDEX, index).apply()
    }
}