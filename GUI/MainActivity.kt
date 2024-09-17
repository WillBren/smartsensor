package com.example.itproject

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Ensures edge-to-edge UI
        setContentView(R.layout.activity_main) // Sets the main menu layout

        // Adjust padding for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Find the "Activity" button and set the OnClickListener to navigate to Activity.kt
        val activityButton = findViewById<Button>(R.id.activity_button)
        activityButton.setOnClickListener {
            val intent = Intent(this, Activity::class.java) // Navigate to Activity.kt
            startActivity(intent)
        }

        // Find the "Health" button and set the OnClickListener to navigate to HealthFragment
        val healthButton = findViewById<Button>(R.id.health_button) // Ensure this ID matches the XML
        healthButton.setOnClickListener {
            val intent = Intent(this, Health::class.java) // Navigate to HealthActivity.kt
            startActivity(intent)
        }


        //FInd nutrition Button
        val nutritionButton = findViewById<Button>(R.id.nutrition_button)
        nutritionButton.setOnClickListener {
            val intent = Intent (this, Nutrition::class.java)
            startActivity(intent)
        }
    }
}
