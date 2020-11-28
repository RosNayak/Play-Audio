package com.roshan.playaudio.listeners

import com.roshan.playaudio.entity.AudioEntity

interface AudioSelectedListener {
    fun onAudioSelected(audio : AudioEntity)
}