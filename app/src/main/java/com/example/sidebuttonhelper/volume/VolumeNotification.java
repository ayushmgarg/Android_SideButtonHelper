package com.example.sidebuttonhelper.volume;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

/**
 * Persistent (ongoing) notification with inline -/+ buttons for one configurable
 * stream (default Media — most used on the go; changeable from Settings in Phase 10).
 * Rides on the existing process kept alive by ShakeWakeService — doesn't need its own
 * foreground service, just posts/updates an ongoing NotificationCompat entry.
 */
public class VolumeNotification {

    public static final String CHANNEL_ID = "volume_control_channel";
    public static final int NOTIFICATION_ID = 2001;

    public static final String ACTION_VOLUME_UP = "com.example.sidebuttonhelper.ACTION_VOLUME_UP";
    public static final String ACTION_VOLUME_DOWN = "com.example.sidebuttonhelper.ACTION_VOLUME_DOWN";
    public static final String EXTRA_STREAM_TYPE = "extra_stream_type";

    private static final String PREFS_NAME = "volume_notification_prefs";
    private static final String KEY_STREAM_TYPE = "notification_stream_type";

    private final Context context;
    private final VolumeController volumeController;

    public VolumeNotification(Context context) {
        this.context = context.getApplicationContext();
        this.volumeController = new VolumeController(this.context);
    }

    /** Call once at app startup (Phase 9, MainActivity/Application onCreate). */
    public static void createChannel(Context context) {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Volume Control", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Persistent volume control with quick +/- buttons");
        channel.setShowBadge(false);
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    public int getConfiguredStream() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_STREAM_TYPE, AudioManager.STREAM_MUSIC);
    }

    /** Changes which stream the inline buttons control, persists it, and refreshes. */
    public void setConfiguredStream(int streamType) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putInt(KEY_STREAM_TYPE, streamType).apply();
        show();
    }

    /** Builds/refreshes the ongoing notification with the current volume level. */
    public void show() {
        int streamType = getConfiguredStream();
        int current = volumeController.getStreamVolume(streamType);
        int max = volumeController.getStreamMaxVolume(streamType);

        PendingIntent downIntent = actionPendingIntent(ACTION_VOLUME_DOWN, streamType);
        PendingIntent upIntent = actionPendingIntent(ACTION_VOLUME_UP, streamType);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
                .setContentTitle(streamLabel(streamType) + " Volume")
                .setContentText(current + " / " + max)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .addAction(android.R.drawable.ic_media_previous, "-", downIntent)
                .addAction(android.R.drawable.ic_media_next, "+", upIntent);

        postNotification(builder);
    }

    /**
     * API 33+ requires the runtime POST_NOTIFICATIONS permission before notify() can
     * succeed; calling it unchecked is a lint MissingPermission error and can silently
     * no-op (or, on some OEM builds, throw) if the user denied/revoked it. Below API 33
     * no runtime grant is needed, so it's treated as always allowed.
     */
    private boolean hasPostNotificationsPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true;
        }
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void postNotification(NotificationCompat.Builder builder) {
        if (!hasPostNotificationsPermission()) {
            // Onboarding (Phase 8) is responsible for requesting this; nothing to show
            // until the user grants it.
            return;
        }
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build());
        } catch (SecurityException e) {
            // Defensive: some OEM ROMs misreport/revoke this permission inconsistently
            // even after checkSelfPermission() returns granted.
        }
    }

    public void cancel() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID);
    }

    private PendingIntent actionPendingIntent(String action, int streamType) {
        Intent intent = new Intent(context, VolumeActionReceiver.class);
        intent.setAction(action);
        intent.putExtra(EXTRA_STREAM_TYPE, streamType);
        int requestCode = ACTION_VOLUME_UP.equals(action) ? 1 : 2;
        return PendingIntent.getBroadcast(context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static String streamLabel(int streamType) {
        if (streamType == AudioManager.STREAM_RING) return "Ring";
        if (streamType == AudioManager.STREAM_NOTIFICATION) return "Notification";
        if (streamType == AudioManager.STREAM_ALARM) return "Alarm";
        if (streamType == AudioManager.STREAM_VOICE_CALL) return "Call";
        return "Media";
    }
}