package com.example.sidebuttonhelper.volume;

import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioManager;

/**
 * Wraps AudioManager with one clean method per stream (Ring, Notification, Alarm,
 * Media, Call). Centralizes the SecurityException handling that Android throws when
 * silencing Ring/Notification without Do-Not-Disturb (Notification Policy) access.
 */
public class VolumeController {

    private final AudioManager audioManager;
    private final NotificationManager notificationManager;

    public VolumeController(Context context) {
        Context appContext = context.getApplicationContext();
        this.audioManager = (AudioManager) appContext.getSystemService(Context.AUDIO_SERVICE);
        this.notificationManager = (NotificationManager) appContext.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    // ---------- Ring ----------
    public int getRingVolume() { return audioManager.getStreamVolume(AudioManager.STREAM_RING); }
    public int getRingMaxVolume() { return audioManager.getStreamMaxVolume(AudioManager.STREAM_RING); }
    public boolean setRingVolume(int index) { return setStreamVolumeSafe(AudioManager.STREAM_RING, index); }

    // ---------- Notification ----------
    public int getNotificationVolume() { return audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION); }
    public int getNotificationMaxVolume() { return audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION); }
    public boolean setNotificationVolume(int index) { return setStreamVolumeSafe(AudioManager.STREAM_NOTIFICATION, index); }

    // ---------- Alarm ----------
    public int getAlarmVolume() { return audioManager.getStreamVolume(AudioManager.STREAM_ALARM); }
    public int getAlarmMaxVolume() { return audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM); }
    public boolean setAlarmVolume(int index) { return setStreamVolumeSafe(AudioManager.STREAM_ALARM, index); }

    // ---------- Media / Music ----------
    public int getMediaVolume() { return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC); }
    public int getMediaMaxVolume() { return audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC); }
    public boolean setMediaVolume(int index) { return setStreamVolumeSafe(AudioManager.STREAM_MUSIC, index); }

    // ---------- Call ----------
    public int getCallVolume() { return audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL); }
    public int getCallMaxVolume() { return audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL); }
    public boolean setCallVolume(int index) { return setStreamVolumeSafe(AudioManager.STREAM_VOICE_CALL, index); }

    // ---------- Generic (for call sites where the stream is user-configurable) ----------
    public int getStreamVolume(int streamType) { return audioManager.getStreamVolume(streamType); }
    public int getStreamMaxVolume(int streamType) { return audioManager.getStreamMaxVolume(streamType); }

    /**
     * Sets a stream's volume directly, catching the SecurityException Android 7+ throws
     * when an app without Do-Not-Disturb access tries to silence RING/NOTIFICATION.
     * @return true if applied, false if blocked (caller should route the user to grant
     * Notification Policy / DND access).
     */
    public boolean setStreamVolumeSafe(int streamType, int index) {
        try {
            audioManager.setStreamVolume(streamType, index, 0);
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }

    public void adjustStreamVolume(int streamType, int direction) {
        try {
            audioManager.adjustStreamVolume(streamType, direction, 0);
        } catch (SecurityException ignored) {
            // Same DND restriction as above; UI should check hasDndAccess() before
            // offering mute on RING/NOTIFICATION.
        }
    }

    // ---------- Mute ----------
    public void muteStream(int streamType) {
        adjustStreamVolume(streamType, AudioManager.ADJUST_MUTE);
    }

    public void unmuteStream(int streamType) {
        adjustStreamVolume(streamType, AudioManager.ADJUST_UNMUTE);
    }

    public void toggleMuteStream(int streamType) {
        adjustStreamVolume(streamType, AudioManager.ADJUST_TOGGLE_MUTE);
    }

    public boolean isStreamMuted(int streamType) {
        return audioManager.getStreamVolume(streamType) == 0;
    }

    // ---------- Ringer mode (Normal / Vibrate / Silent) ----------
    public int getRingerMode() {
        return audioManager.getRingerMode();
    }

    /**
     * @return true if applied, false if Notification Policy (DND) access is required
     * and not yet granted — caller should launch
     * Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS.
     */
    public boolean setRingerMode(int mode) {
        boolean needsDnd = mode == AudioManager.RINGER_MODE_SILENT
                || mode == AudioManager.RINGER_MODE_VIBRATE;
        if (needsDnd && !hasDndAccess()) {
            return false;
        }
        audioManager.setRingerMode(mode);
        return true;
    }

    public boolean hasDndAccess() {
        return notificationManager.isNotificationPolicyAccessGranted();
    }
}