package com.example.steppcounter;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class TemperatureSensorManager implements SensorEventListener {

    private final SensorManager sensorManager;
    final Sensor temperatureSensor;
    private final TemperatureCallback callback;

    public TemperatureSensorManager(Context context, TemperatureCallback callback) {
        this.callback = callback;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        temperatureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
    }

    public void startListening() {
        if (temperatureSensor != null) {
            sensorManager.registerListener(this, temperatureSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    public void stopListening() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            float temperature = event.values[0];
            if (callback != null) {
                callback.onTemperatureChanged(temperature);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Handle sensor accuracy changes here if necessary
    }

    public interface TemperatureCallback {
        void onTemperatureChanged(float temperature);
    }
}

