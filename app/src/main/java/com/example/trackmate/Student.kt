package com.example.trackmate

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class Student : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.student)
        Utils.print("launching Student")
    }
}