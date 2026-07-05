package com.example.sidebuttonhelper.service;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

import com.example.sidebuttonhelper.admin.ScreenLockAdminReceiver;

public class TapAccessibilityService extends AccessibilityService {

    private static final long TAP_WINDOW_MS = 400;   // max gap allowed between taps
    private static final int REQUIRED_TAPS = 2;       // will become configurable in Settings (Phase 10)

    private WindowManager windowManager;
    private View overlayView;
    private int tapCount = 0;
    private long lastTapTime = 0;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        addInvisibleOverlay();
    }

    /**
     * Adds a near-invisible 1x1 overlay window with FLAG_WATCH_OUTSIDE_TOUCH.
     * This lets us receive a notification for every touch anywhere on screen
     * WITHOUT blocking or consuming that touch - normal phone use is unaffected.
     */
    private void addInvisibleOverlay() {
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        overlayView = new View(this);

        int overlayType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                1, 1,
                overlayType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;

        overlayView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                registerTap();
            }
            return false;
        });

        try {
            windowManager.addView(overlayView, params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void registerTap() {
        long now = SystemClock.elapsedRealtime();
        tapCount = (now - lastTapTime > TAP_WINDOW_MS) ? 1 : tapCount + 1;
        lastTapTime = now;

        if (tapCount >= REQUIRED_TAPS) {
            tapCount = 0;
            triggerLock();
        }
    }

    private void triggerLock() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Android 9+ : accessibility service can lock directly, no Device Admin needed
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
        } else {
            // Android 8.0 / 8.1 fallback
            ScreenLockAdminReceiver.lockNow(this);
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Not used - tap detection happens via the overlay's ACTION_OUTSIDE touches above.
    }

    @Override
    public void onInterrupt() {
        // Required override, no action needed.
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (windowManager != null && overlayView != null) {
            try {
                windowManager.removeView(overlayView);
            } catch (Exception ignored) {
            }
        }
    }
}