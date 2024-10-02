package com.example.steppcounter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import android.hardware.SensorEventListener;
import com.google.android.material.button.MaterialButton;
import java.text.DecimalFormat;
import java.util.Calendar;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;


public class StepActivity extends AppCompatActivity implements SensorEventListener {
    private float totalSteps;
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
    private float dailySteps;

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

    }

    //method schedules the daily reset for 12:00am daily
    private void scheduleDailyStepReset() {

        Calendar current = Calendar.getInstance();
        Calendar nextResetTime = Calendar.getInstance();
        nextResetTime.set(Calendar.HOUR_OF_DAY, 0);
        nextResetTime.set(Calendar.MINUTE, 0);
        nextResetTime.set(Calendar.SECOND, 0);
        nextResetTime.set(Calendar.MILLISECOND, 0);

        // If it's already past 12:00 AM today, schedule for the next day
        if (nextResetTime.before(current)) {
            nextResetTime.add(Calendar.DAY_OF_MONTH, 1);
        }

        long initialDelay = nextResetTime.getTimeInMillis() - current.getTimeInMillis();

        // Schedule periodic work to run every 24 hours
        PeriodicWorkRequest resetRequest = new PeriodicWorkRequest.Builder(StepResetWorker.class, 24, TimeUnit.HOURS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build();

        // Enqueue the work
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "stepResetWork",
                ExistingPeriodicWorkPolicy.REPLACE,
                resetRequest);
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

        if (stepSensor == null) {
            Toast.makeText(this, "Device has no sensor", Toast.LENGTH_SHORT).show();
        }
        else {
            mSensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    protected void onPause() {
        super.onPause();

    }

    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            float currentTotalSteps = event.values[0];

            if (totalSteps == 0) {
                // First time app usage or a phone reset.
                totalSteps = currentTotalSteps;
            } else {
                // If there are total steps then these are used to calculate steps when sensor changes
                float newSteps = currentTotalSteps - totalSteps;
                if (newSteps < 0) {
                    // Device reboot detected, reset daily steps
                    dailySteps = currentTotalSteps;
                } else {
                    dailySteps += newSteps; //step changes are added onto the daily step count
                }
                totalSteps = currentTotalSteps; // total step count is updated
            }

            updateUI(); // UI updated
            saveData(); //data saved
        }
    }

    private void updateUI() {
        int currentSteps = (int) dailySteps; //Converts daily step count to an integer
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
        totalSteps = sharedPref.getFloat(PREF_TOTAL_STEPS, 0f); //loads the total step count
        dailySteps = sharedPref.getFloat(PREF_DAILY_STEPS, 0f); //loads the daily step count
        updateUI();
    }

    private float getDistanceStepped(int totalDailySteps) {
        return (float) (totalDailySteps * 74) /100000;
    }

    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
