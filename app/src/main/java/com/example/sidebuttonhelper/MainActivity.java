package com.example.sidebuttonhelper;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sidebuttonhelper.databinding.ActivityMainBinding;
import com.example.sidebuttonhelper.onboarding.OnboardingActivity;
import com.example.sidebuttonhelper.service.ShakeWakeService;
import com.example.sidebuttonhelper.service.TapAccessibilityService;
import com.example.sidebuttonhelper.settings.SettingsActivity;
import com.example.sidebuttonhelper.settings.SettingsPrefs;
import com.example.sidebuttonhelper.volume.VolumeBubbleService;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnSetup.setOnClickListener(v ->
                startActivity(new Intent(this, OnboardingActivity.class)));

        binding.btnSettings.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));

        binding.switchMasterEnable.setChecked(SettingsPrefs.isHelperEnabled(this));
        binding.switchMasterEnable.setOnCheckedChangeListener((btn, isChecked) -> {
            SettingsPrefs.setHelperEnabled(this, isChecked);
            if (isChecked) startServices(); else stopServices();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void startServices() {
        startForegroundServiceCompat(ShakeWakeService.class);
        if (Settings.canDrawOverlays(this)) {
            startForegroundServiceCompat(VolumeBubbleService.class);
        }
    }

    private void stopServices() {
        stopService(new Intent(this, ShakeWakeService.class));
        stopService(new Intent(this, VolumeBubbleService.class));
    }

    private void startForegroundServiceCompat(Class<?> serviceClass) {
        Intent intent = new Intent(this, serviceClass);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void refreshStatus() {
        boolean accessibilityOn = isAccessibilityServiceEnabled();
        boolean overlayOn = Settings.canDrawOverlays(this);

        binding.textAccessibilityStatus.setText(
                accessibilityOn ? R.string.status_accessibility_ready : R.string.status_accessibility_needs_setup);
        binding.textOverlayStatus.setText(
                overlayOn ? R.string.status_overlay_granted : R.string.status_overlay_needs_setup);
    }

    private boolean isAccessibilityServiceEnabled() {
        AccessibilityManager am =
                (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am == null) return false;

        List<AccessibilityServiceInfo> enabledServices =
                am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);

        String targetId = getPackageName() + "/" + TapAccessibilityService.class.getName();

        for (AccessibilityServiceInfo info : enabledServices) {
            if (info.getId().equals(targetId)) {
                return true;
            }
        }
        return false;
    }
}