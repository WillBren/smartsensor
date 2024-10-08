package com.example.steppcounter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

// Firebase imports
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StepResetReceiver extends BroadcastReceiver {
    private static final String PREF_TOTAL_STEPS = "total_steps";
    private static final String PREF_DAILY_STEPS = "daily_steps";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Initialize Firebase
        FirebaseApp.initializeApp(context);

        resetDailySteps(context);
        Log.d("StepResetReceiver", "Daily step count reset successfully.");
    }

    private void resetDailySteps(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        float totalSteps = sharedPref.getFloat(PREF_TOTAL_STEPS, 0f);
        float dailySteps = sharedPref.getFloat(PREF_DAILY_STEPS, 0f);

        // Upload dailySteps to Firebase
        uploadDailyStepsToFirebase(dailySteps);

        // Reset daily steps to 0
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putFloat(PREF_DAILY_STEPS, 0f);
        editor.putFloat(PREF_TOTAL_STEPS, totalSteps); // Keep total steps unchanged
        editor.apply();

        Log.d("StepResetReceiver", "Daily steps reset to 0. Total steps: " + totalSteps);
    }

    private void uploadDailyStepsToFirebase(float dailySteps) {
        // Get a reference to the database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("daily_steps");

        // Get the current date as a key
        String dateKey = getCurrentDate();

        // Upload the dailySteps value under the date key
        myRef.child(dateKey).setValue(dailySteps)
                .addOnSuccessListener(aVoid -> {
                    // Data uploaded successfully
                    Log.d("Firebase", "Daily steps uploaded: " + dailySteps);
                })
                .addOnFailureListener(e -> {
                    // Failed to upload data
                    Log.e("Firebase", "Failed to upload daily steps", e);
                });
    }

    private String getCurrentDate() {
        // Returns the current date in yyyy-MM-dd format
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }
}
