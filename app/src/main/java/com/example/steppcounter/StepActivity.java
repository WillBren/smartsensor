package com.example.steppcounter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import android.hardware.SensorEventListener;
import com.google.android.material.button.MaterialButton;

public class StepActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager mSensorManager = null;
    private Sensor stepSensor;
    private int totalSteps = 0;
    private int previewsTotalSteps=0;
    private ProgressBar progressBar;
    private TextView steps;
    private MaterialButton backButton;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.fragment_activity);


        clearSharedPreferences();  // Clear old data

        progressBar= findViewById(R.id.progressBar);
        steps = findViewById(R.id.steps);

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
    }


    private void setupButtonListeners() {
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(StepActivity.this, "Activity button clicked!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(StepActivity.this, MainActivity.class);
                startActivity(intent);
                // tells button to go from step activity to the main activity
            }
        });
    }



    protected void onResume() {
        super.onResume();

        if (stepSensor == null) {
            Toast.makeText(this, "Device has no sensor", Toast.LENGTH_SHORT).show();
        }
        else {
            mSensorManager.registerListener((SensorEventListener) this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener((SensorListener) this);
    }

    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            totalSteps = (int) event.values[0];
            int currentSteps = totalSteps-previewsTotalSteps;
            steps.setText(String.valueOf(currentSteps));
            // changes total steps when sensor reacts
            progressBar.setProgress(currentSteps);
        }

    }

    private void resetSteps() {
        steps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(StepActivity.this, "Long press to reset steps", Toast.LENGTH_SHORT).show();
            } // method to reset steps after long click (note: reset isn't recognised when app is restarted)
        });

        steps.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                previewsTotalSteps = totalSteps;
                steps.setText("0");
                progressBar.setProgress(0);
                saveData();
                return true;
            }
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
        int savedNumber = (int) sharedPref.getFloat("key1", 0f);
        previewsTotalSteps = savedNumber;
    }


    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void clearSharedPreferences() {
        SharedPreferences sharedPref = getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.clear();  // Clear all the data
        editor.apply();
    }
}
