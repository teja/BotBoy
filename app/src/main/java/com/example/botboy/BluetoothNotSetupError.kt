package com.example.botboy

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity

import kotlinx.android.synthetic.main.activity_bluetooth_not_setup_error.*

class BluetoothNotSetupError : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth_not_setup_error)
        setSupportActionBar(toolbar)
    }

}
