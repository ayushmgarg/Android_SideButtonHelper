package com.example.sidebuttonhelper.admin;

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class ScreenLockAdminReceiver extends DeviceAdminReceiver {

    @Override
    public void onEnabled(Context context, Intent intent) {
        super.onEnabled(context, intent);
        Toast.makeText(context, "Screen-lock permission enabled", Toast.LENGTH_SHORT).show();
    }

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        return "Disabling this will stop the double-tap screen-lock feature from working.";
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        super.onDisabled(context, intent);
        Toast.makeText(context, "Screen-lock permission disabled", Toast.LENGTH_SHORT).show();
    }

    /**
     * Call this from anywhere (e.g. TapAccessibilityService in Phase 3)
     * to actually turn the screen off / lock it.
     */
    public static void lockNow(Context context) {
        ComponentName adminComponent = new ComponentName(context, ScreenLockAdminReceiver.class);
        DevicePolicyManager dpm =
                (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);

        if (dpm != null && dpm.isAdminActive(adminComponent)) {
            dpm.lockNow();
        } else {
            Toast.makeText(context,
                    "Screen-lock permission not granted yet — enable it in Settings",
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Call this to check, before trying to lock, whether the permission is active.
     */
    public static boolean isAdminActive(Context context) {
        ComponentName adminComponent = new ComponentName(context, ScreenLockAdminReceiver.class);
        DevicePolicyManager dpm =
                (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        return dpm != null && dpm.isAdminActive(adminComponent);
    }

    /**
     * Builds the system intent to request admin activation.
     * Onboarding (Phase 8) will fire this with startActivityForResult/ActivityResultLauncher.
     */
    public static Intent buildRequestIntent(Context context) {
        ComponentName adminComponent = new ComponentName(context, ScreenLockAdminReceiver.class);
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Needed so the app can lock your screen when you double-tap.");
        return intent;
    }
}