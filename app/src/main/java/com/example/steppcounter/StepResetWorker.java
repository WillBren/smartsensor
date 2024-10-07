package com.example.steppcounter;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.ForegroundInfo;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class StepResetWorker extends Worker {
    private static final String PREF_TOTAL_STEPS = "total_steps";
    private static final String PREF_DAILY_STEPS = "daily_steps";
    public static final String CHANNEL_ID = "step_reset_channel";
    public static final int NOTIFICATION_ID = 1;

    public StepResetWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        resetDailySteps();
        Log.d("StepResetWorker", "Daily step count reset successfully."); //log account to confirm if step count has reset
        return Result.success(); //returns success if step reset has worked
    }

    private void resetDailySteps() {
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences("MyPref", Context.MODE_PRIVATE); //retrieves current preferences
        float totalSteps = sharedPref.getFloat(PREF_TOTAL_STEPS, 0f); //accesses total steps through the key (PREF_TOTAL_STEPS)

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putFloat(PREF_DAILY_STEPS, 0f); // Reset daily steps to 0
        editor.putFloat(PREF_TOTAL_STEPS, totalSteps); // Keep total steps unchanged
        editor.apply();

        Log.d("StepResetWorker", "Daily steps reset to 0. Total steps: " + totalSteps);
    }

    @Override
    public void onStopped() {
        super.onStopped();
        // Handle worker stopped scenario
    }

    @NonNull
    @Override
    public ForegroundInfo getForegroundInfo() {
        // Create the notification channel (for Android O and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Step Reset Channel";
            String description = "Channel for step reset notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getApplicationContext().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        // Build the notification
        String title = "Step Counter";
        String cancel = "Cancel";

        Notification notification = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setContentTitle(title)
                .setTicker("Resetting steps")
                .setSmallIcon(android.R.drawable.ic_popup_sync) // Make sure this icon exists in your drawable folder
                .setOngoing(true)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, cancel, getCancelIntent())
                .build();

        return new ForegroundInfo(NOTIFICATION_ID, notification);
    }


    private PendingIntent getCancelIntent() {
        return WorkManager.getInstance(getApplicationContext()).createCancelPendingIntent(getId());
    }
}