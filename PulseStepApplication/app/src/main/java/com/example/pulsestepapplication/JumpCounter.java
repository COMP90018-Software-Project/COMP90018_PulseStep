//package com.example.pulsestepapplication;
//
//import android.app.Activity;
//import android.content.Context;
//import android.hardware.Sensor;
//import android.hardware.SensorEventListener;
//import android.hardware.SensorManager;
//import android.util.Log;
//
//public class JumpCounter {
//
//    private static final String TAG = "JumpCounter";
//    private static final int REQUEST_CODE = 10086; // Permission request code
//
//    private final SensorManager sensorManager;
//    private final Sensor jumpDetectorSensor;
//    private final Activity activity;
//    private final Context context;
//
//    private SensorEventListener stepListener;
//    private int jumpCount = 0; // Steps counted in the current tracking session
//    private int savedJumpCount = 0; // Total saved steps across sessions
//    private boolean isTrackingJumps = false;
//
//    private JumpCounterListener jumpCounterListener;
//    /**
//     * Constructor initializes the sensor manager and step detector sensor.
//     *
//     * @param activity The activity context.
//     */
//    public JumpCounter(Activity activity) {
//        this.activity = activity;
//        this.context = activity;
//        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
//        jumpDetectorSensor = sensorManager.getDefaultSensor();
//
//        if (jumpDetectorSensor == null) {
//            Log.e(TAG, "Jump detector sensor is not available!");
//            return;
//        }
//
//        initializeJumpListener();
//    }
//
//    private void initializeJumpListener() {
//    }
//
//    public void unregisterListener() {
//    }
//
//    public void registerListener() {
//    }
//
//    public void startJumpTracking() {
//    }
//
//    public void stopJumpTracking() {
//    }
//
//    /**
//     * Sets the listener to receive step count updates.
//     *
//     * @param listener The StepCounterListener implementation.
//     */
//    public void setJumpCounterListener(JumpCounter.JumpCounterListener listener) {
//        if (listener != null) {
//            this.jumpCounterListener = listener;
//        } else {
//            Log.e(TAG, "Passed StepCounterListener is null");
//        }
//    }
//
//
//    /**
//     * Listener interface for step count updates.
//     */
//    public interface JumpCounterListener {
//        void onJumpCountUpdated(int jumpCount);
//    }
//}
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
    private final Activity activity;
    private final Context context;

    private SensorEventListener jumpListener;
    private int jumpCount = 0; // Jumps counted in the current tracking session
    private int savedJumpCount = 0; // Total saved jumps across sessions
    private boolean isTrackingJumps = false;

    private JumpCounterListener jumpCounterListener;

    // Thresholds for detecting jumps (adjust as needed)
    private static final float JUMP_THRESHOLD_GRAVITY = 0.5f;  // Gravity threshold to consider it a jump
    private static final int JUMP_DETECTION_WINDOW_MS = 500; // Time window to detect a jump (ms)
    private long lastJumpTime = 0;

    /**
     * Constructor initializes the sensor manager and accelerometer sensor.
     *
     * @param activity The activity context.
     */
    public JumpCounter(Activity activity) {
        this.activity = activity;
        this.context = activity;
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
            public void onSensorChanged(SensorEvent event) {
                // Get acceleration values on x, y, and z axes
                float x = event.values[0];
                float y = event.values[1] - 9.81f; // Subtract gravity from y-axis
                float z = event.values[2];

                // Calculate the total acceleration value including gravity
                //TODO： 这里的逻辑要改成x，y，z各自减去before
                float gForce = (float) Math.sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH;

                // Check if the gForce exceeds the jump threshold and ensure there's enough time between jumps
                if (gForce > JUMP_THRESHOLD_GRAVITY) {
                    long now = System.currentTimeMillis();
                    if (now - lastJumpTime > JUMP_DETECTION_WINDOW_MS) {
                        lastJumpTime = now;
                        jumpCount++;
                        if (jumpCounterListener != null) {
                            jumpCounterListener.onJumpCountUpdated(jumpCount);
                        }
                        Log.d(TAG, "Jump detected! Total jumps: " + jumpCount);
                    }
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
    public void setJumpCounterListener(JumpCounter.JumpCounterListener listener) {
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
