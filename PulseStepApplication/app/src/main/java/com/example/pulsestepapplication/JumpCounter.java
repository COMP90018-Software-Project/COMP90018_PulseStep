package com.example.pulsestepapplication;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class JumpCounter {

    private static final String TAG = "JumpCounter";
    private static final int REQUEST_CODE = 10086; // Permission request code

    private final SensorManager sensorManager;
    private final Sensor accelerometerSensor;
//    private final Activity activity;
    private final Context context;

    private SensorEventListener jumpListener;
    private int jumpCount = 0; // Jumps counted in the current tracking session
    private int savedJumpCount = 0; // Total saved jumps across sessions
    private boolean isTrackingJumps = false;

    private JumpCounterListener jumpCounterListener;


    private final float minThreshold = 0.2f;
    private final float maxThreshold = 0.5f;
    private final float finishThreshold = 3.0f;
    private final float largeMovingRate = 1.2f;
    private static final int JUMP_DETECTION_WINDOW_MS = 5; // Time window to detect a jump (ms)
    private long lastJumpTime = 0;
    private boolean is_initial = true;
    boolean isJumpFinished = true;

    private float xInit = 0f;
    private float yInit = 0f;
    private float zInit = 0f;
    private float initAcc = 0f;

    public JumpCounter(Context context) {
        this.context = context;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if (accelerometerSensor == null) {
            Log.e(TAG, "Accelerometer sensor is not available!");
            return;
        }

        initializeJumpListener();
    }


    private void initializeJumpListener() {

        jumpListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                if (!isTrackingJumps) return;

                float x = sensorEvent.values[0];
                float y = sensorEvent.values[1];
                float z = sensorEvent.values[2];

                if (is_initial) {
                    is_initial = false;
                    xInit = x;
                    yInit = y;
                    zInit = z;
                    isJumpFinished = true;
                }


                float nowAcc = (float) Math.sqrt(x * x + y * y + z * z);
                // float pastAcc = (float) Math.sqrt(xLast * xLast + yLast * yLast + zLast * zLast);

                if (nowAcc < largeMovingRate * 9.8f) {
                    // Update the initial gravity vector components
                    xInit = 0.9f * xInit + 0.1f * x;
                    yInit = 0.9f * yInit + 0.1f * y;
                    zInit = 0.9f * zInit + 0.1f * z;
                }

                // Normalize xInit, yInit, zInit to ensure their magnitude equals 9.8
                float magnitude = (float) Math.sqrt(xInit * xInit + yInit * yInit + zInit * zInit);
                if (magnitude != 0) {
                    float scale = 9.8f / magnitude;
                    xInit *= scale;
                    yInit *= scale;
                    zInit *= scale;
                }

                float initDiffAcc = (float) Math.sqrt((xInit-x)*(xInit-x) + (yInit-y)*(yInit-y) + (zInit-z)*(zInit-z));
                if (initDiffAcc > finishThreshold * 9.8f) {
                    isJumpFinished = true;
                }


                // float gForceDiff = Math.abs(nowAcc - pastAcc);

                if (initDiffAcc < maxThreshold*9.8f && initDiffAcc > minThreshold*9.8f && isJumpFinished) {
                    jumpCount++;
                    isJumpFinished = false;
                    if (jumpCounterListener != null) {
                        jumpCounterListener.onJumpCountUpdated(jumpCount);
                    }
                    Log.d(TAG, "Jump detected! Total jumps (/2): " + jumpCount);
                }
            }


            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                // Not used in this implementation
            }
        };
    }

    public void unregisterListener() {
        if (isTrackingJumps && jumpListener != null) {
            sensorManager.unregisterListener(jumpListener, accelerometerSensor);
            isTrackingJumps = false;
        }
    }

    public void registerListener() {
        if (!isTrackingJumps && jumpListener != null) {
            sensorManager.registerListener(jumpListener, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
            isTrackingJumps = true;
        }
    }

    public void startJumpTracking() {
        if (!isTrackingJumps) {
            registerListener();
            Log.d(TAG, "Jump tracking started.");
        }
    }

    public void stopJumpTracking() {
        if (isTrackingJumps) {
            unregisterListener();
            Log.d(TAG, "Jump tracking stopped.");
        }
    }

    /**
     * Sets the listener to receive jump count updates.
     *
     * @param listener The JumpCounterListener implementation.
     */
    public void setJumpCounterListener(JumpCounterListener listener) {
        if (listener != null) {
            this.jumpCounterListener = listener;
        } else {
            Log.e(TAG, "Passed JumpCounterListener is null");
        }
    }

    /**
     * Listener interface for jump count updates.
     */
    public interface JumpCounterListener {
        void onJumpCountUpdated(int jumpCount);
    }
}
