package com.example.botboy

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Bundle
import android.util.Log
import android.widget.SeekBar
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity

import kotlinx.android.synthetic.main.activity_device_handler.*
import kotlinx.android.synthetic.main.content_device_handler.*
import java.io.OutputStream
import java.lang.Exception

class DeviceHandlerActivity : AppCompatActivity(), SeekBar.OnSeekBarChangeListener {
    var deviceName : String? = null
    var deviceAddress : String? = null
    var socket : BluetoothSocket? = null
    var oStream : OutputStream? = null
    private fun initOutputStream(dev : BluetoothDevice?) {
        if (dev == null) {
            Log.i("Teja", "No device passed")
            return
        }
        if (dev.uuids.size == 0) {
            Log.i("Teja", "No UUIDS!");
            return
        }
        socket = dev.createRfcommSocketToServiceRecord(dev!!.uuids[0].uuid);
        if (socket == null) {
            Log.i("Teja", "Socket null")
            return;
        }
        try {
            socket!!.connect()
            Log.i("Teja", "Connection established")
        } catch (e : Exception) {
            Log.i("Teja", e.toString())
            Toast.makeText(this, "Connection to device failed.\n Make sure that the device is on", Toast.LENGTH_LONG).show()
            return
        }
        oStream = socket!!.outputStream
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_handler)
        setSupportActionBar(toolbar)
        if (intent.extras == null) {
            Toast.makeText(this, "Nothing to show", Toast.LENGTH_LONG).show()
            return
        }
        var dev = intent.extras!!.getParcelable<BluetoothDevice>("btdevice")
        initOutputStream(dev)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        goleft.setOnClickListener({sendStr("1");})
        goright.setOnClickListener({sendStr("0");})
        goforward.setOnClickListener({sendStr("2");})
        goback.setOnClickListener({sendStr("3");})
        stop.setOnClickListener({sendStr("4")})
    }

    fun sendStr( v : String) {
        if (oStream == null) {
            Log.i("Teja", "Message send request received but device not ready")
            return
        }
        oStream?.write(v.toByteArray(Charsets.UTF_8))
    }

    fun send( v : Int) {
        if (oStream == null) {
            Log.i("Teja", "Message send request received but device not ready")
            return
        }
        oStream?.write(v)
        oStream?.flush()
    }


    override fun onDestroy() {
        super.onDestroy()
        oStream?.flush()
        oStream?.close()
        socket?.close()
    }


    // Begin: Seekbar listener

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        sendStr("5");
        sendStr(progress.toString());
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
    override fun onStopTrackingTouch(seekBar: SeekBar?) {}

    // End: Seekbar listener
}
