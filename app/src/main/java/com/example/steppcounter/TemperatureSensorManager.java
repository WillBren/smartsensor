package com.example.steppcounter;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

public class TemperatureSensorManager implements SensorEventListener {

    private final SensorManager sensorManager;
    final Sensor temperatureSensor;
    private final TemperatureCallback callback;
    private static final String CHANNEL_ID = "Temperature_Channel";
    private final Context context;
    private static final int NOTIFICATION_PERMISSION_CODE = 123;
    private final Activity activity;  // Add this field
    private boolean isNotificationShowing = false;
    private static final int NOTIFICATION_ID = 1;
    private int currentTempRange = 0;

    public TemperatureSensorManager(Context context, TemperatureCallback callback, Activity activity) {
        this.callback = callback;
        this.context = context;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        this.activity = activity;
        temperatureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        createNotificationChannel(); // Create channel for notifications

        if (temperatureSensor == null) {
            Log.e("TemperatureSensor", "Ambient temperature sensor not available on this device");
        }

        createNotificationChannel();
        checkNotificationPermission();  // Add this method call
    }

    // Add this method to check and request notification permission
    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, "android.permission.POST_NOTIFICATIONS")
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity,
                        new String[]{"android.permission.POST_NOTIFICATIONS"},
                        NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    // Add this method to handle the permission result
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, you can send notifications now
                Log.d("TemperatureSensor", "Notification permission granted");
            } else {
                // Permission denied, handle this case (e.g., show a message to the user)
                Log.d("TemperatureSensor", "Notification permission denied");
            }
        }
    }

    // Method to handle notification logic
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void sendTemperatureNotification(float temperature) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Check for permission before sending notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, "android.permission.POST_NOTIFICATIONS")
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d("TemperatureSensor", "Cannot send notification: permission not granted");
                return;
            }
        }

        int newTempRange;
        if (temperature >= 35) {
            newTempRange = 3;
        } else if (temperature >= 30) {
            newTempRange = 2;
        } else if (temperature >= 25) {
            newTempRange = 1;
        } else {
            newTempRange = 0;
        }

        // Only send a new notification if the temperature range has changed
        if (newTempRange != currentTempRange) {
            currentTempRange = newTempRange;

            if (currentTempRange > 0) {
                String title = "Hydration Reminder";
                String message;
                int priority;

                switch (currentTempRange) {
                    case 3:
                        message = "High risk of dehydration! Keep Hydrated. Temperature is above 35°C!";
                        priority = NotificationCompat.PRIORITY_HIGH;
                        break;
                    case 2:
                        message = "Hydration Reminder. Temperature is above 30°C!";
                        priority = NotificationCompat.PRIORITY_DEFAULT;
                        break;
                    default:
                        message = "Stay Hydrated. Temperature is above 25°C";
                        priority = NotificationCompat.PRIORITY_LOW;
                        break;
                }

                Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setPriority(priority)
                        .build();

                notificationManager.notify(NOTIFICATION_ID, notification);
                isNotificationShowing = true;
            } else if (isNotificationShowing) {
                // Cancel the notification if temperature is below 25°C and a notification was showing
                notificationManager.cancel(NOTIFICATION_ID);
                isNotificationShowing = false;
            }
        }
    }

    // Create a notification channel
    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Temperature Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Notifications for temperature-related hydration reminders");

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                sendTemperatureNotification(temperature);
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

