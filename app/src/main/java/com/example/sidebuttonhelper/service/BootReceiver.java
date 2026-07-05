package com.example.sidebuttonhelper.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;

import androidx.core.content.ContextCompat;

import com.example.sidebuttonhelper.volume.VolumeBubbleService;

public class BootReceiver extends BroadcastReceiver {

    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_BUBBLE_ENABLED = "pref_bubble_enabled";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        boolean isBootAction = Intent.ACTION_BOOT_COMPLETED.equals(action)
                || "android.intent.action.QUICKBOOT_POWERON".equals(action)
                || "com.htc.intent.action.QUICKBOOT_POWERON".equals(action);
        if (!isBootAction) return;

        // Shake-to-wake is the app's core purpose — always restart it.
        ContextCompat.startForegroundService(context, new Intent(context, ShakeWakeService.class));

        // Bubble restarts only if the user had it on and overlay permission is still
        // granted (user could have revoked it manually in system settings).
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean bubbleEnabled = prefs.getBoolean(KEY_BUBBLE_ENABLED, true);
        if (bubbleEnabled && Settings.canDrawOverlays(context)) {
            context.startService(new Intent(context, VolumeBubbleService.class));
        }

        // Re-arm the watchdog in case the reboot cleared WorkManager's schedule.
        ServiceWatchdogWorker.schedule(context);
    }
}