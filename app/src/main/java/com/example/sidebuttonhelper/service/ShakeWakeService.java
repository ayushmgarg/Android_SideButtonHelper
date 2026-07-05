package com.example.sidebuttonhelper.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.sidebuttonhelper.MainActivity;

public class ShakeWakeService extends Service implements SensorEventListener {

    private static final String CHANNEL_ID = "shake_wake_channel";
    private static final int NOTIFICATION_ID = 1001;

    private static final float SHAKE_THRESHOLD_GRAVITY = 2.5f; // will become configurable (sensitivity slider)
    private static final int SHAKE_WINDOW_MS = 1000;
    private static final int REQUIRED_SHAKES = 2;
    private static final int WAKE_DEBOUNCE_MS = 1500;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private int shakeCount = 0;
    private long lastShakeTime = 0;
    private long lastWakeTriggerTime = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
        startForeground(NOTIFICATION_ID, buildNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // ask the OS to restart this service if it gets killed
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float gX = event.values[0] / SensorManager.GRAVITY_EARTH;
        float gY = event.values[1] / SensorManager.GRAVITY_EARTH;
        float gZ = event.values[2] / SensorManager.GRAVITY_EARTH;
        float gForce = (float) Math.sqrt(gX * gX + gY * gY + gZ * gZ);

        if (gForce > SHAKE_THRESHOLD_GRAVITY) {
            long now = SystemClock.elapsedRealtime();
            shakeCount = (now - lastShakeTime > SHAKE_WINDOW_MS) ? 1 : shakeCount + 1;
            lastShakeTime = now;

            if (shakeCount >= REQUIRED_SHAKES) {
                shakeCount = 0;
                if (now - lastWakeTriggerTime > WAKE_DEBOUNCE_MS) {
                    lastWakeTriggerTime = now;
                    wakeScreen();
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void wakeScreen() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powerManager == null) return;

        // SCREEN_BRIGHT_WAKE_LOCK is deprecated but remains the only reliable way
        // to turn the screen on from a background service with no visible Activity.
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                        | PowerManager.ACQUIRE_CAUSES_WAKEUP
                        | PowerManager.ON_AFTER_RELEASE,
                "SideButtonHelper:ShakeWakeLock");
        wakeLock.acquire(3000);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // not used
    }

    private Notification buildNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Shake to Wake", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Side Button Helper active")
                .setContentText("Shake your phone to wake the screen")
                .setSmallIcon(android.R.drawable.ic_lock_power_off)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }
}