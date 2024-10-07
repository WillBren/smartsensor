package com.example.steppcounter;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.hardware.SensorEventListener;
import com.google.android.material.button.MaterialButton;
import java.text.DecimalFormat;
import java.util.Calendar;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;


public class StepActivity extends AppCompatActivity implements SensorEventListener {
    private int totalSteps;
    private SensorManager mSensorManager = null;
    private Sensor stepSensor;
    private int previewsTotalSteps;
    private ProgressBar progressBar;
    private TextView steps;
    private MaterialButton backButton;
    private TextView distanceStepped;
    private final DecimalFormat df = new DecimalFormat("#.##");
    private static final String PREF_TOTAL_STEPS = "total_steps";
    private static final String PREF_DAILY_STEPS = "daily_steps";
    private int dailySteps;
    private int currentTotalSteps;
    private SharedPreferences sharedPref;
    private SharedPreferences.OnSharedPreferenceChangeListener prefListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.fragment_activity);

        progressBar= findViewById(R.id.progressBar);
        steps = findViewById(R.id.steps);
        distanceStepped = findViewById(R.id.distanceStepped);

        backButton = findViewById(R.id.back_button); //sets up back button to id in fragment_activity xml file
        setupButtonListeners();

        sharedPref = getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        prefListener = (sharedPreferences, key) -> {
            if (PREF_DAILY_STEPS.equals(key)) {
                dailySteps = (int) sharedPreferences.getFloat(PREF_DAILY_STEPS, 0f);
                updateUI();
            }
        };

        resetSteps();
        loadData();
        scheduleDailyStepReset();

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        stepSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER); //sets up step count sensor

        //checks if step sensor is available on the device
        if (stepSensor == null) {
            Toast.makeText(this, "Step counter sensor is not present on this device", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Step counter sensor is available", Toast.LENGTH_SHORT).show();
        }

        //Checks for user permission to access the step counter sensor
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACTIVITY_RECOGNITION}, 1);
        }





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
                    dailySteps += newSteps; //step changes are added onto the daily step count
                }
                totalSteps = (int) currentTotalSteps; // total step count is updated
            }

            updateUI(); // UI updated
            saveData(); //data saved
        }
    }

    private void updateUI() {
        int currentSteps = dailySteps; //Converts daily step count to an integer
        steps.setText(String.valueOf(currentSteps)); //daily step count updated
        progressBar.setProgress(currentSteps); //progress bar updated
        float distance = getDistanceStepped(currentSteps);
        distanceStepped.setText(String.format("Distance stepped: %skm", df.format(distance)));

        // For testing purposes
        TextView previewsTotalStepsView = findViewById(R.id.previewsTotalSteps);
        TextView totalStepsView = findViewById(R.id.totalSteps);
        previewsTotalStepsView.setText("Daily Steps: " + dailySteps);
        totalStepsView.setText("Total Steps: " + totalSteps);
    }

    private void resetSteps() {
        // method to reset steps after long click (note: reset isn't recognised when app is restarted)
        steps.setOnClickListener(v -> Toast.makeText(StepActivity.this, "Long press to reset steps", Toast.LENGTH_SHORT).show());

        steps.setOnLongClickListener(v -> {
            previewsTotalSteps = (int) totalSteps;
            steps.setText("0");
            progressBar.setProgress(0);
            saveData();
            return true;
        });
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
        return (float) (totalDailySteps * 74) /100000;
    }

    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}