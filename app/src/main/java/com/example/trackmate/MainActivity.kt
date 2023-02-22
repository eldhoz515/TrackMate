package com.example.trackmate

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.*
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricManager.Authenticators
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CancellationSignal
import android.util.Log
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        checkPermissionsWrapper()
        val button = findViewById<Button>(R.id.button)
        button.setOnClickListener { checkPermissionsWrapper() }
    }

    private fun print(msg: String) {
        Log.d("tm", msg)
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
            print("requesting permissions")
            ActivityCompat.requestPermissions(this, permissions, 1)
        } else {
            discoverDevices()
        }
    }

    private fun checkPermissionsWrapper() {
        print("checking permissions")
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
                    print("Permissions not granted")
                }
            }
        }
    }


    @SuppressLint("MissingPermission")
    private fun discoverDevices() {
        print("discoverDevices()")
        authenticate()
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter.isEnabled) {
            val discover = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    when (intent.action) {
                        BluetoothDevice.ACTION_FOUND -> {
                            print("device found")
                            val device: BluetoothDevice? =
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                            if (device != null) {
                                print("Device found: ${device.name} (${device.address})")
                            }
                        }
                        BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                            print("discovery started")
                        }
                        BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                            print("discovery stopped")
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
//            bluetoothAdapter.startDiscovery()
        }
    }

    private fun getCancellationSignal(): CancellationSignal {
        var cancellationSignal: CancellationSignal? = null
        cancellationSignal = CancellationSignal()
        cancellationSignal.setOnCancelListener {
            print("Authentication was Cancelled by the user")
        }
        return cancellationSignal as CancellationSignal
    }

    private val authenticationCallback: BiometricPrompt.AuthenticationCallback
        get() = @RequiresApi(Build.VERSION_CODES.P)
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                super.onAuthenticationError(errorCode, errString)
                print("Authentication Error : $errString")
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                super.onAuthenticationSucceeded(result)
                print("Authentication Succeeded")
            }
        }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun authenticate() {
        print("authenticate()")
        val biometricPrompt = BiometricPrompt.Builder(this)
            .setTitle("Verify you aren't a bit*h")
            .setSubtitle("")
            .setDescription("Tell me you really are you without telling me you really are you !")
            .setNegativeButton(
                "Cancel",
                this.mainExecutor,
                DialogInterface.OnClickListener { dialog, which ->
                    print("Authentication Cancelled")
                }).build()

        // start the authenticationCallback in mainExecutor
        biometricPrompt.authenticate(getCancellationSignal(), mainExecutor, authenticationCallback)
    }
}