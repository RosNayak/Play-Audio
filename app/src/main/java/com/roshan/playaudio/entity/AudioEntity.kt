package com.roshan.playaudio.entity

import android.net.Uri

data class AudioEntity(
    var uri : Uri,
    var path : String,
    var id : Long,
    var name : String
)