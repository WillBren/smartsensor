package com.example.steppcounter;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public class MainActivity extends AppCompatActivity {

    private TextView stepsValue, heartRateValue, caloriesBurned, bmi;
    private MaterialButton activityButton, healthButton, nutritionButton; //creates buttons

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Inflate the provided XML layout (activity_main.xml)

        // Initialize the views from the XML layout
        stepsValue = findViewById(R.id.steps_value);
        heartRateValue = findViewById(R.id.heart_rate_value);
        caloriesBurned = findViewById(R.id.calories_burned);
        bmi = findViewById(R.id.bmi);
        activityButton = findViewById(R.id.activity_button);
        healthButton = findViewById(R.id.health_button);
        nutritionButton = findViewById(R.id.nutrition_button);

        // Set up click listeners for the buttons
        setupButtonListeners();
    }

    private void setupButtonListeners() {
        activityButton.setOnClickListener(new View.OnClickListener() {
            @Override

            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Activity button clicked!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, StepActivity.class);
                startActivity(intent);
                // tells button to go from main screen to activity screen
            }
        });

        healthButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Health button clicked!", Toast.LENGTH_SHORT).show();
                // tells button to go from main to health screen
            }
        });

        nutritionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Nutrition button clicked!", Toast.LENGTH_SHORT).show();
                // tells button to go from main to nutrition screen
            }
        });
    }


}
