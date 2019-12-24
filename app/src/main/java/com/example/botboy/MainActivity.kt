package com.example.botboy

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.AssetFileDescriptor
import android.os.Bundle
import android.util.Log
import android.view.*
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

import org.tensorflow.lite.Interpreter

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.w3c.dom.Text
import java.io.File
import java.io.FileInputStream
import java.lang.Exception
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class DeviceAdapter(context : Context, list: ArrayList<BluetoothDevice>) :  ArrayAdapter<BluetoothDevice>(context, 0, list) {
    var devList : ArrayList<BluetoothDevice>
    var vi : LayoutInflater
    init {
        this.devList = list
        this.vi = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val dev = getItem(position)
        if (convertView == null) {
            val tv2 = Button(context)
            tv2.text = dev?.name
            tv2.setOnClickListener({
                var intent = Intent(context, DeviceHandlerActivity::class.java)
                intent.putExtra("btdevice", dev)
                ContextCompat.startActivity(context, intent, null)
            })
            return tv2
        }
        (convertView as TextView).text = dev?.name
        return convertView
    }
}

class MainActivity : AppCompatActivity() {
    private val REQUEST_ENABLE_BT = 101;

    private var adapter : BluetoothAdapter? = null;
    var devices : ArrayList<BluetoothDevice> = ArrayList<BluetoothDevice>()

    var tflite : Interpreter? = null
    fun showDevice(it : BluetoothDevice) {
        Log.i("Teja", "Adding")
        if (it.name == "") {
            return
        }
        Log.i("Teja", it.name)
        devices.add(it)
    }

    fun showPairedDevices() {
        adapter?.bondedDevices?.forEach {
            if (it.name != null && it.name != "") showDevice(it)
        }
        searchnets.isClickable = true
    }

    fun initBluetooth() {
        adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            val intent = Intent(this, BluetoothNotSetupError::class.java)
            startActivity(intent)
        }
        searchnets.isClickable = false
        if (!adapter!!.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            searchnets.text = "Initializing bluetooth..."
        } else {
            showPairedDevices()
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            val action : String? = intent!!.action
            if (action == null || intent == null) {
                return
            }
            when(action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? =
                        intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    if (device != null) {
                        showDevice(device)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        deviceListView.setAdapter(DeviceAdapter(this, devices))
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter)
        searchnets.setOnClickListener {
            if (!adapter!!.startDiscovery()) {
                Log.i("Teja", "Error starting discovery")
            }
        }
        startCamera.setOnClickListener {
            var intent = Intent(this, CameraActivity::class.java)
            ContextCompat.startActivity(this, intent, null)
        }

        // Initializes by showing list of paired devices and then enables scan button to search
        // for more devices.
        // initBluetooth()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                showPairedDevices()
            } else {
                Toast.makeText(this, "Can't enable this application without bluetooth", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}
