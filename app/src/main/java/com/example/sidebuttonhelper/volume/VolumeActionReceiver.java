package com.example.sidebuttonhelper.volume;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.VibrationEffect;
import android.os.Vibrator;

/**
 * Receives taps on VolumeNotification's inline -/+ buttons, adjusts the configured
 * stream, gives vibration feedback, and refreshes the notification's displayed level.
 */
public class VolumeActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        int streamType = intent.getIntExtra(VolumeNotification.EXTRA_STREAM_TYPE, AudioManager.STREAM_MUSIC);
        VolumeController controller = new VolumeController(context);

        if (VolumeNotification.ACTION_VOLUME_UP.equals(action)) {
            controller.adjustStreamVolume(streamType, AudioManager.ADJUST_RAISE);
        } else if (VolumeNotification.ACTION_VOLUME_DOWN.equals(action)) {
            controller.adjustStreamVolume(streamType, AudioManager.ADJUST_LOWER);
        } else {
            return;
        }

        vibrateFeedback(context);
        new VolumeNotification(context).show();
    }

    private void vibrateFeedback(Context context) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }
}