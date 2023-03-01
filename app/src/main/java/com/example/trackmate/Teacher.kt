package com.example.trackmate

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import org.json.JSONObject
import java.io.File

class Teacher : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.teacher)
        Utils.print("launching Teacher")
    }

    private fun manageStudents() {
        Utils.print("manageStudents()")
        val studentListFragment = Admin_studentsList()
        studentListFragment.show(supportFragmentManager, "Admin_studentsList")
    }

    private fun notifications() {
        Utils.print("notifications()")
        checkRequests()
        val notification = findViewById<Button>(R.id.s_req_notifier)
        notification.setOnClickListener {
            val teacherReqFragment = Teacher_studentRequests()
            teacherReqFragment.show(supportFragmentManager, "Teacher_studentRequests")
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
            val data=JSONObject()
            data.put("username",file.get("username").toString())
            Server("/teacher/requests", "POST", data.toString(), callback).execute()
        }
    }

    private fun alert() {
        Utils.print("New Requests")
    }

    private fun removeAlert() {
        Utils.print("No new requests")
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