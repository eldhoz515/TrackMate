package com.example.trackmate

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import org.json.JSONObject

class Admin : AppCompatActivity() {
    private val root = "http://192.168.160.176:8888"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin)
        Utils.print("launching Admin")
        notifications()
    }

    private fun notifications() {
        checkRequests()
        val notification = findViewById<Button>(R.id.teacher_requests)
        notification.setOnClickListener {
            val teacherReqFragment = Admin_teacherRequests()
            teacherReqFragment.show(supportFragmentManager, "Admin_teacherRequests")
        }
    }
    private fun checkRequests(){
        val callback=object :HttpCallback{
            override fun onComplete(result: HttpResult?) {
                if(result?.data!=null && result.statusCode==200)
                {
                    val json = JSONObject(result.data)
                    val requests = json.getJSONArray("requests")
                    if(requests.length()>0){
                        alert()
                    }
                    else{
                        removeAlert()
                    }
                }
            }
        }
    }
    private fun alert(){
        Utils.print("New Requests")
    }
    private fun removeAlert(){
        Utils.print("No new requests")
    }

}