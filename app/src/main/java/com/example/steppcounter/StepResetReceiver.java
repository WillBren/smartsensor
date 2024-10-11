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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
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

    //Receives the values and calls for reset
    @Override
    public void onReceive(Context context, Intent intent) {
        //Initialize Firebase
        //FirebaseApp.initializeApp(context);

        resetDailySteps(context);
        Log.d("StepResetReceiver", "Daily step count reset successfully.");
    }

    //Resets the daily steps
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

        // Create a new user with a first and last name
        Map<String, Object> activity = new HashMap<>();
        activity.put("steps", dailySteps);
        activity.put("date", getCurrentDate());
        activity.put("distance", (dailySteps * 74) /100000);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Activity")
                .add(activity)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding document", e);
                    }
                });

    }

    private String getCurrentDate() {
        // Returns the current date in yyyy-MM-dd format
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }
}
