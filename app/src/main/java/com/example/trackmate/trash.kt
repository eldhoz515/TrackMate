package com.example.trackmate

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class trash : AppCompatActivity() {
    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trash)
//        Utils.print(BluetoothAdapter.getDefaultAdapter().address.subSequence(5,10))
        checkPermissionsWrapper()
    }

    private fun checkPermissions(permissions: Array<out String>) {
        var flag = 0
        for (x in permissions) {
            if (ContextCompat.checkSelfPermission(this, x)
                != PackageManager.PERMISSION_GRANTED
            )
                flag = 1
        }
        if (flag == 1) {
            Utils.print("requesting permissions")
            ActivityCompat.requestPermissions(this, permissions, 1)
        } else {
            discoverDevices()
        }
    }

    private fun checkPermissionsWrapper() {
        Utils.print("checking permissions")
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
            checkPermissions(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            checkPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1 -> {
                var flag = 0
                for (x in grantResults) {
                    if (x != PackageManager.PERMISSION_GRANTED)
                        flag = 1
                }
                if (flag != 1) {
                    discoverDevices()
                } else {
                    Utils.print("Permissions not granted")
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun discoverDevices() {
        Utils.print("discoverDevices()")
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (bluetoothAdapter.isEnabled && (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
            ))
        ) {
            val discover = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    when (intent.action) {
                        BluetoothDevice.ACTION_FOUND -> {
                            Utils.print("device found")
                            val device: BluetoothDevice? =
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                            if (device != null) {
                                Utils.print("Device found: ${device.name} (${device.address})")
                            }
                        }
                        BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                            Utils.print("discovery started")
                        }
                        BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                            Utils.print("discovery stopped")
                        }
                    }
                }
            }
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            }
            registerReceiver(discover, filter)
            bluetoothAdapter.startDiscovery()
        } else {
            Utils.print("Services not enabled")
        }
    }
}