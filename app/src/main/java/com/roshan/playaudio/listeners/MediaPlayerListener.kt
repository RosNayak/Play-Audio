package com.roshan.playaudio.listeners

interface MediaPlayerListener {
    fun audioChangedListener(name : String)
    fun audioPausedListener()
    fun audioResumedListener()
}