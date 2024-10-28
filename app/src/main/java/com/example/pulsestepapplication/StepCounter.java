package com.example.pulsestepapplication;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * StepCounter class to track the number of steps using the device's step detector sensor.
 */
public class StepCounter {
    private static final String TAG = "StepCounter";

    private final SensorManager sensorManager;
    private final Sensor stepDetectorSensor;
    private final Context context;

    private SensorEventListener stepListener;
    private int stepCount = 0; // Steps counted in the current tracking session
    private int savedStepCount = 0; // Total saved steps across sessions
    private boolean isTrackingSteps = false;

    private StepCounterListener stepCounterListener;

    /**
     * Listener interface for step count updates.
     */
    public interface StepCounterListener {
        void onStepCountUpdated(int stepCount);
        void onPermissionRequired();
    }

    /**
     * Constructor initializes the sensor manager and step detector sensor.
     *
     * @param context The context.
     */
    public StepCounter(Context context) {
        this.context = context;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

        if (stepDetectorSensor == null) {
            Log.e(TAG, "Step detector sensor is not available!");
            return;
        }

        initializeStepListener();
    }

    /**
     * Initializes the SensorEventListener for step detection.
     */
    private void initializeStepListener() {
        stepListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (isTrackingSteps) {
                    stepCount++;
                    int currentSteps = savedStepCount + stepCount;
                    Log.d(TAG, "Step count updated: " + currentSteps);

                    if (stepCounterListener != null) {
                        stepCounterListener.onStepCountUpdated(currentSteps);
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                // No action needed on accuracy change
            }
        };
    }

    /**
     * Sets the listener to receive step count updates.
     *
     * @param listener The StepCounterListener implementation.
     */
    public void setStepCounterListener(StepCounterListener listener) {
        if (listener != null) {
            this.stepCounterListener = listener;
        } else {
            Log.e(TAG, "Passed StepCounterListener is null");
        }
    }

    /**
     * Starts step tracking by registering the sensor listener.
     */
    public void startStepTracking() {
        if (stepDetectorSensor == null) {
            Log.e(TAG, "Step detector sensor is not available");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Missing ACTIVITY_RECOGNITION permission");
                if (stepCounterListener != null) {
                    stepCounterListener.onPermissionRequired();
                }
                return;
            }
        }

        // Register the sensor listener
        boolean success = sensorManager.registerListener(stepListener, stepDetectorSensor, SensorManager.SENSOR_DELAY_UI);
        if (success) {
            isTrackingSteps = true;
            stepCount = 0; // Reset step count for the new tracking session
            Log.d(TAG, "Step detector registered successfully, tracking started");
        } else {
            Log.e(TAG, "Failed to register step detector listener");
        }
    }

    /**
     * Stops step tracking by unregistering the sensor listener and saving the step count.
     */
    public void stopStepTracking() {
        if (isTrackingSteps) {
            sensorManager.unregisterListener(stepListener);
            isTrackingSteps = false;
            savedStepCount += stepCount; // Save the current step count
            stepCount = 0; // Reset current step count
            Log.d(TAG, "Step tracking stopped, steps saved: " + savedStepCount);
        }
    }
}
