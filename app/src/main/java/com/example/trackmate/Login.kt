package com.example.trackmate

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import org.json.JSONObject
import java.io.File

class Login : AppCompatActivity() {
    private val root = "http://192.168.1.5:8888"
    private val context = this
    private var firstTime = true
    private var profile = "student"
    private lateinit var admin: Button
    private lateinit var teacher: Button
    private lateinit var student: Button
    private lateinit var signup: Button
    private lateinit var login: Button
    private lateinit var request: Button
    private lateinit var teachersList: Spinner
    private lateinit var classesList: Spinner
    private lateinit var name: EditText
    private lateinit var username: EditText
    private lateinit var password: EditText
    private lateinit var msg: TextView
    private var selectedTeacher: String? = null
    private var selectedClass: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)
        Utils.print("launching login")
        setUI()
        setFirstTime()
        setProfile()
    }

    private fun onRequest() {
        Utils.print("onRequest()")
        if (!validation())
            return
        msg.visibility=View.INVISIBLE
        val json=JSONObject()
        json.put("username",username.text.toString().lowercase())
        json.put("password",password.text.toString())
        if(firstTime){
            json.put("name",name.text.toString())
            if(profile=="student"){
                json.put("class",selectedClass)
                json.put("teacher",selectedTeacher)
            }
        }

        when (profile) {
            "admin" -> {
                adminRequest(firstTime,json)
            }
            "student" -> {
                studentRequest(firstTime,json)
            }
            "teacher" -> {
                teacherRequest(firstTime,json)
            }
        }
    }

    private fun adminRequest(firstTime: Boolean,data: JSONObject) {
        Utils.print("adminRequest()")
        val callback=object :HttpCallback{
            override fun onComplete(result: HttpResult?) {
                if(result==null || result.statusCode!=200){
                    msg.text="Invalid credentials"
                }
                else{
                    writeFile("creds.json",data)
                    startActivity(Intent(context,Admin::class.java))
                    finish()
                }
            }
        }
        Server("$root/admin/auth","POST",data.toString(),callback).execute()
    }

    private fun studentRequest(firstTime: Boolean,data: JSONObject) {
        Utils.print("studentRequest()")
        if(firstTime){
            val callback=object :HttpCallback{
                override fun onComplete(result: HttpResult?) {
                    if(result!=null && result.statusCode==200){
                        writeFile("creds.json",data)
                        startActivity(Intent(context,Home::class.java))
                        finish()
                    }
                    else if(result!=null && result.statusCode==401){
                        msg.text="Account already exists"
                    }
                    else{
                        msg.text="Invalid credentials"
                    }
                }
            }
            Server("$root/student/new","POST",data.toString(),callback).execute()
        }
        else{
            val callback=object :HttpCallback{
                override fun onComplete(result: HttpResult?) {
                    if(result!=null && result.statusCode==200){
                        writeFile("creds.json",data)
                        startActivity(Intent(context,Student::class.java))
                        finish()
                    }
                    else if(result!=null && result.statusCode==401){
                        msg.text="Invalid credentials"
                    }
                    else{
                        msg.text="No matching account accepted in class"
                    }
                }
            }
            Server("$root/student/auth","POST",data.toString(),callback).execute()
        }
    }

    private fun teacherRequest(firstTime: Boolean,data: JSONObject) {
        Utils.print("teacherRequest()")
        if(firstTime){
            val callback=object :HttpCallback{
                override fun onComplete(result: HttpResult?) {
                    if(result!=null && result.statusCode==200){
                        writeFile("creds.json",data)
                        startActivity(Intent(context,Home::class.java))
                        finish()
                    }
                    else if(result!=null && result.statusCode==401){
                        msg.text="Account already exists"
                    }
                    else{
                        msg.text="Invalid credentials"
                    }
                }
            }
            Server("$root/teacher/new","POST",data.toString(),callback).execute()
        }
        else{
            val callback=object :HttpCallback{
                override fun onComplete(result: HttpResult?) {
                    if(result!=null && result.statusCode==200){
                        writeFile("creds.json",data)
                        startActivity(Intent(context,Teacher::class.java))
                        finish()
                    }
                    else if(result!=null && result.statusCode==401){
                        msg.text="Invalid credentials"
                    }
                    else{
                        msg.text="No matching account accepted by Admin"
                    }
                }
            }
            Server("$root/teacher/auth","POST",data.toString(),callback).execute()
        }
    }

    private fun validation(): Boolean {
        Utils.print("validation()")
        var valid = true
        listOf(username, password).forEach { item ->
            run {
                if (item.text.toString().length < 5) {
                    msg.text = "Length of all fields must be atleast 5 characters"
                    valid = false
                }
            }
        }
        if (firstTime) {
            if (name.text.toString().length < 5) {
                msg.text = "Length of all fields must be atleast 5 characters"
                valid = false
            }
        }
        if (profile == "student") {
            if (selectedTeacher == null || selectedClass == null) {
                msg.text = "Select your class and teacher"
                valid = false
            }
        }
        return valid
    }

    private fun setUI() {
        setViews()
        listOf(admin, teacher, student).forEach { button ->
            run {
                button.setOnClickListener {
                    profile = button.text.toString().lowercase()
                    setProfile()
                }
            }
        }
        signup.setOnClickListener {
            firstTime = true
            setFirstTime()
        }
        login.setOnClickListener {
            firstTime = false
            setFirstTime()
        }
        request.setOnClickListener {
            onRequest()
        }
    }

    private fun setViews() {
        admin = findViewById(R.id.admin)
        teacher = findViewById(R.id.teacher)
        student = findViewById(R.id.student)
        signup = findViewById(R.id.signup)
        login = findViewById(R.id.login)
        request = findViewById(R.id.request)
        teachersList = findViewById(R.id.teachers_list)
        classesList = findViewById(R.id.classes_list)
        name = findViewById(R.id.name)
        username = findViewById(R.id.username)
        password = findViewById(R.id.password)
        msg = findViewById(R.id.loginMsg)
    }

    private fun setProfile() {
        when (profile) {
            "student" -> {
                teachersList.visibility = View.VISIBLE
                classesList.visibility = View.VISIBLE
            }
            else -> {
                teachersList.visibility = View.GONE
                classesList.visibility = View.GONE
            }
        }
    }

    private fun setFirstTime() {
        if (firstTime) {
            name.visibility = View.VISIBLE
        } else {
            name.visibility = View.GONE
        }
    }

    private fun writeFile(fname: String, data: JSONObject) {
        val jsonString = data.toString()
        val file = File(filesDir, fname)
        file.writeText(jsonString)
    }

}