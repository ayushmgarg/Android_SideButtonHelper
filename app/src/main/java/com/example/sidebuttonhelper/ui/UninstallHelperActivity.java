package com.example.sidebuttonhelper.ui;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sidebuttonhelper.admin.ScreenLockAdminReceiver;
import com.example.sidebuttonhelper.databinding.ActivityUninstallHelperBinding;

public class UninstallHelperActivity extends AppCompatActivity {

    private ActivityUninstallHelperBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUninstallHelperBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnRevokeAndUninstall.setOnClickListener(v -> revokeAdminThenUninstall());
    }

    private void revokeAdminThenUninstall() {
        ComponentName adminComponent = new ComponentName(this, ScreenLockAdminReceiver.class);
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

        if (dpm != null && dpm.isAdminActive(adminComponent)) {
            dpm.removeActiveAdmin(adminComponent);
        }

        Toast.makeText(this, "Permissions released. Opening uninstall screen…", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }
}