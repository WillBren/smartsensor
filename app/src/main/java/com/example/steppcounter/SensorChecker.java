package com.example.steppcounter;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.widget.Toast;

public class SensorChecker {

    private final SensorManager sensorManager;
    private final Context context;  // Save the context to use for Toast or other UI updates

    public SensorChecker(Context context) {
        this.context = context;
        // Initialize SensorManager properly using the provided context
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    }

    // Method to check if the Significant Motion Sensor is available
    public void checkForSignificantMotionSensor() {
        Sensor significantMotionSensor = sensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION);

        if (significantMotionSensor != null) {
            // Significant Motion Sensor is available, display a Toast
            Toast.makeText(context, "Significant Motion Sensor is available", Toast.LENGTH_SHORT).show();
        } else {
            // Significant Motion Sensor is not available, display a Toast
            Toast.makeText(context, "Significant Motion Sensor is not available", Toast.LENGTH_SHORT).show();
        }
    }

    // Method to check if the Heart Rate Sensor is available
    public void checkForHeartRateSensor() {
        Sensor heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);

        if (heartRateSensor != null) {
            // Heart Rate Sensor is available, display a Toast
            Toast.makeText(context, "Heart Rate Sensor is available", Toast.LENGTH_SHORT).show();
        } else {
            // Heart Rate Sensor is not available, display a Toast
            Toast.makeText(context, "Heart Rate Sensor is not available", Toast.LENGTH_SHORT).show();
        }
    }
}
