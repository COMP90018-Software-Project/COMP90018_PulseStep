package com.example.pulsestepapplication;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class StepTrackingService extends Service {
    private static final String TAG = "StepService";
    private static final int NOTIFICATION_ID = 1;
    public static final String CHANNEL_ID = "StepTrackingChannel";

    // Define broadcast action strings
    public static final String ACTION_PAUSE_STEP_COUNTING = "com.example.pulsestepapplication.ACTION_PAUSE_STEP_COUNTING";
    public static final String ACTION_RESUME_STEP_COUNTING = "com.example.pulsestepapplication.ACTION_RESUME_STEP_COUNTING";

    // StepCounter
    private StepCounter stepCounter;

    // BroadcastReceiver used to receive commands to pause and resume step counting
    private BroadcastReceiver serviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) return;
            switch (intent.getAction()) {
                case ACTION_PAUSE_STEP_COUNTING:
                    stepCounter.stopStepTracking();
                    Log.d(TAG, "Received ACTION_PAUSE_STEP_COUNTING");
                    break;
                case ACTION_RESUME_STEP_COUNTING:
                    stepCounter.startStepTracking();
                    Log.d(TAG, "Received ACTION_RESUME_STEP_COUNTING");
                    break;
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        // Initialize StepCounter
        stepCounter = new StepCounter(this);
        stepCounter.setStepCounterListener(new StepCounter.StepCounterListener() {
            @Override
            public void onStepCountUpdated(int stepCount) {
                sendStepUpdate(stepCount);
            }

            @Override
            public void onPermissionRequired() {
                Log.e(TAG, "ACTIVITY_RECOGNITION permission required");
                // Because the service cannot directly request permissions, ensure that permissions are requested and granted in the activity
                stopSelf();
            }
        });

        // Register BroadcastReceiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_PAUSE_STEP_COUNTING);
        filter.addAction(ACTION_RESUME_STEP_COUNTING);
        LocalBroadcastManager.getInstance(this).registerReceiver(serviceReceiver, filter);

        Log.d(TAG, "Service onCreate");
    }

    private void createNotificationChannel() {
        // Create notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Step Tracking";
            String description = "Notifies the user that the step tracking service is running";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private NotificationCompat.Builder getNotificationBuilder() {
        // Create notification
        Intent notificationIntent = new Intent(this, MainActivity.class); // Adjust target activity as needed
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("PulseStep Running")
                .setContentText("Step tracking service is running in the background")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        return builder;
    }

    private void sendStepUpdate(int stepCount) {
        Intent intent = new Intent("com.example.pulsestepapplication.STEP_UPDATE");
        intent.putExtra("stepCount", stepCount);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, getNotificationBuilder().build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH);
        } else {
            startForeground(NOTIFICATION_ID, getNotificationBuilder().build());
        }

        // Start step counting
        //stepCounter.startStepTracking();
        Log.d(TAG, "Service onStartCommand");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stepCounter.stopStepTracking();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(serviceReceiver);
        Log.d(TAG, "Service destroyed");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
