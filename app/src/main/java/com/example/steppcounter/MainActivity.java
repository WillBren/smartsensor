package com.example.steppcounter;

import android.hardware.Sensor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView stepsValue, heartRateValue, caloriesBurned, bmi, temp;
    private MaterialButton activityButton, healthButton, editDetailsButton; //creates buttons
    private TemperatureSensorManager temperatureSensorManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Inflate the provided XML layout (activity_main.xml)

        // Create an instance of SensorChecker and check for Significant Motion Sensor
        SensorChecker sensorChecker = new SensorChecker(this);  // Pass 'this' as the context
        sensorChecker.checkForSignificantMotionSensor();

        // Initialize the views from the XML layout
        stepsValue = findViewById(R.id.steps_value);
        heartRateValue = findViewById(R.id.heart_rate_value);
        caloriesBurned = findViewById(R.id.calories_burned);
        bmi = findViewById(R.id.bmi);
        activityButton = findViewById(R.id.activity_button);
        healthButton = findViewById(R.id.health_button);
        editDetailsButton = findViewById(R.id.edit_details_button);

        // Set up click listeners for the buttons
        setupButtonListeners();

        temp = findViewById(R.id.alert);

        //if temperature sensor is active then change temperature on main screen
        temperatureSensorManager = new TemperatureSensorManager(this, new TemperatureSensorManager.TemperatureCallback() {
            @Override
            public void onTemperatureChanged(float temperature) {
                temp.setText("Current Temperature: " + temperature + "°C");
            }
        }, this);

        //if temperature sensor is not on device, set text to tell user that sensor is not available
        if (temperatureSensorManager == null || temperatureSensorManager.temperatureSensor == null) {
            temp.setText("Ambient Temperature Sensor not available");
        } else {
            temperatureSensorManager.startListening();
        }

        // Check for Heart Rate Sensor
        sensorChecker.checkForHeartRateSensor();

        // Check for Significant Motion Sensor
        sensorChecker.checkForSignificantMotionSensor();
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

        editDetailsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Nutrition button clicked!", Toast.LENGTH_SHORT).show();
                // tells button to go from main to nutrition screen
            }
        });
    }

    protected void onDestroy() {
        super.onDestroy();
        if (temperatureSensorManager != null) {
            temperatureSensorManager.stopListening();
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (temperatureSensorManager != null) {
            temperatureSensorManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }




}
