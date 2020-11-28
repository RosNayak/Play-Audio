package com.roshan.playaudio.priorities

enum class ServiceActions(val action : String) {
    START_SERVICE("start service"),
    STOP_SERVICE("stop service"),
    PLAY_PREVIOUS("play previous"),
    PLAY_NEXT("play next"),
    PAUSE_OR_PLAY("pause or play")
}