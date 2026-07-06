package com.example.sidebuttonhelper.ui;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sidebuttonhelper.databinding.ActivityOemGuidanceBinding;

public class OemGuidanceActivity extends AppCompatActivity {

    private ActivityOemGuidanceBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOemGuidanceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String manufacturer = Build.MANUFACTURER.toLowerCase();
        binding.textManufacturerDetected.setText(
                "Detected device: " + Build.MANUFACTURER + " " + Build.MODEL);
        binding.textInstructions.setText(instructionsFor(manufacturer));

        binding.btnOpenAutostartSettings.setOnClickListener(v -> openAutostartSettings(manufacturer));
    }

    private String instructionsFor(String manufacturer) {
        if (manufacturer.contains("xiaomi")) {
            return "MIUI stops background apps aggressively. Tap below, then enable autostart for this app.";
        } else if (manufacturer.contains("oppo")) {
            return "ColorOS restricts background apps. Tap below, then allow this app to run in the background.";
        } else if (manufacturer.contains("vivo")) {
            return "Funtouch OS restricts background apps. Tap below, then enable background/autostart access.";
        } else if (manufacturer.contains("huawei") || manufacturer.contains("honor")) {
            return "EMUI/Magic UI restricts background apps. Tap below, enable 'Manage manually', then turn on Auto-launch.";
        } else if (manufacturer.contains("asus")) {
            return "Tap below, then enable auto-start for this app.";
        } else {
            return "Your device (" + Build.MANUFACTURER + ") doesn't usually need extra setup beyond the "
                    + "battery optimization exemption from Setup.";
        }
    }

    private void openAutostartSettings(String manufacturer) {
        Intent intent = new Intent();
        try {
            if (manufacturer.contains("xiaomi")) {
                intent.setComponent(new ComponentName("com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"));
            } else if (manufacturer.contains("oppo")) {
                intent.setComponent(new ComponentName("com.coloros.safecenter",
                        "com.coloros.safecenter.permission.startup.StartupAppListActivity"));
            } else if (manufacturer.contains("vivo")) {
                intent.setComponent(new ComponentName("com.vivo.permissionmanager",
                        "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"));
            } else if (manufacturer.contains("huawei") || manufacturer.contains("honor")) {
                intent.setComponent(new ComponentName("com.huawei.systemmanager",
                        "com.huawei.systemmanager.optimize.process.ProtectActivity"));
            } else if (manufacturer.contains("asus")) {
                intent.setComponent(new ComponentName("com.asus.mobilemanager",
                        "com.asus.mobilemanager.autostart.AutoStartActivity"));
            } else {
                openAppInfoFallback();
                return;
            }
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            openAppInfoFallback();
        }
    }

    private void openAppInfoFallback() {
        Toast.makeText(this, "Couldn't find manufacturer settings — opening app info instead",
                Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }
}