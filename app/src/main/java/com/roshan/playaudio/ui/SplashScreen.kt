package com.roshan.playaudio.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.roshan.playaudio.R

class SplashScreen : AppCompatActivity() {

    private val SPLASH_TIME : Long = 1000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        Handler(Looper.getMainLooper()).postDelayed(Runnable {
            val startMainActivityIntent = Intent(this@SplashScreen, MainActivity::class.java)
            startActivity(startMainActivityIntent)
            finish()
        }, SPLASH_TIME)
    }
}