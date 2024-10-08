package com.example.pulsestepapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.model.LatLng;

public class LocationTrackingService extends Service {
    private static final String TAG = "LocationTrackingService";
    private static final int NOTIFICATION_ID = 1;
    public static final String CHANNEL_ID = "LocationTrackingChannel";

    // 定义广播动作字符串
    public static final String ACTION_PAUSE_STEP_COUNTING = "com.example.pulsestepapplication.ACTION_PAUSE_STEP_COUNTING";
    public static final String ACTION_RESUME_STEP_COUNTING = "com.example.pulsestepapplication.ACTION_RESUME_STEP_COUNTING";

    // 位置相关变量
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    // StepCounter
    private StepCounter stepCounter;

    // 广播接收器，用于接收暂停和恢复步数计数的命令
    private BroadcastReceiver serviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) return;
            switch (intent.getAction()) {
                case ACTION_PAUSE_STEP_COUNTING:
                    stepCounter.stopStepTracking();
                    Log.d(TAG, "收到ACTION_PAUSE_STEP_COUNTING");
                    break;
                case ACTION_RESUME_STEP_COUNTING:
                    stepCounter.startStepTracking();
                    Log.d(TAG, "收到ACTION_RESUME_STEP_COUNTING");
                    // 获取后台定位权限状态
                    boolean isBackgroundPermissionGranted = intent.getBooleanExtra("background_permission_granted", false);
                    // 根据权限状态调整位置更新
                    startLocationUpdates(isBackgroundPermissionGranted);
                    break;
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        setupLocationCallback();

        // 初始化StepCounter
        stepCounter = new StepCounter(this);
        stepCounter.setStepCounterListener(new StepCounter.StepCounterListener() {
            @Override
            public void onStepCountUpdated(int stepCount) {
                sendStepUpdate(stepCount);
            }

            @Override
            public void onPermissionRequired() {
                Log.e(TAG, "ACTIVITY_RECOGNITION权限是必需的");
                // 由于服务无法直接请求权限，确保在活动中请求并授予权限
                stopSelf();
            }
        });

        // 注册BroadcastReceiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_PAUSE_STEP_COUNTING);
        filter.addAction(ACTION_RESUME_STEP_COUNTING);
        LocalBroadcastManager.getInstance(this).registerReceiver(serviceReceiver, filter);

        Log.d(TAG, "服务onCreate");
    }

    /**
     * 创建前台服务的通知渠道
     */
    private void createNotificationChannel() {
        // 创建通知渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "位置追踪";
            String description = "通知用户位置追踪服务正在运行";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * 构建前台服务的通知
     *
     * @return NotificationCompat.Builder对象
     */
    private NotificationCompat.Builder getNotificationBuilder() {
        // 创建通知
        Intent notificationIntent = new Intent(this, GoogleMapActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("PulseStep Running")
                .setContentText("位置追踪服务正在后台运行")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        return builder;
    }

    /**
     * 设置位置回调以接收位置更新
     */
    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (android.location.Location location : locationResult.getLocations()) {
                    // 处理位置更新
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    sendLocationUpdate(latLng);
                    Log.d(TAG, "位置更新: " + latLng.toString());
                }
            }
        };
    }

    /**
     * 开始请求位置更新
     *
     * @param isBackgroundPermissionGranted 是否已授予后台定位权限
     */
    @SuppressLint("MissingPermission")
    private void startLocationUpdates(boolean isBackgroundPermissionGranted) {
        LocationRequest locationRequest;
        if (isBackgroundPermissionGranted) {
            // 后台定位权限已授予，使用高精度
            locationRequest = new LocationRequest.Builder(5000)
                    .setMinUpdateIntervalMillis(3000)
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .build();
        } else {
            // 后台定位权限未授予，仅前台使用，降低精度以节省电量
            locationRequest = new LocationRequest.Builder(5000)
                    .setMinUpdateIntervalMillis(3000)
                    .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                    .build();
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    /**
     * 停止请求位置更新
     */
    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    /**
     * 发送位置更新广播到活动
     *
     * @param latLng 更新的位置坐标
     */
    private void sendLocationUpdate(LatLng latLng) {
        Intent intent = new Intent("com.example.pulsestepapplication.LOCATION_UPDATE");
        intent.putExtra("lat", latLng.latitude);
        intent.putExtra("lng", latLng.longitude);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * 发送步数更新广播到活动
     *
     * @param stepCount 更新的步数
     */
    private void sendStepUpdate(int stepCount) {
        Intent intent = new Intent("com.example.pulsestepapplication.STEP_UPDATE");
        intent.putExtra("stepCount", stepCount);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 检查权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "缺少FOREGROUND_SERVICE_LOCATION权限");
                stopSelf();
                return START_NOT_STICKY;
            }
        }
        // 启动前台服务
        startForeground(NOTIFICATION_ID, getNotificationBuilder().build());

        // 获取后台定位权限状态
        boolean isBackgroundPermissionGranted = false;
        if (intent != null) {
            isBackgroundPermissionGranted = intent.getBooleanExtra("background_permission_granted", false);
        }

        // 开始位置更新
        startLocationUpdates(isBackgroundPermissionGranted);
        // 开始步数计数
        stepCounter.startStepTracking();
        Log.d(TAG, "服务onStartCommand");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
        stepCounter.stopStepTracking();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(serviceReceiver);
        Log.d(TAG, "服务被销毁");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
