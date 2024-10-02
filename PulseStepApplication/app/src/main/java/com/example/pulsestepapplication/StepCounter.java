package com.example.pulsestepapplication;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class StepCounter {
    private final SensorManager sensorManager;
    private final Sensor stepDetectorSensor;
    private SensorEventListener stepListener;
    private int stepCount = 0; // 本次跟踪的步数
    private int savedStepCount = 0; // 保存的步数
    private boolean isTrackingSteps = false;
    private StepCounterListener stepCounterListener;
    private final Context context;
    private final Activity activity;
    private static final int REQUEST_CODE = 1001; // 权限请求代码

    public interface StepCounterListener {
        void onStepCountUpdated(int stepCount);
    }

    public StepCounter(Activity activity) {
        this.context = activity;
        this.activity = activity;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

        if (stepDetectorSensor == null) {
            Log.e("StepCounter", "步数检测器不可用！");
            return;
        }

        stepListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                // 确保只有在 isTrackingSteps 为 true 时才更新步数
                if (isTrackingSteps) {
                    stepCount++;
                    int currentSteps = savedStepCount + stepCount;

                    Log.d("StepCounter", "步数更新: " + currentSteps);

                    if (stepCounterListener != null) {
                        stepCounterListener.onStepCountUpdated(currentSteps);
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                // 不需要处理精度变化
            }
        };
    }

    public void setStepCounterListener(StepCounterListener listener) {
        if (listener != null) {
            this.stepCounterListener = listener;
        } else {
            Log.e("StepCounter", "传入的 StepCounterListener 为空");
        }
    }

    public void startStepTracking() {
        if (stepDetectorSensor == null) {
            Log.e("StepCounter", "步数检测器不可用");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACTIVITY_RECOGNITION)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.e("StepCounter", "缺少 ACTIVITY_RECOGNITION 权限");
                ActivityCompat.requestPermissions(activity, new String[]{android.Manifest.permission.ACTIVITY_RECOGNITION}, REQUEST_CODE);
                return;
            }
        }

        // 注册传感器监听器
        boolean success = sensorManager.registerListener(stepListener, stepDetectorSensor, SensorManager.SENSOR_DELAY_UI);
        if (success) {
            isTrackingSteps = true; // 设置为正在跟踪
            stepCount = 0; // 开始新一次跟踪，重置步数
            Log.d("StepCounter", "步数检测器已注册成功, 开始计步");
        } else {
            Log.e("StepCounter", "无法注册步数检测器监听器");
        }
    }

    public void stopStepTracking() {
        if (isTrackingSteps) {
            // 取消传感器监听器注册
            sensorManager.unregisterListener(stepListener);
            isTrackingSteps = false; // 设置为停止跟踪
            savedStepCount += stepCount; // 保存当前步数
            stepCount = 0; // 重置本次跟踪的步数
            Log.d("StepCounter", "停止步数追踪, 保存步数: " + savedStepCount);
        }
    }

    public void resetStepTracking() {
        isTrackingSteps = false;
        stepCount = 0; // 重置本次跟踪的步数
        savedStepCount = 0; // 重置保存的步数
        Log.d("StepCounter", "步数重置");
    }

    public void registerListener() {
        startStepTracking();
    }

    public void unregisterListener() {
        stopStepTracking();
    }
}
