package com.example.sidebuttonhelper.settings;

import android.os.Bundle;
import android.widget.SeekBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.example.sidebuttonhelper.R;
import com.example.sidebuttonhelper.databinding.ActivitySettingsBinding;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;

    // SeekBar range 0-25 maps to threshold 1.5 - 4.0 (matches ShakeWakeService's expected range)
    private static final float SEEKBAR_MIN = 1.5f;
    private static final float SEEKBAR_STEP = 0.1f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        loadCurrentValues();
        wireListeners();
    }

    private void loadCurrentValues() {
        int tapCount = SettingsPrefs.getTapCount(this);
        binding.radio2Taps.setChecked(tapCount == 2);
        binding.radio3Taps.setChecked(tapCount == 3);

        float threshold = SettingsPrefs.getShakeThreshold(this);
        int seekProgress = Math.round((threshold - SEEKBAR_MIN) / SEEKBAR_STEP);
        binding.seekBarShakeSensitivity.setProgress(seekProgress);
        updateShakeLabel(threshold);

        binding.switchHomeScreenOnly.setChecked(SettingsPrefs.isHomeScreenOnly(this));
        binding.switchExcludeCalls.setChecked(SettingsPrefs.isExcludeDuringCalls(this));
        binding.switchExcludeCamera.setChecked(SettingsPrefs.isExcludeCamera(this));

        binding.switchStreamRing.setChecked(SettingsPrefs.isStreamEnabled(this, "ring"));
        binding.switchStreamNotification.setChecked(SettingsPrefs.isStreamEnabled(this, "notification"));
        binding.switchStreamAlarm.setChecked(SettingsPrefs.isStreamEnabled(this, "alarm"));
        binding.switchStreamMedia.setChecked(SettingsPrefs.isStreamEnabled(this, "media"));
        binding.switchStreamCall.setChecked(SettingsPrefs.isStreamEnabled(this, "call"));
    }

    private void wireListeners() {
        binding.radio2Taps.setOnClickListener(v -> SettingsPrefs.setTapCount(this, 2));
        binding.radio3Taps.setOnClickListener(v -> SettingsPrefs.setTapCount(this, 3));

        binding.seekBarShakeSensitivity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float threshold = SEEKBAR_MIN + (progress * SEEKBAR_STEP);
                updateShakeLabel(threshold);
                if (fromUser) {
                    SettingsPrefs.setShakeThreshold(SettingsActivity.this, threshold);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        binding.switchHomeScreenOnly.setOnCheckedChangeListener((btn, checked) ->
                SettingsPrefs.setHomeScreenOnly(this, checked));

        binding.switchExcludeCalls.setOnCheckedChangeListener((btn, checked) ->
                SettingsPrefs.setExcludeDuringCalls(this, checked));

        binding.switchExcludeCamera.setOnCheckedChangeListener((btn, checked) ->
                SettingsPrefs.setExcludeCamera(this, checked));

        binding.switchStreamRing.setOnCheckedChangeListener((btn, checked) ->
                SettingsPrefs.setStreamEnabled(this, "ring", checked));
        binding.switchStreamNotification.setOnCheckedChangeListener((btn, checked) ->
                SettingsPrefs.setStreamEnabled(this, "notification", checked));
        binding.switchStreamAlarm.setOnCheckedChangeListener((btn, checked) ->
                SettingsPrefs.setStreamEnabled(this, "alarm", checked));
        binding.switchStreamMedia.setOnCheckedChangeListener((btn, checked) ->
                SettingsPrefs.setStreamEnabled(this, "media", checked));
        binding.switchStreamCall.setOnCheckedChangeListener((btn, checked) ->
                SettingsPrefs.setStreamEnabled(this, "call", checked));

        binding.btnLanguageEnglish.setOnClickListener(v -> setAppLanguage("en"));
        binding.btnLanguageHindi.setOnClickListener(v -> setAppLanguage("hi"));
    }

    private void updateShakeLabel(float threshold) {
        binding.textShakeSensitivityValue.setText(
                getString(R.string.shake_sensitivity_label, threshold));
    }

    private void setAppLanguage(String languageCode) {
        SettingsPrefs.setLanguage(this, languageCode);
        AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(languageCode));
        // AppCompatDelegate handles the activity recreation needed to apply the new locale.
    }
}