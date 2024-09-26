package com.example.steppcounter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorListener;
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


public class StepActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager mSensorManager = null;
    private Sensor stepSensor;
    private int totalSteps = 0;
    private int previewsTotalSteps=0;
    private ProgressBar progressBar;
    private TextView steps;
    private MaterialButton backButton;
    private TextView distanceStepped;
    private final DecimalFormat df = new DecimalFormat("#.##");



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

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        stepSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER); //sets up step count sensor

        //checks if step sensor is available on the device
        if (stepSensor == null) {
            Toast.makeText(this, "Step counter sensor is not present on this device", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Step counter sensor is available", Toast.LENGTH_SHORT).show();
        }


        AlarmScheduler.scheduleMidnightReset(this);
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



    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            totalSteps = (int) event.values[0];
            int currentSteps;
            if (totalSteps <= 0) {
                previewsTotalSteps = totalSteps;
                currentSteps = totalSteps;
            } else {
                currentSteps = totalSteps-previewsTotalSteps;
            }
            steps.setText(String.valueOf(currentSteps));
            // changes total steps when sensor reacts
            progressBar.setProgress(currentSteps);

            // changes total distance when step count changes
            float distance = getDistanceStepped(currentSteps);
            distanceStepped.setText(String.format("Distance stepped: %skm", df.format(distance)));

            //testing purposes- step counts
            TextView previewsTotalStepsView = findViewById(R.id.previewsTotalSteps);
            TextView totalStepsView = findViewById(R.id.totalSteps);
            previewsTotalStepsView.setText("Previous Total Steps: " + previewsTotalSteps);
            totalStepsView.setText("Total Steps: " + totalSteps);
        }

    }

    private void resetSteps() {
        // method to reset steps after long click (note: reset isn't recognised when app is restarted)
        steps.setOnClickListener(v -> Toast.makeText(StepActivity.this, "Long press to reset steps", Toast.LENGTH_SHORT).show());

        steps.setOnLongClickListener(v -> {
            previewsTotalSteps = totalSteps;
            steps.setText("0");
            progressBar.setProgress(0);
            saveData();
            return true;
        });
    }

    private void saveData() { //saves the number of steps taken
        SharedPreferences sharedPref = getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putFloat("key1", previewsTotalSteps);
        editor.apply();
    }

    private void loadData() { //loads number of steps taken after restarting app
        SharedPreferences sharedPref = getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        previewsTotalSteps = (int) sharedPref.getFloat("key1", 0f);
    }

    private float getDistanceStepped(int totalDailySteps) {
        return (float) (totalDailySteps * 74) /100000;
    }



    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    /*private void clearSharedPreferences() {
        SharedPreferences sharedPref = getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.clear();  // Clear all the data
        editor.apply();
    } */
}
