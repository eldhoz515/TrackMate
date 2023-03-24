package com.example.trackmate

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.*
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricPrompt
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.LocalTime

class Student : AppCompatActivity() {

    private var timings = mutableListOf<JSONObject>()
    private val day = LocalDate.now().dayOfWeek.value
    private lateinit var countDownTimer: CountDownTimer
    private var authenticated = false
    private var apps = true
    private var screenOff = false
    private lateinit var verify: Button
    private lateinit var timer_msg: TextView
    private lateinit var verified: TextView
    private lateinit var usedApps: TextView
    private var creds = JSONObject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.student)
        Utils.print("launching Student")
        apps = true
        init()
        checkPermissionsWrapper()
    }

    private fun checkPermissionsWrapper() {
        Utils.print("checking permissions")
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
            checkPermissions(
                arrayOf(
                    Manifest.permission.BLUETOOTH_ADVERTISE
                )
            )
        } else {
            discover()
        }
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
            discover()
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
                    discover()
                } else {
                    Utils.print("Permissions not granted")
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun discover() {
        Utils.print("discover()")
        startActivityForResult(
            Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).putExtra(
                BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,
                300
            ), 1
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Utils.print("Discovery request : $resultCode")
    }

    private fun init() {
        setUI()
        setListener()
        setStatus()
        sendStatus()
        getTimings()
        getCreds()
    }

    override fun onStop() {
        super.onStop()
        Utils.print("stop")
        monitor()
    }

    override fun onDestroy() {
        super.onDestroy()
        Utils.print("destroy")
        monitor()
    }

    override fun onPause() {
        super.onPause()
        Utils.print("pause")
        monitor()
    }

    private fun reset() {
        Utils.print("resetting")
        apps = false
        authenticated = false
        alert()
        setStatus()
    }

    private fun monitor() {
        Handler().postDelayed({
            Utils.print("monitor()")
            if (!screenOff) {
                apps = true
                sendStatus()
                setStatus()
            }
        }, 2000)
    }

    private fun alert() {
        Utils.print("alert()")
        verify.visibility = View.VISIBLE
    }

    private fun formatTime(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }

    private fun checkTime() {
        Utils.print("checking time")
        val time = LocalTime.now()
        if (timings.size > 0) {
            if (day != 7) {
                when (val pos = checkPeriod(time)) {
                    0 -> {
                        val text = "Class not started yet"
                        Utils.print(text)
                        timer_msg.text = text
                        verify.visibility = View.INVISIBLE
                    }
                    timings.size -> {
                        val text = "See you on next class.."
                        Utils.print(text)
                        timer_msg.text = text
                        verify.visibility = View.INVISIBLE
                    }
                    else -> {
                        if (!authenticated) {
                            alert()
                        }
                        val seconds =
                            (timings[pos].getInt("hr") - time.hour) * 60 * 60 +
                                    (timings[pos].getInt("min") - time.minute) * 60
                        countDownTimer =
                            object : CountDownTimer((seconds * 1000).toLong(), 1 * 1000) {
                                override fun onTick(millisUntilFinished: Long) {
                                    val text =
                                        "Reset in ${formatTime((millisUntilFinished / 1000).toInt())}"
                                    timer_msg.text = text
                                    Utils.print(text)
                                }

                                override fun onFinish() {
                                    reset()
                                    countDownTimer.cancel()
                                    Handler().postDelayed({ checkTime() }, 1000)
                                }
                            }
                        countDownTimer.start()
                    }
                }
            } else {
                Utils.print("Sunday...")
                timer_msg.text = "Sunday..."
            }
        } else {
            Utils.print("no timings set")
        }
    }

    private fun checkPeriod(time: LocalTime): Int {
        var pos = 0
        while (pos < timings.size) {
            if (time.hour > timings[pos].getInt("hr") ||
                (time.hour == timings[pos].getInt("hr") && time.minute >= timings[pos].getInt("min"))
            ) {
                ++pos
            } else {
                break
            }
        }
        return pos
    }

    private fun setUI() {
        Utils.print("setUI()")
        val json = Utils.readFile(this, "creds.json")
        if (json != null) {
            findViewById<TextView>(R.id.student_username).text = json.getString("username")
        }
        verify = findViewById(R.id.verify)
        timer_msg = findViewById(R.id.timer_msg)
        usedApps = findViewById(R.id.used_apps)
        verified = findViewById(R.id.verified)
        findViewById<Button>(R.id.student_attendance).setOnClickListener {
            showAttendance()
        }
        verify.setOnClickListener {
            authenticate()
        }
    }

    private fun showAttendance() {
        Utils.print("showAttendance()")
        val file = readFile("creds.json")
        if (file != null) {
            val data = JSONObject()
            data.put("class", file.get("class").toString())
            data.put("username", file.get("username").toString())
            val sAttFragment = Student_Attendance.newInstance(data)
            sAttFragment.show(supportFragmentManager, "Student_Attendance")
        } else {
            Utils.print("Couldn't get student details")
        }
    }

    private fun setListener() {
        Utils.print("setListener()")
        val screen = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    Intent.ACTION_SCREEN_ON -> {
                        screenOff = false
                        Utils.print("screen on")
                    }
                    Intent.ACTION_SCREEN_OFF -> {
                        screenOff = true
                        Utils.print("screen off")
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screen, filter)
    }

    private fun getTimings() {
        Utils.print("getTimings()")
        val callback = object : HttpCallback {
            override fun onComplete(result: HttpResult?) {
                if (result?.data != null && result.statusCode == 200) {
                    val json = JSONObject(result.data)
                    val t = json.getJSONArray("timings")
                    for (i in 0 until t.length()) {
                        timings.add(t.getJSONObject(i))
                    }
                    Utils.print(timings)
                    checkTime()
                }
            }
        }
        Server(this, "/admin/timings", "GET", null, callback).execute()
    }

    private fun getCancellationSignal(): CancellationSignal {
        var cancellationSignal: CancellationSignal? = null
        cancellationSignal = CancellationSignal()
        cancellationSignal.setOnCancelListener {
            Utils.print("Authentication was Cancelled by the user")
        }
        return cancellationSignal
    }

    private val authenticationCallback: BiometricPrompt.AuthenticationCallback
        get() =
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                    super.onAuthenticationError(errorCode, errString)
                    Utils.print("Authentication Error : $errString")
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                    super.onAuthenticationSucceeded(result)
                    Utils.print("Authentication Succeeded")
                    verified()
                }
            }

    private fun verified() {
        Utils.print("verified")
        verify.visibility = View.INVISIBLE
        authenticated = true
        sendStatus()
        setStatus()
    }

    private fun authenticate() {
        Utils.print("authenticate()")
        val biometricPrompt = BiometricPrompt.Builder(this)
            .setTitle("Are you Near?")
            .setSubtitle("")
            .setDescription("Verify your presence near this device by authenticating using Bio-metric")
            .setNegativeButton(
                "Cancel",
                this.mainExecutor,
                DialogInterface.OnClickListener { dialog, which ->
                    Utils.print("Authentication Cancelled")
                }).build()
        biometricPrompt.authenticate(getCancellationSignal(), mainExecutor, authenticationCallback)
    }

    private fun sendStatus() {
        Utils.print("sending status")
        val callback = object : HttpCallback {
            override fun onComplete(result: HttpResult?) {
                if (result != null && result.statusCode == 200) {
                    Utils.print("Status send successfully")
                } else {
                    Utils.print("Status couldn't be send")
                }
            }
        }
        val json = JSONObject()
        val status = JSONObject()
//        json.put("username", "arun")
//        json.put("class", "cse")
        json.put("username", creds.getString("username"))
        json.put("class", creds.getString("class"))
//        Todo
        if (authenticated)
            status.put("auth", 1)
        else
            status.put("auth", 0)
        if (apps)
            status.put("apps", 1)
        else
            status.put("apps", 0)
        Utils.print(status)
        json.put("status", status)
        Server(this, "/student/status", "POST", json.toString(), callback).execute()
    }

    private fun setStatus() {
        if (authenticated) {
            verified.setBackgroundResource(R.drawable.tick_button)
        } else {
            verified.setBackgroundResource(R.drawable.remove_button)
        }
        if (apps) {
            usedApps.setBackgroundResource(R.drawable.mobile)
        } else {
            usedApps.setBackgroundResource(R.drawable.tick_button)
        }
    }

    private fun getCreds() {
        val json = readFile("creds.json")
        if (json != null)
            creds = json
    }

    private fun readFile(fname: String): JSONObject? {
        val file = File(filesDir, fname)
        return if (file.exists()) {
            val jsonString = file.readText()
            JSONObject(jsonString)
        } else {
            null
        }
    }
}