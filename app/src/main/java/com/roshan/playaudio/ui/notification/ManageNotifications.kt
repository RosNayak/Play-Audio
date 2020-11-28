package com.roshan.playaudio.ui.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import com.roshan.playaudio.Constants
import com.roshan.playaudio.R
import com.roshan.playaudio.priorities.AudioPlayStatus
import com.roshan.playaudio.priorities.ServiceActions
import com.roshan.playaudio.ui.MainActivity
import com.roshan.playaudio.ui.PlayAudioService

object ManageNotifications {

    fun createAudioNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationId : String = Constants.AUDIO_NOTIFICATION_CHANNEL_ID
            val channelName : String = context.resources.getString(R.string.audioChannelName)
            val descriptionText : String = context.resources.getString(R.string.audioChannelDescriptionText)
            val channelImportance : Int = NotificationManager.IMPORTANCE_DEFAULT

            val notificationChannel = NotificationChannel(notificationId, channelName, channelImportance)
            notificationChannel.description = descriptionText

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    fun getAudioNotification(context: Context, intent: Intent) : NotificationCompat.Builder {

        val audio = intent.getStringExtra(Constants.AUDIO_PLAYED)
        val file = intent.getStringExtra(Constants.AUDIO_FILE)

        val startActivityIntent = Intent(context, MainActivity::class.java)
        val openActivityIntent = PendingIntent.getActivity(context, 1, startActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val playPrevIntent = Intent(context, PlayAudioService::class.java)
        playPrevIntent.action = ServiceActions.PLAY_PREVIOUS.action
        val pendingIntent1 = PendingIntent.getService(context, 2, playPrevIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val playNextIntent = Intent(context, PlayAudioService::class.java)
        playNextIntent.action = ServiceActions.PLAY_NEXT.action
        val pendingIntent2 = PendingIntent.getService(context, 3, playNextIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val pauseOrPlayIntent = Intent(context, PlayAudioService::class.java)
        pauseOrPlayIntent.action = ServiceActions.PAUSE_OR_PLAY.action
        val pendingIntent3 = PendingIntent.getService(context, 4, pauseOrPlayIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val spotifyBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.spotify)

        val notification = NotificationCompat.Builder(context, Constants.AUDIO_NOTIFICATION_CHANNEL_ID)
        notification.setSmallIcon(R.drawable.headphone)
            .setContentTitle(file)
            .setContentText(audio)
            .setLargeIcon(spotifyBitmap)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0, 1, 2) /* prev pause/play next */
                .setMediaSession(MediaSessionCompat(context, "Media session").sessionToken))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openActivityIntent)
            .addAction(R.drawable.skip_prev_black, "Play previous", pendingIntent1)
            .addAction(R.drawable.pause_black, "Pause audio", pendingIntent3)
            .addAction(R.drawable.skip_next_black, "Play next", pendingIntent2)
            .setChannelId(Constants.AUDIO_NOTIFICATION_CHANNEL_ID)
            .setAutoCancel(false)  //don't dismiss when clicked on notification.
            .setOngoing(true)  //prevent user from cancelling the notification.

        return notification

    }

    fun manageNotification(
        context: Context,
        notificationId: Int,
        builder: NotificationCompat.Builder,
        audioStatus : AudioPlayStatus
    ) {
        when (audioStatus) {
            AudioPlayStatus.AUDIO_PLAYING -> showNotification(context, notificationId, builder)
            AudioPlayStatus.AUDIO_PAUSED -> showNotification(context, notificationId, builder)
            AudioPlayStatus.AUDIO_CHANGED -> showNotification(context, notificationId, builder)
            AudioPlayStatus.AUDIO_PLAY_STOPPED -> dismissNotification(context, notificationId)
        }
    }

    private fun showNotification(context: Context, notificationId: Int, builder: NotificationCompat.Builder) {
        when (notificationId) {
            Constants.AUDIO_NOTIFICATION_ID -> {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(Constants.AUDIO_NOTIFICATION_ID, builder.build())
            }
        }
    }

    private fun dismissNotification(context: Context, notificationId : Int) {
        when (notificationId) {
            Constants.AUDIO_NOTIFICATION_ID -> {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(Constants.AUDIO_NOTIFICATION_ID)
            }
        }
    }

}