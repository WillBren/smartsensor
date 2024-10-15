package com.example.steppcounter;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private TextView temp;
    private MaterialButton activityButton, healthButton, editDetailsButton, logoutButton; //creates buttons
    private TemperatureSensorManager temperatureSensorManager;
    private FirebaseAuth mAuth;


    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Inflate the provided XML layout (activity_main.xml)

        mAuth = FirebaseAuth.getInstance();

        // Create an instance of SensorChecker and check for Significant Motion Sensor
        SensorChecker sensorChecker = new SensorChecker(this);  // Pass 'this' as the context
        sensorChecker.checkForSignificantMotionSensor();

        // Initialize the views from the XML layout
        TextView stepsValue = findViewById(R.id.steps_value);
        TextView heartRateValue = findViewById(R.id.heart_rate_value);
        TextView caloriesBurned = findViewById(R.id.calories_burned);
        TextView bmi = findViewById(R.id.bmi);
        activityButton = findViewById(R.id.activity_button);
        healthButton = findViewById(R.id.health_button);
        editDetailsButton = findViewById(R.id.edit_details_button);
        logoutButton = findViewById(R.id.logout_button);

        // Set up click listeners for the buttons
        setupButtonListeners();

        temp = findViewById(R.id.alert);

        //if temperature sensor is active then change temperature on main screen
        temperatureSensorManager = new TemperatureSensorManager(this, new TemperatureSensorManager.TemperatureCallback() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onTemperatureChanged(float temperature) {
                temp.setText("Current Temperature: " + temperature + "°C");
            }
        }, this);

        //if temperature sensor is not on device, set text to tell user that sensor is not available
        if (temperatureSensorManager.temperatureSensor == null) {
            temp.setText("Ambient Temperature Sensor not available");
        } else {
            temperatureSensorManager.startListening();
        }

        // Check for Heart Rate Sensor
        sensorChecker.checkForHeartRateSensor();

        // Check for Significant Motion Sensor
        sensorChecker.checkForSignificantMotionSensor();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser == null){
            // If no user is signed in, navigate back to Login Activity
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void setupButtonListeners() {
        activityButton.setOnClickListener(v -> {
            Toast.makeText(MainActivity.this, "Activity button clicked!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(MainActivity.this, StepActivity.class);
            startActivity(intent);
            // tells button to go from main screen to activity screen
        });

        healthButton.setOnClickListener(v -> {
            Toast.makeText(MainActivity.this, "Health button clicked!", Toast.LENGTH_SHORT).show();
            // tells button to go from main to health screen
        });

        editDetailsButton.setOnClickListener(v -> {
            Toast.makeText(MainActivity.this, "Nutrition button clicked!", Toast.LENGTH_SHORT).show();
            // tells button to go from main to nutrition screen
        });

        logoutButton.setOnClickListener(v -> {
            mAuth.signOut(); // Sign out from Firebase
            Toast.makeText(MainActivity.this, "Logged out successfully!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class); // Navigate to Login Activity
            startActivity(intent);
            finish(); // Close the MainActivity
        });
    }

    protected void onDestroy() {
        super.onDestroy();
        if (temperatureSensorManager != null) {
            temperatureSensorManager.stopListening();
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (temperatureSensorManager != null) {
            temperatureSensorManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
