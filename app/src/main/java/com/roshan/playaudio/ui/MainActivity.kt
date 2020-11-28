package com.roshan.playaudio.ui

import android.Manifest
import android.app.Service
import android.content.*
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.provider.Settings
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.roshan.playaudio.Constants
import com.roshan.playaudio.R
import com.roshan.playaudio.StorageClass
import com.roshan.playaudio.entity.AudioEntity
import com.roshan.playaudio.handler.PreferenceHandler
import com.roshan.playaudio.listeners.AudioSelectedListener
import com.roshan.playaudio.listeners.MediaPlayerListener
import com.roshan.playaudio.listeners.ReadPermissionDialogButtonClickedListener
import com.roshan.playaudio.priorities.ServiceActions
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.view.*
import kotlinx.android.synthetic.main.display_audio_view_layout.*
import kotlinx.android.synthetic.main.display_audio_view_layout.view.*


class MainActivity : AppCompatActivity()
    , ReadPermissionDialogButtonClickedListener
    , AudioSelectedListener
    , MediaPlayerListener {

    private lateinit var bottomSheetBehavior : BottomSheetBehavior<*>
    private val READ_PERMISSION_REQUEST_CODE : Int = 100
    private val audioFileList : ArrayList<AudioEntity> = ArrayList()
    private lateinit var serviceConnection : ServiceConnection
    private lateinit var service : PlayAudioService
    private var isBound : Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = layoutInflater.inflate(R.layout.activity_main, null)
        setContentView(view)
        setViewChangListener(view)
        checkForStoragePermissions()
        setServiceConnection()

        val startAudioServiceIntent = Intent(this, PlayAudioService::class.java)
        bindService(startAudioServiceIntent, serviceConnection, BIND_AUTO_CREATE)
    }

    override fun onStart() {
        super.onStart()
        setUpBottomNavView()
    }

    private fun setUpBottomNavView() {
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        setSupportActionBar(dummyToolBar)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        bottomSheet.topLayout.ivControlBottomSheet.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.up_nav))
        bottomSheet.topLayout.tvSongTitle.text = "Play a audio"
        bottomSheet.topLayout.background = ContextCompat.getDrawable(this, R.color.bottomSheetCollapsedColor)

        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                if (slideOffset >= 0.5) {
                    bottomSheet.topLayout.ivControlBottomSheet.setImageDrawable(ContextCompat.getDrawable(this@MainActivity, R.drawable.down_nav))
                    bottomSheet.topLayout.background = ContextCompat.getDrawable(this@MainActivity, R.color.black)
                    bottomSheet.topLayout.ivControlMusicPlay.visibility = View.INVISIBLE
                } else {
                    bottomSheet.topLayout.ivControlBottomSheet.setImageDrawable(ContextCompat.getDrawable(this@MainActivity, R.drawable.up_nav))
                    bottomSheet.topLayout.background = ContextCompat.getDrawable(this@MainActivity, R.color.bottomSheetCollapsedColor)
                    bottomSheet.topLayout.ivControlMusicPlay.visibility = View.VISIBLE
                }
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {}

        })

        bottomSheet.topLayout.ivControlBottomSheet.setOnClickListener {
            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            } else if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED){
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }

        bottomSheet.topLayout.ivControlMusicPlay.setOnClickListener {
            if (bottomSheet.topLayout.ivControlMusicPlay.contentDescription
                == getString(R.string.paused)
            ) {
                service.playAudio()
            } else {
                service.pauseAudio()
            }
        }

        bottomSheet.imageViewSkipPrevious.setOnClickListener { service.playPreviousAudio() }

        bottomSheet.imageViewSkipNext.setOnClickListener { service.playNextAudio() }

        bottomSheet.imageViewControlMusic.setOnClickListener {
            if (bottomSheet.topLayout.ivControlMusicPlay.contentDescription
                == getString(R.string.paused)
            ) {
                service.playAudio()
            } else {
                service.pauseAudio()
            }
        }
    }

    private fun setServiceConnection() {
        serviceConnection = object : ServiceConnection {
            override fun onServiceDisconnected(p0: ComponentName?) {
                Log.d("MyTest", "disconnected.")
            }

            override fun onServiceConnected(p0: ComponentName?, binder: IBinder?) {
                Log.d("MyTest", "connected.")
                service = (binder as PlayAudioService.ComponentBinder).getService()
                service.setMediaPlayerListener(this@MainActivity)
            }
        }
    }

    private fun setViewChangListener(view : View) {
        val viewTreeObserver = view.viewTreeObserver
        if (viewTreeObserver.isAlive) {
            viewTreeObserver.addOnGlobalLayoutListener {
                bottomSheetBehavior.peekHeight = bottomSheet.topLayout.height
                val layoutParams = viewFreeSpace.layoutParams
                layoutParams.height = bottomSheet.topLayout.height
                viewFreeSpace.layoutParams = layoutParams
            }
        }
    }

    private fun checkForStoragePermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
         != PackageManager.PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                && !PreferenceHandler.isFirstTimePermission(this)) {

            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    READ_PERMISSION_REQUEST_CODE
                )
            }
        } else {
            val data = getAllAudioFiles()
            setUpAudioList(data)
        }
    }

    private fun getAllAudioFiles() : ArrayList<AudioEntity> {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            "_data"
        )

        val sortOrder = "${MediaStore.Audio.Media.DISPLAY_NAME} ASC"

        val cursor = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )

        cursor.use { cursor ->
            if (cursor != null) {
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val pathColumn = cursor.getColumnIndexOrThrow("_data")

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    val path = cursor.getString(pathColumn)

                    val pathSlices = path.split('/') as MutableList<*>
                    pathSlices.removeAt(0)
                    if (pathSlices.size == 4 ||
                        pathSlices.contains("WhatsApp Audio") ||
                        pathSlices.contains("Download")) {
                        audioFileList.add(AudioEntity(uri, path, id, name))
                    }
                }
            }
        }

        StorageClass.audioList = audioFileList

        return audioFileList
    }

    private fun setUpAudioList(data : ArrayList<AudioEntity>) {
        val layoutManager = LinearLayoutManager(this)
        val adapter = AudioRecyclerView(this, data, this)
        audioList.adapter = adapter
        audioList.layoutManager = layoutManager
    }

    override fun onBackPressed() {
        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
            finish()
        } else {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    override fun positiveButtonClicked() {
        val permissionSettingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        permissionSettingsIntent.data = uri
        startActivityForResult(permissionSettingsIntent, READ_PERMISSION_REQUEST_CODE)
    }

    override fun negativeButtonClicked() {
        Toast.makeText(this, "Permission is required.", Toast.LENGTH_LONG).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            READ_PERMISSION_REQUEST_CODE -> {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                 == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission granted.", Toast.LENGTH_SHORT).show()
                    getAllAudioFiles()
                } else {
                    Toast.makeText(this, "Permission is required.", Toast.LENGTH_LONG).show()
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            READ_PERMISSION_REQUEST_CODE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission granted.", Toast.LENGTH_SHORT).show()
                    val data = getAllAudioFiles()
                    setUpAudioList(data)
                } else {
                    Toast.makeText(this, "Permission is required.", Toast.LENGTH_LONG).show()
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onAudioSelected(audio: AudioEntity) {
        val startAudioServiceIntent = Intent(this, PlayAudioService::class.java)
        startAudioServiceIntent.action = ServiceActions.START_SERVICE.action
        startAudioServiceIntent.putExtra(
            Constants.AUDIO_INDEX_IN_LIST,
            StorageClass.getAudioIndex(audio)
        )
        startAudioServiceIntent.putExtra(Constants.AUDIO_PLAYED, audio.name)
        val temp = audio.path.split('/')
        startAudioServiceIntent.putExtra(Constants.AUDIO_FILE, temp[temp.size - 2])

        bottomSheet.topLayout.tvSongTitle.text = audio.name
        bottomSheet.tvMusicTitle.text = audio.name
        
        startService(startAudioServiceIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindAndStopService()
    }

    private fun unbindAndStopService() {
        val stopAudioServiceIntent = Intent(this, PlayAudioService::class.java)
        unbindService(serviceConnection)
        stopService(stopAudioServiceIntent)
    }

    override fun audioChangedListener(name: String) {
        bottomSheet.tvMusicTitle.text = name
        bottomSheet.topLayout.tvSongTitle.text = name
        audioResumedListener()
    }

    override fun audioPausedListener() {
        bottomSheet.topLayout.ivControlMusicPlay.contentDescription = "paused"
        bottomSheet.imageViewControlMusic.contentDescription = "paused"
        bottomSheet.topLayout.ivControlMusicPlay.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.play_icon))
        bottomSheet.imageViewControlMusic.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.play_icon))
    }

    override fun audioResumedListener() {
        bottomSheet.topLayout.ivControlMusicPlay.contentDescription = "play"
        bottomSheet.imageViewControlMusic.contentDescription = "play"
        bottomSheet.topLayout.ivControlMusicPlay.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pause))
        bottomSheet.imageViewControlMusic.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pause))
    }

}