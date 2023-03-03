package com.example.trackmate

import android.content.*
import android.hardware.biometrics.BiometricPrompt
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CancellationSignal
import android.os.CountDownTimer
import android.view.View
import android.widget.Button
import android.widget.TextView
import org.json.JSONObject
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.student)
        Utils.print("launching Student")
        apps = true
        setUI()
        setListener()
        setStatus()
        sendStatus()
        getTimings()
        checkTime()
    }

    private fun reset() {
        Utils.print("resetting")
        apps = false
        authenticated = false
        alert()
        setStatus()
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
                                    val text = "Reset in ${formatTime(seconds)}"
                                    timer_msg.text = text
                                    Utils.print(text)
                                }

                                override fun onFinish() {
                                    reset()
                                    checkTime()
                                }
                            }
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
                (time.hour == timings[pos].getInt("hr") && time.minute > timings[pos].getInt("min"))
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
        verify = findViewById(R.id.verify)
        timer_msg = findViewById(R.id.timer_msg)
        usedApps = findViewById(R.id.used_apps)
        verified = findViewById(R.id.verified)
        verify.setOnClickListener {
            authenticate()
        }
    }

    private fun setListener() {
        Utils.print("setListener()")
        val screen = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    Intent.ACTION_SCREEN_ON -> {
                        screenOff = false
                    }
                    Intent.ACTION_SCREEN_OFF -> {
                        screenOff = true
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
                }
            }
        }
        Server("/student/timings", "GET", null, callback).execute()
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
        Server("/student/status", "POST", json.toString(), callback).execute()
    }

    private fun setStatus() {
        if (authenticated) {
            verified.text = "You are authenticated"
        } else {
            verified.text = "Not authenticated"
        }
        if (apps) {
            usedApps.text = "You have used other apps"
        } else {
            usedApps.text = "No other apps used"
        }
    }
}