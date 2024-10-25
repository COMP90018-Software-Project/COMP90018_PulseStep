package com.example.pulsestepapplication;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class JumpTrackingService extends Service {

    private static final String TAG = "JumpTrackingService";
    private static final int NOTIFICATION_ID = 1;
    public static final String CHANNEL_ID = "JumpTrackingChannel";

    private JumpCounter jumpCounter;
    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        // Initialize JumpCounter
        jumpCounter = new JumpCounter(this);
        jumpCounter.setJumpCounterListener(jumpCount -> {
            // Broadcast jump count updates to interested components
            Intent intent = new Intent("com.example.jumpcounter.JUMP_COUNT_UPDATED");
            intent.putExtra("jumpCount", jumpCount);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        });

        // Acquire a WakeLock to keep the CPU running even when the screen is off
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "JumpTrackingService:WakeLock");
        wakeLock.acquire();

        Log.d(TAG, "Service onCreate");
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Jump Tracking", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Tracks your jumps in the background");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification getNotification() {
        Intent notificationIntent = new Intent(this, JumpActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Jump Counter Running")
                .setContentText("The jump tracking service is running in the background")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, getNotification());
        jumpCounter.startJumpTracking();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        jumpCounter.stopJumpTracking();
        Log.d(TAG, "Service destroyed");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
