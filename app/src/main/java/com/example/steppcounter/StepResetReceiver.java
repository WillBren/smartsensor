package com.example.steppcounter;

import static android.content.ContentValues.TAG;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

// Firebase imports
import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class StepResetReceiver extends BroadcastReceiver {
    private static final String PREF_TOTAL_STEPS = "total_steps";
    private static final String PREF_DAILY_STEPS = "daily_steps";

    @Override
    public void onReceive(Context context, Intent intent) {
        resetDailySteps(context);
        Log.d("StepResetReceiver", "Daily step count reset successfully.");
    }

    private void resetDailySteps(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        float totalSteps = sharedPref.getFloat(PREF_TOTAL_STEPS, 0f);
        float dailySteps = sharedPref.getFloat(PREF_DAILY_STEPS, 0f);

        uploadDailyStepsToFirebase(dailySteps);
        Log.d("StepResetReceiver", "Steps uploaded to database: " + dailySteps);

        // Reset daily steps to 0
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putFloat(PREF_DAILY_STEPS, 0f);
        editor.putFloat(PREF_TOTAL_STEPS, totalSteps); // Keep total steps unchanged
        editor.apply();

        Log.d("StepResetReceiver", "Daily steps reset to 0. Total steps: " + totalSteps);
    }

    private void uploadDailyStepsToFirebase(float dailySteps) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            String userId = currentUser.getUid();  // Get the user's UID

            // Create a map for storing step data
            Map<String, Object> activity = new HashMap<>();
            activity.put("steps", dailySteps);
            activity.put("date", getCurrentDate());
            double distanceInMeters = (dailySteps * 74) / 100.0; // Convert steps to meters
            activity.put("distance", distanceInMeters); // Store the distance in meters
            activity.put("calories burnt", Math.round((dailySteps * 0.04)));  // Calculate calories burnt

            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // Use the current date as the document ID for the subcollection
            String currentDate = getCurrentDate();

            // Upload steps to the current user's document in the "Activity" subcollection
            db.collection("users")
                    .document(userId)  // Each user gets their own document identified by their UID
                    .collection("Activity")  // Activity subcollection for each user
                    .document(currentDate)  // Use current date as the document ID
                    .set(activity)  // Use set() to create/overwrite the document with current date as ID
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "DocumentSnapshot successfully written for date: " + currentDate);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error writing document", e);
                        }
                    });
        } else {
            Log.w(TAG, "No authenticated user found. Unable to upload steps.");
        }
    }


    private String getCurrentDate() {
        // Returns the current date in yyyy-MM-dd format
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }
}
