package com.roshan.playaudio.ui

import android.app.Notification
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import android.telephony.CellInfo
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.roshan.playaudio.Constants
import com.roshan.playaudio.StorageClass
import com.roshan.playaudio.entity.AudioEntity
import com.roshan.playaudio.handler.PreferenceHandler
import com.roshan.playaudio.listeners.MediaPlayerListener
import com.roshan.playaudio.priorities.AudioPlayStatus
import com.roshan.playaudio.priorities.ServiceActions
import com.roshan.playaudio.ui.notification.ManageNotifications

class PlayAudioService : Service()
    , MediaPlayer.OnCompletionListener {

    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var binder: ComponentBinder
    private lateinit var audioList: ArrayList<AudioEntity>
    private var currentAudioIndex: Int = 0
    private val mediaPlayer = MediaPlayer()
    private var currentPlayPosition : Long? = null
    private lateinit var mediaPlayerListener: MediaPlayerListener
    private var isCallOnGoing : Boolean = false

    inner class ComponentBinder : Binder() {
        fun getService(): PlayAudioService = this@PlayAudioService
    }

    override fun onCreate() {
        Log.d("MyTest", "In onCreate")
        super.onCreate()
        binder = ComponentBinder()
        setMediaPlayerListeners()
        audioList = StorageClass.audioList!!
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        notificationBuilder = ManageNotifications.getAudioNotification(this, intent as Intent)
        currentAudioIndex = intent.getIntExtra(Constants.AUDIO_INDEX_IN_LIST, -1)
        if (currentAudioIndex != -1) PreferenceHandler.setCurrentIndex(this, currentAudioIndex)

        listenPhoneStateChanged()

        if (intent.action == ServiceActions.START_SERVICE.action) {
            if (currentAudioIndex == -1) currentAudioIndex = 0

            ManageNotifications.manageNotification(
                this,
                Constants.AUDIO_NOTIFICATION_ID,
                notificationBuilder,
                AudioPlayStatus.AUDIO_PLAYING
            )

            if (mediaPlayer.isPlaying) { mediaPlayer.reset() }

            mediaPlayer.apply {
                setDataSource(this@PlayAudioService, audioList[currentAudioIndex].uri)
                prepare()
                start()
            }

            mediaPlayerListener.audioResumedListener()

        } else if (intent.action == ServiceActions.STOP_SERVICE.action) {
            stopSelf()
        } else if (intent.action == ServiceActions.PLAY_PREVIOUS.action) {
            playPreviousAudio(true)
        } else if (intent.action == ServiceActions.PLAY_NEXT.action) {
            playNextAudio(true)
        } else if (intent.action == ServiceActions.PAUSE_OR_PLAY.action) {
            if (mediaPlayer.isPlaying) {
                pauseAudio(true)
            } else {
                playAudio(true)
            }
        }

        Log.d("MyTest", "In onStart")

        return START_REDELIVER_INTENT
    }

    override fun onBind(p0: Intent?): IBinder? {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MyTest", "On destroy called.")
        ManageNotifications.manageNotification(
            this,
            Constants.AUDIO_NOTIFICATION_ID,
            notificationBuilder,
            AudioPlayStatus.AUDIO_PLAY_STOPPED
        )
    }

    private fun setMediaPlayerListeners() {
        mediaPlayer.setOnCompletionListener(this)
    }
    private fun registerBroadcastReceiver() {
        val intentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
    }

    override fun onCompletion(mediaPlayer: MediaPlayer?) {
        currentAudioIndex = (currentAudioIndex + 1) % audioList.size
        val currentAudio = audioList[currentAudioIndex]
        val temp = currentAudio.path.split('/')
        notificationBuilder.setContentTitle(temp[temp.size - 2])
        notificationBuilder.setContentText(currentAudio.name)
        ManageNotifications.manageNotification(
            this@PlayAudioService,
            Constants.AUDIO_NOTIFICATION_ID,
            notificationBuilder,
            AudioPlayStatus.AUDIO_CHANGED
        )

        //play the next audio.
        val uri = currentAudio.uri
        mediaPlayer?.apply {
            reset()
            setDataSource(this@PlayAudioService, uri)
            prepare()
            start()
        }

        mediaPlayerListener.audioChangedListener(currentAudio.name)

        Log.d("MyTest", "Completed")
    }

    fun setMediaPlayerListener(mediaPlayerListener: MediaPlayerListener) {
        this.mediaPlayerListener = mediaPlayerListener
    }

    fun pauseAudio(fromNotification: Boolean = false) {
        if (fromNotification) {
            currentAudioIndex = PreferenceHandler.getCurrentIndex(this)
        }

        val temp = audioList[currentAudioIndex].path.split('/')
        notificationBuilder.setOngoing(false)
            .setContentText(audioList[currentAudioIndex].name)
            .setContentTitle(temp[temp.size - 2])

        ManageNotifications.manageNotification(
            this@PlayAudioService,
            Constants.AUDIO_NOTIFICATION_ID,
            notificationBuilder,
            AudioPlayStatus.AUDIO_PAUSED
        )
        currentPlayPosition = mediaPlayer.currentPosition.toLong()
        mediaPlayer.pause()
        mediaPlayerListener.audioPausedListener()
    }

    fun playAudio(fromNotification: Boolean = false) {
        if (fromNotification) {
            currentAudioIndex = PreferenceHandler.getCurrentIndex(this)
        }

        val temp = audioList[currentAudioIndex].path.split('/')
        notificationBuilder.setOngoing(true)
            .setContentText(audioList[currentAudioIndex].name)
            .setContentTitle(temp[temp.size - 2])

        ManageNotifications.manageNotification(
            this@PlayAudioService,
            Constants.AUDIO_NOTIFICATION_ID,
            notificationBuilder,
            AudioPlayStatus.AUDIO_PLAYING
        )
        mediaPlayer.start()
        mediaPlayerListener.audioResumedListener()
    }

    fun playNextAudio(fromNotification : Boolean = false) {
        if (fromNotification) {
            currentAudioIndex = (PreferenceHandler.getCurrentIndex(this) + 1) % audioList.size
            PreferenceHandler.setCurrentIndex(this, currentAudioIndex)
        }
        else {
            currentAudioIndex = (currentAudioIndex + 1) % audioList.size
            PreferenceHandler.setCurrentIndex(this, currentAudioIndex)
        }
        Log.d("MyTest", "${PreferenceHandler.getCurrentIndex(this)}")
        val uri = audioList[currentAudioIndex].uri
        val temp = audioList[currentAudioIndex].path.split('/')

        notificationBuilder.setOngoing(true)
            .setContentTitle(temp[temp.size - 2])
            .setContentText(audioList[currentAudioIndex].name)
        ManageNotifications.manageNotification(
            this@PlayAudioService,
            Constants.AUDIO_NOTIFICATION_ID,
            notificationBuilder,
            AudioPlayStatus.AUDIO_PLAYING
        )

        mediaPlayer.apply {
            reset()
            setDataSource(this@PlayAudioService, uri)
            prepare()
            start()
        }
        mediaPlayerListener.audioChangedListener(audioList[currentAudioIndex].name)
    }

    fun playPreviousAudio(fromNotification: Boolean = false) {
        if (fromNotification) {
            currentAudioIndex = PreferenceHandler.getCurrentIndex(this) - 1
            if (currentAudioIndex < 0) currentAudioIndex = audioList.size - 1
            PreferenceHandler.setCurrentIndex(this, currentAudioIndex)
        }
        else {
            currentAudioIndex -= 1
            if (currentAudioIndex < 0) currentAudioIndex = audioList.size - 1
            PreferenceHandler.setCurrentIndex(this, currentAudioIndex)
        }
        Log.d("MyTest", "${PreferenceHandler.getCurrentIndex(this)}")
        val uri = audioList[currentAudioIndex].uri
        val temp = audioList[currentAudioIndex].path.split('/')

        notificationBuilder.setOngoing(true)
            .setContentTitle(temp[temp.size - 2])
            .setContentText(audioList[currentAudioIndex].name)
        ManageNotifications.manageNotification(
            this@PlayAudioService,
            Constants.AUDIO_NOTIFICATION_ID,
            notificationBuilder,
            AudioPlayStatus.AUDIO_PLAYING
        )

        mediaPlayer.apply {
            reset()
            setDataSource(this@PlayAudioService, uri)
            prepare()
            start()
        }
        mediaPlayerListener.audioChangedListener(audioList[currentAudioIndex].name)
    }

    private fun listenPhoneStateChanged() {
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val phoneStateListener = UserPhoneStateListener()
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    inner class UserPhoneStateListener : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            when (state) {
                TelephonyManager.CALL_STATE_RINGING -> {
                    Log.d("MyTest", "Phone ringing")
                    pauseAudio()
                    isCallOnGoing = true
                }
                TelephonyManager.CALL_STATE_IDLE -> {
                    Log.d("MyTest", "Phone idle")
                    if (isCallOnGoing) {
                        playAudio()
                        isCallOnGoing = false
                    }
                }
            }
        }
    }

}