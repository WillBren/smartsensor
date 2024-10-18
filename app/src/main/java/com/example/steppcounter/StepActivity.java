package com.example.steppcounter;
import static android.content.ContentValues.TAG;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import android.hardware.SensorEventListener;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.animation.Easing;
import com.google.firebase.firestore.QueryDocumentSnapshot;


public class StepActivity extends AppCompatActivity implements SensorEventListener {
    private int totalSteps;
    private SensorManager mSensorManager = null;
    private Sensor stepSensor;
    private ProgressBar progressBar;
    private TextView steps;
    private MaterialButton backButton;
    private TextView distanceStepped;
    private TextView caloriesBurnt;
    private final DecimalFormat df = new DecimalFormat("#.##");
    private static final String PREF_TOTAL_STEPS = "total_steps";
    private static final String PREF_DAILY_STEPS = "daily_steps";
    private int dailySteps;
    private SharedPreferences sharedPref;
    private SharedPreferences.OnSharedPreferenceChangeListener prefListener;
    private LineChart lineChart;


    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.fragment_activity);

        progressBar= findViewById(R.id.progressBar);
        steps = findViewById(R.id.steps);
        distanceStepped = findViewById(R.id.distanceStepped);
        caloriesBurnt = findViewById(R.id.caloriesBurnt);
        lineChart = findViewById(R.id.lineChart);  // Initialize the chart

        backButton = findViewById(R.id.back_button); //sets up back button to id in fragment_activity xml file
        setupButtonListeners();

        // Set the chart title
        TextView chartTitle = findViewById(R.id.chartTitle);
        chartTitle.setText("Weekly Steps Overview");



        sharedPref = getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        prefListener = (sharedPreferences, key) -> {
            if (PREF_DAILY_STEPS.equals(key)) {
                dailySteps = (int) sharedPreferences.getFloat(PREF_DAILY_STEPS, 0f);
                updateUI();
            }
        };

        loadData();
        scheduleDailyStepReset();

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        stepSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER); //sets up step count sensor

        setupChart();  // Add this function to configure the chart
        loadWeeklySteps();  // Function to retrieve step data for the past week

        //checks if step sensor is available on the device
        if (stepSensor == null) {
            Toast.makeText(this, "Step counter sensor is not present on this device", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Step counter sensor is available", Toast.LENGTH_SHORT).show();
        }

        //Checks for user permission to access the step counter sensor
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, 1);
        }

        FirebaseApp.initializeApp(this);

    }

    @Override
    //asks user for permission to access step counter sensor
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                Toast.makeText(this, "Activity recognition permission granted", Toast.LENGTH_SHORT).show();
            } else {
                // Permission denied
                Toast.makeText(this, "Activity recognition permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // cancels any existing alarms to avoid conflict with main alarm.
    private void cancelExistingAlarm() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, StepResetReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        alarmManager.cancel(pendingIntent);
    }

    //method schedules the daily reset for 12:00am daily
    private void scheduleDailyStepReset() {
        cancelExistingAlarm(); // Cancel any existing alarms

        // Set up the alarm to trigger at the next midnight
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // If it's past midnight, schedule for the next day
        if (Calendar.getInstance().after(calendar)) {
            calendar.add(Calendar.DATE, 1);
        }

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, StepResetReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Schedule the alarm
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
    }

    private void setupButtonListeners() {
        backButton.setOnClickListener(v -> {
            Toast.makeText(StepActivity.this, "Activity button clicked!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(StepActivity.this, MainActivity.class);
            startActivity(intent);
            // tells button to go from step activity to the main activity
        });
    }

    protected void onResume() {
        super.onResume();
        loadData(); // Load the latest data, including after a reset
        if (stepSensor == null) {
            Toast.makeText(this, "Device has no sensor", Toast.LENGTH_SHORT).show();
        } else {
            mSensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    protected void onPause() {
        super.onPause();
        sharedPref.unregisterOnSharedPreferenceChangeListener(prefListener);
        mSensorManager.unregisterListener(this);
    }

    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            float currentTotalSteps = event.values[0];

            //Might be redundant
            if (totalSteps == 0) {
                // First time app usage or a phone reset.
                totalSteps = (int) currentTotalSteps;
            } else {
                // If there are total steps then these are used to calculate steps when sensor changes
                float newSteps = currentTotalSteps - totalSteps;
                if (newSteps < 0) {
                    // Device reboot detected, reset daily steps
                    dailySteps = (int) currentTotalSteps;
                } else {
                    dailySteps += (int) newSteps; //step changes are added onto the daily step count
                }
                totalSteps = (int) currentTotalSteps; // total step count is updated
            }

            updateUI(); // UI updated
            saveData(); //data saved

        }
    }

    @SuppressLint("SetTextI18n")
    private void updateUI() {
        int currentSteps = dailySteps; //Converts daily step count to an integer
        steps.setText(String.valueOf(currentSteps)); //daily step count updated
        progressBar.setProgress(currentSteps); //progress bar updated
        float distance = getDistanceStepped(currentSteps);
        distanceStepped.setText(String.format("Distance stepped: %sm", df.format(distance)));

        float burntCalories = getCaloriesBurnt(currentSteps);
        caloriesBurnt.setText(String.format(Locale.getDefault(), "%.0f calories burnt", burntCalories));


    }

    private void saveData() { //saves the number of steps taken
        SharedPreferences sharedPref = getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putFloat(PREF_TOTAL_STEPS, totalSteps); //saves total step count
        editor.putFloat(PREF_DAILY_STEPS, dailySteps); //saves the daily step count
        editor.apply();
    }

    private void loadData() { //loads number of steps taken after restarting app
        SharedPreferences sharedPref = getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        totalSteps = (int) sharedPref.getFloat(PREF_TOTAL_STEPS, 0f); //loads the total step count
        dailySteps = (int) sharedPref.getFloat(PREF_DAILY_STEPS, 0f); //loads the daily step count
        updateUI();
    }

    private float getDistanceStepped(int totalDailySteps) {
        // Calculate distance in meters
        return (float) (totalDailySteps * 74) / 100; // Convert steps to meters
    }

    private float getCaloriesBurnt(int totalDailySteps) {
        return (float) (totalDailySteps*0.04);
    }

    private void loadWeeklySteps() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Fetch last 7 days' data
        db.collection("users")
                .document(userId)
                .collection("Activity")
                .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(7)  // Fetch data for the last 7 days
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Entry> entries = new ArrayList<>();  // List for chart data
                    List<String> dates = new ArrayList<>(); // List for dates
                    int index = 0;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        long steps = document.getLong("steps");
                        entries.add(new Entry(index, steps));  // Add steps data to entries
                        index++;

                        // Get the date from the document
                        String dateString = document.getString("date");
                        if (dateString != null) {
                            // Parse the date and format it to only show month and day
                            try {
                                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                SimpleDateFormat outputFormat = new SimpleDateFormat("MM-dd", Locale.getDefault());
                                Date date = inputFormat.parse(dateString);
                                String formattedDate = outputFormat.format(date);
                                dates.add(0, formattedDate); // Store the formatted date in the list
                            } catch (ParseException e) {
                                Log.e(TAG, "Error parsing date: " + dateString, e);
                            }
                        }
                    }



                    // Create dataset and assign it to the chart
                    LineDataSet dataSet = new LineDataSet(entries, "Steps");
                    dataSet.setLineWidth(2f);
                    LineData lineData = new LineData(dataSet);
                    lineChart.setData(lineData);
                    lineChart.invalidate();  // Refresh chart

                    // Update the x-axis with the fetched dates
                    XAxis xAxis = lineChart.getXAxis();
                    xAxis.setValueFormatter(new IndexAxisValueFormatter(dates)); // Set the dates as labels
                })
                .addOnFailureListener(e -> Log.w(TAG, "Error retrieving step data", e));
    }

    private void setupChart() {
        lineChart.getDescription().setEnabled(false);  // Disable chart description
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);  // Interval of 1 day


        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setEnabled(false);  // Disable the right axis
    }



    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}