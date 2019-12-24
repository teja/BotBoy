package com.example.botboy

import androidx.appcompat.app.AppCompatActivity

import android.os.Bundle

class CameraActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        savedInstanceState ?: supportFragmentManager.beginTransaction()
            .replace(R.id.container, CameraConnectionFragment.newInstance())
            .commit()
    }
}