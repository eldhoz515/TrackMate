package com.example.trackmate

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import org.json.JSONObject

class Admin : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin)
        Utils.print("launching Admin")
        addClass()
    }

    private fun init(){
        notifications()
        manageTeachers()
        manageClasses()
        manageStudents()
    }

    private fun addClass(){
        Utils.print("addClass()")
        val studentListFragment = Admin_classAdd()
        studentListFragment.show(supportFragmentManager, "Admin_classAdd")
    }

    private fun manageStudents(){
        Utils.print("manageStudents()")
        val studentListFragment = Admin_studentsList()
        studentListFragment.show(supportFragmentManager, "Admin_studentsList")
    }

    private fun manageClasses(){
        Utils.print("manageClasses()")
        val classListFragment = Admin_classList()
        classListFragment.show(supportFragmentManager, "Admin_classList")
    }

    private fun manageTeachers() {
        Utils.print("manageTeachers()")
        val tListButton = findViewById<Button>(R.id.button_t_list)
        tListButton.setOnClickListener {
            val teacherListFragment = Admin_teachersList()
            teacherListFragment.show(supportFragmentManager, "Admin_teachersList")
        }
    }

    private fun notifications() {
        Utils.print("notifications()")
        checkRequests()
        val notification = findViewById<Button>(R.id.teacher_requests)
        notification.setOnClickListener {
            val teacherReqFragment = Admin_teacherRequests()
            teacherReqFragment.show(supportFragmentManager, "Admin_teacherRequests")
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
        Server("/admin/requests", "POST", null, callback).execute()
    }

    private fun alert() {
        Utils.print("New Requests")
    }

    private fun removeAlert() {
        Utils.print("No new requests")
    }

}