package com.example.sidebuttonhelper.volume;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SeekBar;

import com.example.sidebuttonhelper.R;

/**
 * Floating always-on-top bubble (reuses the Draw-Over-Apps permission granted during
 * onboarding). Collapsed: small draggable circle. Tap (not drag) expands it into a
 * panel with Ring/Notification/Alarm/Media/Call sliders, per-stream mute buttons, and
 * a Normal/Vibrate/Silent ringer switch. Reachable without opening the app — this is
 * the main replacement for physical volume buttons.
 */
public class VolumeBubbleService extends Service {

    private static final String PREFS_NAME = "volume_bubble_prefs";
    private static final String KEY_X = "bubble_x";
    private static final String KEY_Y = "bubble_y";
    private static final int CLICK_DRAG_TOLERANCE_PX = 12;
    private static final long CLICK_MAX_DURATION_MS = 200;

    private WindowManager windowManager;
    private View bubbleRoot;
    private WindowManager.LayoutParams params;
    private VolumeController volumeController;
    private Vibrator vibrator;
    private boolean expanded = false;

    @Override
    public void onCreate() {
        super.onCreate();

        if (!Settings.canDrawOverlays(this)) {
            // Onboarding (Phase 8) is responsible for requesting this permission first.
            stopSelf();
            return;
        }

        volumeController = new VolumeController(this);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        bubbleRoot = LayoutInflater.from(this).inflate(R.layout.view_volume_bubble, null);

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, // minSdk 26 == API level this was introduced
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        params.x = prefs.getInt(KEY_X, 100);
        params.y = prefs.getInt(KEY_Y, 300);

        windowManager.addView(bubbleRoot, params);

        setupBubbleDrag();
        setupExpandedPanel();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // not a bound service
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (windowManager != null && bubbleRoot != null) {
            windowManager.removeView(bubbleRoot);
        }
    }

    // ---------- Drag + tap-to-expand ----------

    private void setupBubbleDrag() {
        View icon = bubbleRoot.findViewById(R.id.bubble_icon);
        icon.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            private long touchStartTime;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        touchStartTime = System.currentTimeMillis();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(bubbleRoot, params);
                        return true;

                    case MotionEvent.ACTION_UP:
                        float dx = Math.abs(event.getRawX() - initialTouchX);
                        float dy = Math.abs(event.getRawY() - initialTouchY);
                        long duration = System.currentTimeMillis() - touchStartTime;
                        boolean wasClick = dx < CLICK_DRAG_TOLERANCE_PX
                                && dy < CLICK_DRAG_TOLERANCE_PX
                                && duration < CLICK_MAX_DURATION_MS;
                        if (wasClick) {
                            toggleExpanded();
                        } else {
                            savePosition();
                        }
                        return true;

                    default:
                        return false;
                }
            }
        });
    }

    private void toggleExpanded() {
        expanded = !expanded;
        bubbleRoot.findViewById(R.id.expanded_panel)
                .setVisibility(expanded ? View.VISIBLE : View.GONE);
    }

    private void savePosition() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putInt(KEY_X, params.x)
                .putInt(KEY_Y, params.y)
                .apply();
    }

    // ---------- Sliders / mute buttons / ringer mode ----------

    private void setupExpandedPanel() {
        bindStreamRow(AudioManager.STREAM_RING, R.id.seek_ring, R.id.btn_mute_ring,
                volumeController.getRingMaxVolume(), volumeController.getRingVolume());
        bindStreamRow(AudioManager.STREAM_NOTIFICATION, R.id.seek_notification, R.id.btn_mute_notification,
                volumeController.getNotificationMaxVolume(), volumeController.getNotificationVolume());
        bindStreamRow(AudioManager.STREAM_ALARM, R.id.seek_alarm, R.id.btn_mute_alarm,
                volumeController.getAlarmMaxVolume(), volumeController.getAlarmVolume());
        bindStreamRow(AudioManager.STREAM_MUSIC, R.id.seek_media, R.id.btn_mute_media,
                volumeController.getMediaMaxVolume(), volumeController.getMediaVolume());
        bindStreamRow(AudioManager.STREAM_VOICE_CALL, R.id.seek_call, R.id.btn_mute_call,
                volumeController.getCallMaxVolume(), volumeController.getCallVolume());

        bubbleRoot.findViewById(R.id.btn_ringer_normal)
                .setOnClickListener(v -> applyRingerMode(AudioManager.RINGER_MODE_NORMAL));
        bubbleRoot.findViewById(R.id.btn_ringer_vibrate)
                .setOnClickListener(v -> applyRingerMode(AudioManager.RINGER_MODE_VIBRATE));
        bubbleRoot.findViewById(R.id.btn_ringer_silent)
                .setOnClickListener(v -> applyRingerMode(AudioManager.RINGER_MODE_SILENT));
    }

    private void bindStreamRow(final int streamType, int seekBarId, int muteBtnId, int max, int current) {
        SeekBar seekBar = bubbleRoot.findViewById(seekBarId);
        ImageView muteBtn = bubbleRoot.findViewById(muteBtnId);

        seekBar.setMax(max);
        seekBar.setProgress(current);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser) {
                    volumeController.setStreamVolumeSafe(streamType, progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar sb) {
                // no-op
            }

            @Override
            public void onStopTrackingTouch(SeekBar sb) {
                vibrateFeedback();
            }
        });

        muteBtn.setOnClickListener(v -> {
            volumeController.toggleMuteStream(streamType);
            boolean nowMuted = volumeController.isStreamMuted(streamType);
            seekBar.setProgress(nowMuted ? 0 : seekBar.getMax());
            vibrateFeedback();
        });
    }

    private void applyRingerMode(int mode) {
        boolean applied = volumeController.setRingerMode(mode);
        if (applied) {
            vibrateFeedback();
        }
        // If not applied: Notification Policy (DND) access missing. Onboarding
        // (Phase 8) is responsible for requesting it — this service doesn't prompt.
    }

    private void vibrateFeedback() {
        if (vibrator == null) return;
        vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE));
    }
}