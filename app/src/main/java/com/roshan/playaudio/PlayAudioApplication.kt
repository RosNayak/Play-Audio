package com.roshan.playaudio

import android.app.Application
import com.roshan.playaudio.ui.notification.ManageNotifications

class PlayAudioApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        ManageNotifications.createAudioNotificationChannel(this)
    }
}