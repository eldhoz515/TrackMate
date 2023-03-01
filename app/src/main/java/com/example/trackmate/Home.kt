package com.example.trackmate

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import org.json.JSONObject
import java.io.File

/*Jump to login activity if credentials aren't present
  Else authenticate based on the type of credentials and direct to corresponding activity
 */

class Home : AppCompatActivity() {
    private val context = this
    private lateinit  var msg :TextView
    private lateinit  var retry :Button

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home)
        msg = findViewById(R.id.msg)
        retry = findViewById(R.id.retry)
        Utils.print("launching home")
        msg.text = "Connecting to server"
        isServerAvailable()
    }

    private fun checkCreds() {
        Utils.print("checkCreds()")
        val file = File(filesDir, "creds.json")
        if (file.exists()) {
            Utils.print("creds exist")
            authenticate()
        } else {
            Utils.print("creds doesn't exist")
            startActivity(Intent(this, Login::class.java))
            finish()
        }
    }

    private fun authenticate() {
        Utils.print("authenticate()")
        val json = readFile("creds.json")
        if (json != null) {
            when (json.get("type")) {
                "admin" -> {
                    val callback = object : HttpCallback {
                        override fun onComplete(result: HttpResult?) {
                            if (result != null && result.statusCode == 200) {
                                startActivity(Intent(context, Admin::class.java))
                                finish()
                            }
                            else{
                                retry.visibility=View.VISIBLE
                                msg.text="Something went wrong"
                                retry.setOnClickListener {
                                    retry.visibility=View.INVISIBLE
                                    authenticate()
                                }
                            }
                        }
                    }
                    Server("/admin/auth", "POST", json.toString(), callback).execute()
                }
                "teacher" -> {
                    val callback = object : HttpCallback {
                        override fun onComplete(result: HttpResult?) {
                            if (result != null && result.statusCode == 200) {
                                startActivity(Intent(context, Teacher::class.java))
                                finish()
                            } else {
                                val printMsg="Request not accepted yet"
                                Utils.print(printMsg)
                                retry.visibility=View.VISIBLE
                                msg.text=printMsg
                                retry.setOnClickListener {
                                    retry.visibility=View.INVISIBLE
                                    authenticate()
                                }
                            }
                        }
                    }
                    Server("/teacher/auth", "POST", json.toString(), callback).execute()
                }
                "student" -> {
                    val callback = object : HttpCallback {
                        override fun onComplete(result: HttpResult?) {
                            if (result != null && result.statusCode == 200) {
                                startActivity(Intent(context, Student::class.java))
                                finish()
                            } else {
                                val printMsg="Request not accepted yet"
                                Utils.print(printMsg)
                                retry.visibility=View.VISIBLE
                                msg.text=printMsg
                                retry.setOnClickListener {
                                    retry.visibility=View.INVISIBLE
                                    authenticate()
                                }
                            }
                        }
                    }
                    Server("/student/auth", "POST", json.toString(), callback).execute()
                }
            }
        }
    }

    private fun isServerAvailable() {
        Utils.print("isServerAvailable()")
        val callback = object : HttpCallback {
            override fun onComplete(result: HttpResult?) {
                if (result != null && result.data == "TrackMate") {
                    Utils.print("Server available")
                    checkCreds()
                } else {
                    val printMsg="Can't connect to server"
                    Utils.print(printMsg)
                    msg.text=printMsg
                    retry.visibility = View.VISIBLE
                    retry.setOnClickListener {
                        retry.visibility=View.INVISIBLE
                        isServerAvailable()
                    }
                }
            }
        }
        Server("/", "GET", null, callback).execute()
    }

    fun writeFile(fname: String, data: JSONObject) {
        val jsonString = data.toString()
        val file = File(filesDir, fname)
        file.writeText(jsonString)
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