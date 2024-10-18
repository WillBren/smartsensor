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
import android.graphics.Color;
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
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.utils.ColorTemplate;

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

    // LineChart, BarChart, PieChart
    private LineChart stepsLineChart;
    private BarChart distanceBarChart;
    private PieChart caloriesPieChart;

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_activity);

        // Initialize UI elements
        progressBar = findViewById(R.id.progressBar);
        steps = findViewById(R.id.steps);
        distanceStepped = findViewById(R.id.distanceStepped);
        caloriesBurnt = findViewById(R.id.caloriesBurnt);

        // Initialize LineChart, BarChart, and PieChart
        stepsLineChart = findViewById(R.id.stepsLineChart);
        distanceBarChart = findViewById(R.id.distanceBarChart);
        caloriesPieChart = findViewById(R.id.caloriesPieChart);

        backButton = findViewById(R.id.back_button);
        setupButtonListeners();

        // Set the chart titles
        TextView stepsChartTitle = findViewById(R.id.stepsChartTitle);
        stepsChartTitle.setText("Weekly Steps Overview");

        TextView distanceChartTitle = findViewById(R.id.distanceChartTitle);
        distanceChartTitle.setText("Weekly Distance Overview");

        TextView caloriesChartTitle = findViewById(R.id.caloriesChartTitle);
        caloriesChartTitle.setText("Weekly Calories Burnt Overview");

        // Set up shared preferences and sensor
        sharedPref = getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        prefListener = (sharedPreferences, key) -> {
            if (PREF_DAILY_STEPS.equals(key)) {
                dailySteps = (int) sharedPreferences.getFloat(PREF_DAILY_STEPS, 0f);
                updateUI();
            }
        };
        sharedPref.registerOnSharedPreferenceChangeListener(prefListener);

        loadData();
        scheduleDailyStepReset();

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        stepSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER); //sets up step count sensor

        setupCharts();  // Configure all charts
        loadWeeklyData();  // Retrieve and plot weekly data

        // Check if step sensor is available on the device
        if (stepSensor == null) {
            Toast.makeText(this, "Step counter sensor is not present on this device", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Step counter sensor is available", Toast.LENGTH_SHORT).show();
        }

        // Check for user permission to access the step counter sensor
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
            Toast.makeText(StepActivity.this, "Back button clicked!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(StepActivity.this, MainActivity.class);
            startActivity(intent);
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

            if (totalSteps == 0) {
                // First time app usage or a phone reset.
                totalSteps = (int) currentTotalSteps;
            } else {
                float newSteps = currentTotalSteps - totalSteps;
                if (newSteps < 0) {
                    dailySteps = (int) currentTotalSteps;
                } else {
                    dailySteps += (int) newSteps;
                }
                totalSteps = (int) currentTotalSteps;
            }

            updateUI();
            saveData();
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateUI() {
        int currentSteps = dailySteps;
        steps.setText(String.valueOf(currentSteps));
        progressBar.setProgress(currentSteps);
        float distance = getDistanceStepped(currentSteps);
        distanceStepped.setText(String.format("Distance stepped: %sm", df.format(distance)));

        float burntCalories = getCaloriesBurnt(currentSteps);
        caloriesBurnt.setText(String.format(Locale.getDefault(), "%.0f calories burnt", burntCalories));
    }

    private void saveData() {
        SharedPreferences sharedPref = getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putFloat(PREF_TOTAL_STEPS, totalSteps);
        editor.putFloat(PREF_DAILY_STEPS, dailySteps);
        editor.apply();
    }

    private void loadData() {
        SharedPreferences sharedPref = getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        totalSteps = (int) sharedPref.getFloat(PREF_TOTAL_STEPS, 0f);
        dailySteps = (int) sharedPref.getFloat(PREF_DAILY_STEPS, 0f);
        updateUI();
    }

    private float getDistanceStepped(int totalDailySteps) {
        return (float) (totalDailySteps * 0.74);
    }

    private float getCaloriesBurnt(int totalDailySteps) {
        return (float) (totalDailySteps * 0.04);
    }

    private void loadWeeklyData() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        db.collection("users")
                .document(userId)
                .collection("Activity")
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(7)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Entry> stepsEntries = new ArrayList<>();
                    List<BarEntry> distanceEntries = new ArrayList<>();
                    List<PieEntry> caloriesEntries = new ArrayList<>();
                    List<String> dates = new ArrayList<>();
                    int index = 0;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        long steps = document.getLong("steps");
                        float distance = getDistanceStepped((int) steps);
                        float calories = getCaloriesBurnt((int) steps);

                        stepsEntries.add(new Entry(index, steps));
                        distanceEntries.add(new BarEntry(index, distance));
                        caloriesEntries.add(new PieEntry(calories, formatDate(document.getString("date"))));

                        String dateString = document.getString("date");
                        if (dateString != null) {
                            try {
                                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                SimpleDateFormat outputFormat = new SimpleDateFormat("MM-dd", Locale.getDefault());
                                Date date = inputFormat.parse(dateString);
                                String formattedDate = outputFormat.format(date);
                                dates.add(0, formattedDate);
                            } catch (ParseException e) {
                                Log.e(TAG, "Error parsing date: " + dateString, e);
                            }
                        }

                        index++;
                    }

                    // Populate LineChart for steps
                    LineDataSet stepsDataSet = new LineDataSet(stepsEntries, "Steps");
                    LineData stepsLineData = new LineData(stepsDataSet);
                    stepsLineChart.setData(stepsLineData);
                    stepsLineChart.invalidate();

                    // Populate BarChart for distance with rainbow colors for each day
                    BarDataSet distanceDataSet = new BarDataSet(distanceEntries, "Distance (m)");
                    // Define rainbow colors for 7 bars (you can add more colors if necessary)
                    int[] rainbowColors = {
                            Color.RED, Color.YELLOW, Color.GREEN, Color.BLUE, Color.CYAN, Color.MAGENTA, Color.DKGRAY
                    };
                    distanceDataSet.setColors(rainbowColors);  // Apply the rainbow color scheme to bars
                    BarData distanceBarData = new BarData(distanceDataSet);
                    distanceBarChart.setData(distanceBarData);
                    distanceBarChart.invalidate();

                    // Populate PieChart for calories with rainbow colors for each slice
                    PieDataSet caloriesDataSet = new PieDataSet(caloriesEntries, "Calories Burnt");
                    // Apply the same rainbow color scheme to the pie chart
                    caloriesDataSet.setColors(rainbowColors);  // Use the same rainbow colors
                    PieData caloriesPieData = new PieData(caloriesDataSet);
                    caloriesPieData.setDrawValues(false);  // Remove default value labels for clarity
                    caloriesPieChart.setData(caloriesPieData);
                    caloriesPieChart.invalidate();

                    // Update the x-axis labels for both the LineChart and BarChart
                    XAxis stepsXAxis = stepsLineChart.getXAxis();
                    stepsXAxis.setValueFormatter(new IndexAxisValueFormatter(dates));

                    XAxis distanceXAxis = distanceBarChart.getXAxis();
                    distanceXAxis.setValueFormatter(new IndexAxisValueFormatter(dates));
                })
                .addOnFailureListener(e -> Log.w(TAG, "Error retrieving step data", e));
    }

    private void setupCharts() {
        configureLineChart(stepsLineChart, "Steps");
        configureBarChart(distanceBarChart, "Distance (m)");
        configurePieChart(caloriesPieChart, "Calories Burnt");
    }

    private void configureLineChart(LineChart chart, String label) {
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);
    }

    private void configureBarChart(BarChart chart, String label) {
        chart.getDescription().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setFitBars(true);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);
    }

    private void configurePieChart(PieChart chart, String label) {
        chart.getDescription().setEnabled(false);  // Disable chart description
        chart.setTouchEnabled(true);  // Enable touch gestures
        chart.setDragDecelerationFrictionCoef(0.95f);  // Set deceleration friction
        chart.setDrawHoleEnabled(true);  // Enable the center hole
        chart.setHoleColor(Color.WHITE);  // Set the hole color to white
        chart.setTransparentCircleRadius(61f);  // Set the transparency circle radius

        // Hide the value labels (so only the dates are shown in the slices)
        chart.setDrawEntryLabels(true);  // Enable showing entry labels (dates)

        chart.invalidate();  // Refresh the chart
    }

    private String formatDate(String dateString) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MM-dd", Locale.getDefault());
            Date date = inputFormat.parse(dateString);
            return outputFormat.format(date);
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date: " + dateString, e);
            return dateString;
        }
    }

    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
