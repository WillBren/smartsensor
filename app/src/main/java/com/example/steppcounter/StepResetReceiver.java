package com.example.steppcounter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class StepResetReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Reset steps
        SharedPreferences sharedPref = context.getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putFloat("key1", 0f); // Reset the saved steps to 0
        editor.apply();

        // Reschedule the alarm for the next day
        AlarmScheduler.scheduleMidnightReset(context);
    }
}
