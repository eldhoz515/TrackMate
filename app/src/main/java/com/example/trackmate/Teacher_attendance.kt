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
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject

private lateinit var selectedClass: String
private var students = JSONObject()

class Teacher_attendance : DialogFragment() {
    private lateinit var fragmentView: View
    private lateinit var classesList: Spinner
    private var classes = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentView = inflater.inflate(R.layout.teacher_attendance, container, false)
        return fragmentView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Utils.print("Launching teacher attendance")
        getClasses()
    }

    private fun getClasses() {
        val callback = object : HttpCallback {
            override fun onComplete(result: HttpResult?) {
                if (result?.data != null && result.statusCode == 200) {
                    val json = JSONObject(result.data).getJSONArray("classes")
                    Utils.print(json)
                    for (className in 0 until json.length()) {
                        classes.add(json.get(className).toString())
                    }
                    if (classes.size > 0) {
                        setupSpinner()
                    } else {
                        Utils.print("No classes")
                        val msg = fragmentView.findViewById<TextView>(R.id.text_no_class2)
                        msg.visibility = View.VISIBLE
                    }
                }
            }
        }
        Server("/teacher/class/list", "GET", null, callback).execute()
    }

    private fun setupSpinner() {
        Utils.print("setupSpinner()")
        classesList = fragmentView.findViewById(R.id.t_a_c_list)
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, classes)
        classesList.adapter = adapter
        classesList.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                Utils.print("class selected")
                val item = parent.getItemAtPosition(position).toString()
                selectedClass = item
                students = JSONObject()
                getStudents(item)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Another interface callback
            }
        }
    }

    private fun getStudents(className: String) {
        val data = JSONObject()
        data.put("class", className)
        val callback = object : HttpCallback {
            override fun onComplete(result: HttpResult?) {
                if (result?.data != null && result.statusCode == 200) {
                    val json = JSONObject(result.data)
                    for (student in json.keys()) {
                        students.put("${json.getJSONObject(student).get("id")}",json.getJSONObject(student))
                    }
                    if (students.length() > 0) {
                        getAttendance()
                    } else {
                        Utils.print("No students in this class")
                        val msg = fragmentView.findViewById<TextView>(R.id.t_a_msg)
                        msg.visibility = View.VISIBLE
                    }
                }
            }
        }
        Server("/teacher/class/view", "POST", data.toString(), callback).execute()
    }

    private fun getAttendance() {
        Utils.print("getAttendance()")
        discoverDevices()
    }

    @SuppressLint("MissingPermission")
    private fun discoverDevices() {
        Utils.print("discoverDevices()")
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val discover = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        Utils.print("device found")
                        val device: BluetoothDevice? =
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        if (device != null) {
                            deviceFound(device.name, device.address)
                        }
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                        Utils.print("discovery started")
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        Utils.print("discovery stopped")
                        calculateAttendance()
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        context?.registerReceiver(discover, filter)
        bluetoothAdapter.startDiscovery()
    }

    private fun deviceFound(name: String, address: String) {
        Utils.print("Device found: $name ($address)")
        if(students.get(address)!=null){
            val student= students.getJSONObject(address)
            student.put("attendance",1)
            students.put(address,student)
        }
        else{
            Utils.print("address not found")
        }
    }

    private fun calculateAttendance() {

    }
}