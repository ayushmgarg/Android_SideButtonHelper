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

import com.example.sidebuttonhelper.R;
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
                getString(R.string.oem_detected_device, Build.MANUFACTURER, Build.MODEL));
        binding.textInstructions.setText(instructionsFor(manufacturer));

        binding.btnOpenAutostartSettings.setOnClickListener(v -> openAutostartSettings(manufacturer));
    }

    private String instructionsFor(String manufacturer) {
        if (manufacturer.contains("xiaomi")) {
            return getString(R.string.oem_instructions_xiaomi);
        } else if (manufacturer.contains("oppo")) {
            return getString(R.string.oem_instructions_oppo);
        } else if (manufacturer.contains("vivo")) {
            return getString(R.string.oem_instructions_vivo);
        } else if (manufacturer.contains("huawei") || manufacturer.contains("honor")) {
            return getString(R.string.oem_instructions_huawei);
        } else if (manufacturer.contains("asus")) {
            return getString(R.string.oem_instructions_asus);
        } else {
            return getString(R.string.oem_instructions_default, Build.MANUFACTURER);
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
        Toast.makeText(this, R.string.oem_fallback_toast, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }
}