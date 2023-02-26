package com.example.trackmate

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class Teacher : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.teacher)
        Utils.print("launching Teacher")
    }
}