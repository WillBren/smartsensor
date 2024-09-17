package com.example.itproject

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_activity) // Use fragment_activity.xml as the layout
    }
}