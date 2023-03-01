package com.example.trackmate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import org.json.JSONObject

class Admin_classAdd : DialogFragment() {
    private var classes = mutableListOf<String>()
    private lateinit var fragmentView: View
    private lateinit var className: EditText
    private lateinit var addClass: Button
    private lateinit var msg: TextView
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentView = inflater.inflate(R.layout.admin_class_add, container, false)
        return fragmentView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Utils.print("Launching class Add")
        getList()
        setUI()
    }

    private fun getList() {
        Utils.print("getting class list")
        val callback = object : HttpCallback {
            override fun onComplete(result: HttpResult?) {
                if (result?.data != null && result.statusCode == 200) {
                    val json = JSONObject(result.data).getJSONArray("classes")
                    Utils.print(json)
                    for (className in 0 until json.length()) {
                        classes.add(json.get(className).toString())
                    }
                }
            }
        }
        Server("/admin/class/list", "GET", null, callback).execute()
    }

    private fun setUI() {
        className = fragmentView.findViewById(R.id.class_name)
        addClass = fragmentView.findViewById(R.id.add_class)
        msg = fragmentView.findViewById(R.id.add_c_msg)
        addClass.setOnClickListener {
            msg.visibility = View.GONE
            addNewClass()
        }
    }

    private fun addNewClass() {
        Utils.print("adding new class")
        val callback = object : HttpCallback {
            override fun onComplete(result: HttpResult?) {
                msg.visibility = View.VISIBLE
                if (result != null) {
                    if (result.statusCode == 200) {
                        Utils.print("Added new class")
                        msg.text = "Added new class"
                    } else if (result.statusCode == 403) {
                        Utils.print("class exists")
                        msg.text = "Class already exists"
                    }
                } else {
                    msg.text = "Couldn't add new class"
                }
            }
        }
        val json = JSONObject()
        json.put("class", className.text.toString())
        Server("/admin/class/add", "POST", json.toString(), callback).execute()
    }
}