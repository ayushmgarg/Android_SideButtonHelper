package com.example.sidebuttonhelper.onboarding;

import android.Manifest;
import android.app.Activity;
import android.app.AppOpsManager;
import android.app.NotificationManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Process;
import android.provider.Settings;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.sidebuttonhelper.MainActivity;
import com.example.sidebuttonhelper.R;
import com.example.sidebuttonhelper.R;
import com.example.sidebuttonhelper.admin.ScreenLockAdminReceiver;
import com.example.sidebuttonhelper.service.ServiceWatchdogWorker;
import com.example.sidebuttonhelper.service.TapAccessibilityService;
import com.example.sidebuttonhelper.volume.VolumeNotification;

import java.util.ArrayList;
import java.util.List;

/**
 * Guided permission wizard, one plain-language step at a time. Each step explains why
 * a permission is needed before sending the user to the right system screen — none of
 * these can be silently auto-granted, all require a real navigation to Settings
 * (or a runtime permission dialog for POST_NOTIFICATIONS).
 */
public class OnboardingActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_ONBOARDING_DONE = "onboarding_done";
    private static final int REQ_POST_NOTIFICATIONS = 501;

    interface AccessChecker {
        boolean isGranted(Context context);
    }

    interface AccessRequester {
        void request(Activity activity);
    }

    static class OnboardingStep {
        final String title;
        final String explanation;
        final AccessChecker checker;
        final AccessRequester requester;

        OnboardingStep(String title, String explanation, AccessChecker checker, AccessRequester requester) {
            this.title = title;
            this.explanation = explanation;
            this.checker = checker;
            this.requester = requester;
        }
    }

    private final List<OnboardingStep> steps = new ArrayList<>();
    private int currentStep = 0;

    private Button nextButton;
    private Button backButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        buildSteps();

        nextButton = findViewById(R.id.btn_onboarding_next);
        backButton = findViewById(R.id.btn_onboarding_back);
        nextButton.setOnClickListener(v -> goToStep(currentStep + 1));
        backButton.setOnClickListener(v -> goToStep(currentStep - 1));

        showStep(0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // User may be returning from a system Settings screen — refresh this step's status.
        refreshCurrentStepFragment();
    }

    @SuppressWarnings("deprecation") // checkOpNoThrow needed for API < 29, no replacement exists below Q
    private void buildSteps() {
        steps.add(new OnboardingStep(
                getString(R.string.onboard_step1_title),
                getString(R.string.onboard_step1_desc),
                context -> {
                    String enabled = Settings.Secure.getString(context.getContentResolver(),
                            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
                    String serviceId = context.getPackageName() + "/" + TapAccessibilityService.class.getName();
                    return enabled != null && enabled.contains(serviceId);
                },
                activity -> activity.startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        ));

        steps.add(new OnboardingStep(
                getString(R.string.onboard_step2_title),
                getString(R.string.onboard_step2_desc),
                Settings::canDrawOverlays,
                activity -> activity.startActivity(new Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + activity.getPackageName())))
        ));

        steps.add(new OnboardingStep(
                getString(R.string.onboard_step3_title),
                getString(R.string.onboard_step3_desc),
                context -> {
                    AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
                    int mode;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        mode = appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                                Process.myUid(), context.getPackageName());
                    } else {
                        // Deprecated in API 29, but it's the only option on API 26-28 (our minSdk is 26)
                        mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                                Process.myUid(), context.getPackageName());
                    }
                    return mode == AppOpsManager.MODE_ALLOWED;
                },
                activity -> activity.startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        ));

        steps.add(new OnboardingStep(
                getString(R.string.onboard_step4_title),
                getString(R.string.onboard_step4_desc),
                context -> {
                    DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
                    ComponentName admin = new ComponentName(context, ScreenLockAdminReceiver.class);
                    return dpm != null && dpm.isAdminActive(admin);
                },
                activity -> {
                    ComponentName admin = new ComponentName(activity, ScreenLockAdminReceiver.class);
                    Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                    intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin);
                    intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                            "Needed so the app can lock your screen when you tap.");
                    activity.startActivity(intent);
                }
        ));

        steps.add(new OnboardingStep(
                getString(R.string.onboard_step5_title),
                getString(R.string.onboard_step5_desc),
                context -> {
                    PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                    return pm != null && pm.isIgnoringBatteryOptimizations(context.getPackageName());
                },
                activity -> {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + activity.getPackageName()));
                    activity.startActivity(intent);
                }
        ));

        steps.add(new OnboardingStep(
                getString(R.string.onboard_step6_title),
                getString(R.string.onboard_step6_desc),
                context -> {
                    NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    return nm != null && nm.isNotificationPolicyAccessGranted();
                },
                activity -> activity.startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        ));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            steps.add(new OnboardingStep(
                    getString(R.string.onboard_step7_title),
                    getString(R.string.onboard_step7_desc),
                    context -> ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                            == PackageManager.PERMISSION_GRANTED,
                    activity -> ActivityCompat.requestPermissions(activity,
                            new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_POST_NOTIFICATIONS)
            ));
        }
    }

    private void showStep(int index) {
        currentStep = index;
        OnboardingStep step = steps.get(index);
        PermissionStepFragment fragment = PermissionStepFragment.newInstance(step.title, step.explanation, index);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.onboarding_fragment_container, fragment)
                .commit();
        updateNavButtons();
    }

    private void goToStep(int index) {
        if (index < 0) return;
        if (index >= steps.size()) {
            finishOnboarding();
            return;
        }
        showStep(index);
    }

    boolean isStepGranted(int index) {
        return steps.get(index).checker.isGranted(this);
    }

    void requestStepAccess(int index) {
        steps.get(index).requester.request(this);
    }

    private void updateNavButtons() {
        backButton.setEnabled(currentStep > 0);
        boolean isLast = currentStep == steps.size() - 1;
        nextButton.setText(isLast ? getString(android.R.string.ok) : "Next");
    }

    private void refreshCurrentStepFragment() {
        Fragment f = getSupportFragmentManager().findFragmentById(R.id.onboarding_fragment_container);
        if (f instanceof PermissionStepFragment) {
            ((PermissionStepFragment) f).refreshGrantedState();
        }
    }

    private void finishOnboarding() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putBoolean(KEY_ONBOARDING_DONE, true)
                .apply();
        VolumeNotification.createChannel(this);
        ServiceWatchdogWorker.schedule(this);
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        refreshCurrentStepFragment();
    }
}