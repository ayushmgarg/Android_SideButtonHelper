package com.example.sidebuttonhelper.onboarding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.sidebuttonhelper.R;

/**
 * Renders a single onboarding step. Delegates the actual system call (startActivity /
 * requestPermissions) to the hosting OnboardingActivity, which owns the Activity
 * context those APIs need.
 */
public class PermissionStepFragment extends Fragment {

    private static final String ARG_TITLE = "arg_title";
    private static final String ARG_EXPLANATION = "arg_explanation";
    private static final String ARG_STEP_INDEX = "arg_step_index";

    private int stepIndex;
    private TextView statusLabel;
    private Button grantButton;

    public static PermissionStepFragment newInstance(String title, String explanation, int stepIndex) {
        PermissionStepFragment fragment = new PermissionStepFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_EXPLANATION, explanation);
        args.putInt(ARG_STEP_INDEX, stepIndex);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_permission_step, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = requireArguments();
        stepIndex = args.getInt(ARG_STEP_INDEX);

        ((TextView) view.findViewById(R.id.step_title)).setText(args.getString(ARG_TITLE));
        ((TextView) view.findViewById(R.id.step_explanation)).setText(args.getString(ARG_EXPLANATION));
        statusLabel = view.findViewById(R.id.step_status);
        grantButton = view.findViewById(R.id.btn_grant);

        grantButton.setOnClickListener(v ->
                ((OnboardingActivity) requireActivity()).requestStepAccess(stepIndex));

        refreshGrantedState();
    }

    void refreshGrantedState() {
        if (!isAdded()) return;
        boolean granted = ((OnboardingActivity) requireActivity()).isStepGranted(stepIndex);
        statusLabel.setText(granted ? "Granted \u2713" : "Not granted yet");
        grantButton.setText(granted ? "Open Settings" : "Grant Access");
    }
}