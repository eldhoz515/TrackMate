package com.example.trackmate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.json.JSONObject

class Admin_teacherRequests : Fragment() {
    private val root = "http://192.168.1.5:8888"
    private var teacherRequests= mutableListOf<JSONObject>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.admin_teacher_requests, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Utils.print("Launching teacher requests")
        getRequests()
    }

    private fun getRequests() {
        val callback = object : HttpCallback {
            override fun onComplete(result: HttpResult?) {
                if (result?.data != null && result.statusCode == 200) {
                    val json = JSONObject(result.data)
                    val requests=json.getJSONArray("requests")
                    for(i in 0 until requests.length()){
                        teacherRequests.add(requests.getJSONObject(i))
                    }
                    if(teacherRequests.size>0){
                        setUI()
                    }
                    else{
                        Utils.print("No teacher requests")
                    }
                }
            }
        }
        Server("$root/admin/requests", "GET", null, callback).execute()
    }

    private fun setUI(){

    }
}