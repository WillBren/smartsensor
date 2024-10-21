package com.example.steppcounter;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView temp;
    private MaterialButton activityButton, healthButton, editDetailsButton, logoutButton; //creates buttons
    private TemperatureSensorManager temperatureSensorManager;
    private FirebaseAuth mAuth;
    private TextView stepsValue, distanceValue, caloriesBurned, bmiValue;
    private FirebaseFirestore db;

    private TextView welcomeBanner;


    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Inflate the provided XML layout (activity_main.xml)

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();




        // Initialize the views from the XML layout
        welcomeBanner = findViewById(R.id.welcome_banner);
        stepsValue = findViewById(R.id.steps_value);
        distanceValue = findViewById(R.id.distance_value); // Add this TextView in XML for distance
        caloriesBurned = findViewById(R.id.calories_burned);
        bmiValue = findViewById(R.id.bmi);
        activityButton = findViewById(R.id.activity_button);

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



        // Load and display user's name
        loadUserName();

        // Load weekly data from Firestore
        loadWeeklyData();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // If no user is signed in, navigate back to Login Activity
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void setupButtonListeners() {
        activityButton.setOnClickListener(v -> {
            //Toast.makeText(MainActivity.this, "Activity button clicked!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(MainActivity.this, StepActivity.class);
            startActivity(intent);
            // tells button to go from main screen to activity screen
        });

        editDetailsButton.setOnClickListener(v -> {
            //Toast.makeText(MainActivity.this, "Nutrition button clicked!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(MainActivity.this, EditDetailsActivity.class);
            startActivity(intent);
        });

        logoutButton.setOnClickListener(v -> {
            mAuth.signOut(); // Sign out from Firebase
            //Toast.makeText(MainActivity.this, "Logged out successfully!", Toast.LENGTH_SHORT).show();
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

    private void loadWeeklyData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();

        // Get current date and the date 7 days ago
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        String endDate = sdf.format(calendar.getTime());
        calendar.add(Calendar.DAY_OF_YEAR, -7);
        String startDate = sdf.format(calendar.getTime());

        db.collection("users")
                .document(userId)
                .collection("Activity")
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            int totalSteps = 0;
                            double totalDistance = 0;
                            int totalCalories = 0;

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                int steps = document.getLong("steps").intValue();
                                double distance = document.getDouble("distance");
                                int calories = document.getLong("calories burnt").intValue();

                                totalSteps += steps;
                                totalDistance += distance;
                                totalCalories += calories;
                            }

                            stepsValue.setText(String.valueOf(totalSteps) + " Steps");
                            distanceValue.setText(String.format(Locale.getDefault(), "%.2f meters", totalDistance));
                            caloriesBurned.setText(String.valueOf(totalCalories) + " kcal");

                            loadUserBMI();
                        } else {
                            Toast.makeText(MainActivity.this, "Error getting documents: " + task.getException(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void loadUserBMI() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();

        // Access the "Details" subcollection and "personalInfo" document
        db.collection("users")
                .document(userId)
                .collection("Details")
                .document("personalInfo")
                .get()
                .addOnCompleteListener(new OnCompleteListener<com.google.firebase.firestore.DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<com.google.firebase.firestore.DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            com.google.firebase.firestore.DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                // Firestore stores height and weight as integers (in your example)
                                long height = document.getLong("height"); // Height in centimeters
                                long weight = document.getLong("weight"); // Weight in grams

                                // Convert height to meters and weight to kilograms
                                double heightInMeters = height / 100.0;
                                double weightInKg = weight;

                                if (heightInMeters > 0) {
                                    double bmi = weightInKg / (heightInMeters * heightInMeters);
                                    bmiValue.setText(String.format(Locale.getDefault(), "BMI: %.2f", bmi));
                                } else {
                                    bmiValue.setText("BMI: N/A");
                                }
                            } else {
                                Toast.makeText(MainActivity.this, "User personal info not found", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(MainActivity.this, "Error retrieving user data", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void loadUserName() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();

        // Access the "Details" subcollection and "personalInfo" document
        db.collection("users")
                .document(userId)
                .collection("Details")
                .document("personalInfo")
                .get()
                .addOnCompleteListener(new OnCompleteListener<com.google.firebase.firestore.DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<com.google.firebase.firestore.DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            com.google.firebase.firestore.DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                String name = document.getString("name");
                                if (name != null && !name.isEmpty()) {
                                    welcomeBanner.setText("Welcome, " + name + "!");
                                } else {
                                    welcomeBanner.setText("Welcome!");
                                }
                            } else {
                                welcomeBanner.setText("Welcome!");
                                Toast.makeText(MainActivity.this, "User personal info not found", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(MainActivity.this, "Error retrieving user data", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

}

