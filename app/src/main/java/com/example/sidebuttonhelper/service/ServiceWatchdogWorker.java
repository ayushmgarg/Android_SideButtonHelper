package com.example.sidebuttonhelper.service;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.sidebuttonhelper.volume.VolumeBubbleService;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Periodic safety net (WorkManager minimum interval: 15 min). Some OEMs
 * (Xiaomi/Samsung/Vivo/Oppo/etc.) kill background services aggressively even with
 * battery-optimization exemption granted — this checks core services are alive and
 * restarts them if not.
 */
public class ServiceWatchdogWorker extends Worker {

    private static final String WORK_NAME = "service_watchdog";
    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_BUBBLE_ENABLED = "pref_bubble_enabled";

    public ServiceWatchdogWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    /** Call from BootReceiver and from onboarding/MainActivity once setup completes. */
    public static void schedule(Context context) {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                ServiceWatchdogWorker.class, 15, TimeUnit.MINUTES)
                .build();
        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request);
    }

    public static void cancel(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();

        if (!isServiceRunning(context, ShakeWakeService.class.getName())) {
            ContextCompat.startForegroundService(context, new Intent(context, ShakeWakeService.class));
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean bubbleEnabled = prefs.getBoolean(KEY_BUBBLE_ENABLED, true);
        boolean bubbleRunning = isServiceRunning(context, VolumeBubbleService.class.getName());
        if (bubbleEnabled && !bubbleRunning && Settings.canDrawOverlays(context)) {
            context.startService(new Intent(context, VolumeBubbleService.class));
        }

        return Result.success();
    }

    @SuppressWarnings("deprecation")
    private boolean isServiceRunning(Context context, String serviceClassName) {
        // getRunningServices() is deprecated for general use, but API 26+ restricts its
        // results to the calling app's own services — exactly this check. Third-party
        // apps can no longer use it to snoop on other apps.
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager == null) return false;
        List<ActivityManager.RunningServiceInfo> services =
                activityManager.getRunningServices(Integer.MAX_VALUE);
        for (ActivityManager.RunningServiceInfo info : services) {
            if (serviceClassName.equals(info.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}