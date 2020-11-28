package com.roshan.playaudio

import com.roshan.playaudio.entity.AudioEntity

//anything to be stored for the lifetime of the app.
object StorageClass {
    var audioList : ArrayList<AudioEntity>? = null

    //helper functions.
    fun getAudioIndex(audioEntity: AudioEntity) : Int = audioList!!.indexOf(audioEntity)

}