package com.example.steppcounter;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class StepResetWorker extends Worker {
    private static final String PREF_TOTAL_STEPS = "total_steps";
    private static final String PREF_DAILY_STEPS = "daily_steps";

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
}