package com.example.trackmate

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import org.json.JSONObject

class Admin : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin)
        Utils.print("launching Admin")
        setUI()
//        checkRequests()
    }

    private fun setUI() {
        Utils.print("setting UI")
        val json = Utils.readFile(this, "creds.json")
        if (json != null) {
            findViewById<TextView>(R.id.admin_username).text = json.getString("username")
        }
        findViewById<Button>(R.id.teacher_check_a).setOnClickListener {
            manageTimings()
        }
        findViewById<Button>(R.id.admin_view_classes).setOnClickListener {
            manageClasses()
        }
        findViewById<Button>(R.id.admin_add_class).setOnClickListener {
            addClass()
        }
        findViewById<Button>(R.id.s_req_notifier).setOnClickListener {
            notifications()
        }
        findViewById<Button>(R.id.button_s_list).setOnClickListener {
            manageTeachers()
        }
        findViewById<Button>(R.id.admin_view_students).setOnClickListener {
            manageStudents()
        }
        findViewById<Button>(R.id.student_attendance).setOnClickListener {
            //todo
        }
    }

    private fun manageTimings() {
        Utils.print("manageTimings()")
        val studentListFragment = Admin_timings()
        studentListFragment.show(supportFragmentManager, "Admin_timings")
    }

    private fun addClass() {
        Utils.print("addClass()")
        val studentListFragment = Admin_classAdd()
        studentListFragment.show(supportFragmentManager, "Admin_classAdd")
    }

    private fun manageStudents() {
        Utils.print("manageStudents()")
        val studentListFragment = Admin_studentsList()
        studentListFragment.show(supportFragmentManager, "Admin_studentsList")
    }

    private fun manageClasses() {
        Utils.print("manageClasses()")
        val classListFragment = Admin_classList()
        classListFragment.show(supportFragmentManager, "Admin_classList")
    }

    private fun manageTeachers() {
        Utils.print("manageTeachers()")
        val teacherListFragment = Admin_teachersList()
        teacherListFragment.show(supportFragmentManager, "Admin_teachersList")
    }

    private fun notifications() {
        Utils.print("notifications()")
        val teacherReqFragment = Admin_teacherRequests()
        teacherReqFragment.show(supportFragmentManager, "Admin_teacherRequests")
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
        Server(this, "/admin/requests", "POST", null, callback).execute()
    }

    private fun alert() {
        Utils.print("New Requests")
    }

    private fun removeAlert() {
        Utils.print("No new requests")
    }

}