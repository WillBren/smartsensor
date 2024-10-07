package com.example.steppcounter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

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

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putFloat(PREF_DAILY_STEPS, 0f); // Reset daily steps to 0
        editor.putFloat(PREF_TOTAL_STEPS, totalSteps); // Keep total steps unchanged
        editor.apply();

        Log.d("StepResetReceiver", "Daily steps reset to 0. Total steps: " + totalSteps);
    }
}
