package com.example.sidebuttonhelper.settings;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsPrefs {

    private static final String PREFS_NAME = "side_button_helper_prefs";

    private static final String KEY_HELPER_ENABLED = "pref_helper_enabled";
    private static final String KEY_TAP_COUNT = "pref_tap_count";
    private static final String KEY_SHAKE_THRESHOLD = "pref_shake_threshold";
    private static final String KEY_HOME_SCREEN_ONLY = "pref_home_screen_only";
    private static final String KEY_EXCLUDE_CALLS = "pref_exclude_during_calls";
    private static final String KEY_EXCLUDE_CAMERA = "pref_exclude_camera";
    private static final String KEY_LANGUAGE = "pref_app_language";

    private static final String KEY_STREAM_RING = "pref_bubble_enabled_streams_ring";
    private static final String KEY_STREAM_NOTIFICATION = "pref_bubble_enabled_streams_notification";
    private static final String KEY_STREAM_ALARM = "pref_bubble_enabled_streams_alarm";
    private static final String KEY_STREAM_MEDIA = "pref_bubble_enabled_streams_media";
    private static final String KEY_STREAM_CALL = "pref_bubble_enabled_streams_call";

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isHelperEnabled(Context context) {
        return prefs(context).getBoolean(KEY_HELPER_ENABLED, false);
    }

    public static void setHelperEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_HELPER_ENABLED, enabled).apply();
    }

    public static int getTapCount(Context context) {
        return prefs(context).getInt(KEY_TAP_COUNT, 2);
    }

    public static void setTapCount(Context context, int count) {
        prefs(context).edit().putInt(KEY_TAP_COUNT, count).apply();
    }

    public static float getShakeThreshold(Context context) {
        return prefs(context).getFloat(KEY_SHAKE_THRESHOLD, 2.5f);
    }

    public static void setShakeThreshold(Context context, float threshold) {
        prefs(context).edit().putFloat(KEY_SHAKE_THRESHOLD, threshold).apply();
    }

    public static boolean isHomeScreenOnly(Context context) {
        return prefs(context).getBoolean(KEY_HOME_SCREEN_ONLY, false);
    }

    public static void setHomeScreenOnly(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_HOME_SCREEN_ONLY, value).apply();
    }

    public static boolean isExcludeDuringCalls(Context context) {
        return prefs(context).getBoolean(KEY_EXCLUDE_CALLS, true);
    }

    public static void setExcludeDuringCalls(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_EXCLUDE_CALLS, value).apply();
    }

    public static boolean isExcludeCamera(Context context) {
        return prefs(context).getBoolean(KEY_EXCLUDE_CAMERA, true);
    }

    public static void setExcludeCamera(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_EXCLUDE_CAMERA, value).apply();
    }

    public static String getLanguage(Context context) {
        return prefs(context).getString(KEY_LANGUAGE, "en");
    }

    public static void setLanguage(Context context, String languageCode) {
        prefs(context).edit().putString(KEY_LANGUAGE, languageCode).apply();
    }

    public static boolean isStreamEnabled(Context context, String streamKey) {
        return prefs(context).getBoolean(streamKeyToPrefKey(streamKey), true);
    }

    public static void setStreamEnabled(Context context, String streamKey, boolean enabled) {
        prefs(context).edit().putBoolean(streamKeyToPrefKey(streamKey), enabled).apply();
    }

    private static String streamKeyToPrefKey(String streamKey) {
        switch (streamKey) {
            case "ring": return KEY_STREAM_RING;
            case "notification": return KEY_STREAM_NOTIFICATION;
            case "alarm": return KEY_STREAM_ALARM;
            case "media": return KEY_STREAM_MEDIA;
            case "call": return KEY_STREAM_CALL;
            default: throw new IllegalArgumentException("Unknown stream key: " + streamKey);
        }
    }
}