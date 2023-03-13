package com.example.trackmate

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.Group
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject

private lateinit var selectedClass: String
private var students = JSONObject()
private var studentsList = mutableListOf<JSONObject>()

class Teacher_attendance : DialogFragment() {
    private lateinit var fragmentView: View
    private lateinit var classesList: Spinner
    private lateinit var save: Button
    private lateinit var retry: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var group:Group
    private lateinit var adapter: Teacher_attendance.AdapterAttendanceList

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
                        setUI()
                    } else {
                        Utils.print("No classes")
                        val msg = fragmentView.findViewById<TextView>(R.id.t_a_msg)
                        msg.visibility = View.VISIBLE
                    }
                }
            }
        }
        Server(requireContext(),"/teacher/class/list", "GET", null, callback).execute()
    }

    private fun setUI() {
        Utils.print("setUI()")
        classesList = fragmentView.findViewById(R.id.t_a_c_list)
        save = fragmentView.findViewById(R.id.mark_attendance)
        retry = fragmentView.findViewById(R.id.check_attendance)
        group.visibility=View.VISIBLE
        save.setOnClickListener {
            markAttendance()
        }
        retry.setOnClickListener {
            getAttendance()
        }
        recyclerView = fragmentView.findViewById(R.id.t_a_list_list)
        val layoutManager = LinearLayoutManager(context)
        recyclerView.layoutManager = layoutManager
        setupSpinner()
    }

    private fun setupSpinner() {
        Utils.print("setupSpinner()")
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
                        students.put(
                            "${json.getJSONObject(student).get("id")}",
                            json.getJSONObject(student)
                        )
                    }
                    Utils.print(students)
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
        Server(requireContext(),"/teacher/class/view", "POST", data.toString(), callback).execute()
    }

    private fun getAttendance() {
        Utils.print("getAttendance()")
        group.visibility=View.GONE
        Utils.print(studentsList)
        val t = studentsList.size
        studentsList = mutableListOf()
        recyclerView.visibility = View.INVISIBLE
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
                        displayAttendance()
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
        if (students.has(address)) {
            val student = students.getJSONObject(address)
            Utils.print("${student.get("username")} is nearby")
            student.put("attendance", 1)
            students.put(address, student)
        } else {
            Utils.print("address not found")
        }
    }

    private fun displayAttendance() {
        Utils.print("displayAttendance()")
        Utils.print(students)
        studentsList = mutableListOf()
        for (address in students.keys()) {
            val student = students.getJSONObject(address)
            if (student.has("attendance")) {
                studentsList.add(student)
            } else {
                studentsList.add(0, student)
            }
        }
        Utils.print(studentsList)
        adapter = AdapterAttendanceList(studentsList,requireContext())
        recyclerView.adapter = adapter
        recyclerView.visibility = View.VISIBLE
        save.visibility = View.VISIBLE
        retry.visibility = View.VISIBLE
    }

    class AdapterAttendanceList(private val items: MutableList<JSONObject>,private val con: Context) :
        RecyclerView.Adapter<AdapterAttendanceList.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.t_a_name)
            val apps: TextView = view.findViewById(R.id.t_a_apps)
            val attendance: TextView = view.findViewById(R.id.t_a_attendance)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_t_a_list, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.name.text = item.get("name").toString()
            if (item.has("status")) {

                if (item.getJSONObject("status").getInt("apps") == 1) {
                    holder.apps.text = "used"
                } else {
                    holder.apps.text = "didn't use"
                }
            } else {
                holder.apps.text = "used"
            }
            if (item.has("attendance")) {
                if (item.has("status")) {
                    if (item.getJSONObject("status").getInt("auth") == 1)
                        holder.attendance.text = "present"
                    else
                        holder.attendance.text = "not sure"
                } else {
                    holder.attendance.text = "not sure"
                }
            } else {
                holder.attendance.text = "absent"
            }
        }

        override fun getItemCount() = items.size
    }

    private fun markAttendance() {
        val json = JSONObject()
        json.put("class", selectedClass)
        val attendance = JSONObject()
        for (student in studentsList) {
            if (student.has("attendance"))
                attendance.put(student.getString("username"), 1)
            else
                attendance.put(student.getString("username"), 0)
        }
        json.put("attendance", attendance)
        Utils.print(json)
        val callback = object : HttpCallback {
            override fun onComplete(result: HttpResult?) {
                if (result != null && result.statusCode == 200) {
                    Utils.print("marked attendance successfully")
                } else {
                    Utils.print("marking attendance failed")
                }
            }
        }
        Server(requireContext(),"/teacher/attendance", "POST", json.toString(), callback).execute()
    }
}