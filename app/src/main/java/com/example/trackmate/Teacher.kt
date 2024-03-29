package com.example.trackmate

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.io.File

private lateinit var teacherName: String

class Teacher : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.teacher)
        Utils.print("launching Teacher")
        setUI()
    }

    private fun setUI() {
        Utils.print("setUI()")
        val json = Utils.readFile(this, "creds.json")
        if (json != null) {
            findViewById<TextView>(R.id.teacher_username).text = json.getString("username")
        }
        findViewById<Button>(R.id.student_attendance).setOnClickListener {
            viewAttendance()
        }
        findViewById<Button>(R.id.teacher_check_a).setOnClickListener {
            findViewById<TextView>(R.id.t_error).visibility = View.GONE
            checkPermissionsWrapper()
        }
        findViewById<Button>(R.id.button_s_list).setOnClickListener {
            manageStudents()
        }
        findViewById<Button>(R.id.s_req_notifier).setOnClickListener {
            notifications()
        }
    }

    private fun viewAttendance(){
        Utils.print("getAttendance()")
        val studentListFragment = Attendance()
        studentListFragment.show(supportFragmentManager, "Attendance")
    }

    private fun manageStudents() {
        Utils.print("manageStudents()")
        val studentListFragment = Admin_studentsList()
        studentListFragment.show(supportFragmentManager, "Admin_studentsList")
    }

    private fun notifications() {
        Utils.print("notifications()")
        val file = readFile("creds.json")
        if (file != null) {
            teacherName = file.get("username").toString()
            val teacherReqFragment = Teacher_studentRequests.newInstance(teacherName)
            teacherReqFragment.show(supportFragmentManager, "Teacher_studentRequests")
        }
        else{
            Utils.print("Couldn't get teacher name")
        }
    }

    private fun checkRequests() {
        Utils.print("checking requests")
        val callback = object : HttpCallback {
            override fun onComplete(result: HttpResult?) {
                if (result?.data != null && result.statusCode == 200) {
                    val json = JSONObject(result.data)
                    val requests = json.getJSONArray("requests")
                    if (requests.length() > 0) {
                        alert()
                    } else {
                        removeAlert()
                    }
                }
            }
        }
        val file = readFile("creds.json")

        if (file != null) {
            val data = JSONObject()
            teacherName = file.get("username").toString()
            data.put("username", teacherName)
            Server(this, "/teacher/requests", "POST", data.toString(), callback).execute()
        }
    }

    private fun alert() {
        Utils.print("New Requests")
    }

    private fun removeAlert() {
        Utils.print("No new requests")
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
            checkServices()
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
                    checkServices()
                } else {
                    Utils.print("Permissions not granted")
                    findViewById<TextView>(R.id.t_error).visibility = View.VISIBLE
                }
            }
        }
    }

    private fun checkServices() {
        Utils.print("checkServices()")
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (bluetoothAdapter.isEnabled && (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
                        LocationManager.NETWORK_PROVIDER
                ))
        )
            getAttendance()
        else {
            Utils.print("Services disabled")
            findViewById<TextView>(R.id.t_error).visibility = View.VISIBLE
        }
    }

    private fun getAttendance() {
        Utils.print("getAttendance()")
        val teacherAttendanceFragment = Teacher_attendance()
        teacherAttendanceFragment.show(supportFragmentManager, "Teacher_attendance")
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