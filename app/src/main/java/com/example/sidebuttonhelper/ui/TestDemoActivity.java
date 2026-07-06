package com.example.sidebuttonhelper.ui;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.MotionEvent;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sidebuttonhelper.databinding.ActivityTestDemoBinding;
import com.example.sidebuttonhelper.settings.SettingsPrefs;

public class TestDemoActivity extends AppCompatActivity implements SensorEventListener {

    private static final long TAP_WINDOW_MS = 400;

    private ActivityTestDemoBinding binding;
    private SensorManager sensorManager;
    private Sensor accelerometer;

    private int tapCount = 0;
    private long lastTapTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTestDemoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        int requiredTaps = SettingsPrefs.getTapCount(this);
        binding.textTapInstructions.setText(
                "Tap the box below " + requiredTaps + " times quickly to test lock detection");

        binding.tapTestArea.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                registerTap(requiredTaps);
            }
            return true;
        });

        binding.btnOpenVolumeBubbleTest.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)));
    }

    private void registerTap(int requiredTaps) {
        long now = SystemClock.elapsedRealtime();
        tapCount = (now - lastTapTime > TAP_WINDOW_MS) ? 1 : tapCount + 1;
        lastTapTime = now;
        binding.textTapCount.setText("Taps detected: " + tapCount);

        if (tapCount >= requiredTaps) {
            tapCount = 0;
            binding.textTapResult.setText("Would lock the screen now (demo only — nothing actually locked)");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float gX = event.values[0] / SensorManager.GRAVITY_EARTH;
        float gY = event.values[1] / SensorManager.GRAVITY_EARTH;
        float gZ = event.values[2] / SensorManager.GRAVITY_EARTH;
        float gForce = (float) Math.sqrt(gX * gX + gY * gY + gZ * gZ);
        float threshold = SettingsPrefs.getShakeThreshold(this);

        binding.textShakeMagnitude.setText(String.format(
                "Current: %.2f  |  Threshold: %.2f%s",
                gForce, threshold, gForce > threshold ? "  — would trigger!" : ""));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // not used
    }
}