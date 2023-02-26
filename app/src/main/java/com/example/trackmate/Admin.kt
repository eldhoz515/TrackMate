package com.example.trackmate

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class Admin : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin)
        Utils.print("launching Admin")
    }
}