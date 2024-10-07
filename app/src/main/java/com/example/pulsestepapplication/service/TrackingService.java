package com.example.pulsestepapplication.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.pulsestepapplication.AmapActivity;

public class TrackingService extends Service {

    private Handler timerHandler = new Handler();
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (AmapActivity.isTracking && !AmapActivity.isPaused) {
                long millis = SystemClock.elapsedRealtime() - AmapActivity.startTime;
                int seconds = (int) (millis / 1000);
                int minutes = seconds / 60;
                seconds %= 60;
                // Send time update to activity via broadcast
                sendTimeUpdate(String.format("%02d:%02d", minutes, seconds));
            }
            timerHandler.postDelayed(this, 1000);
        }
    };

    private void startTracking() {
        timerHandler.postDelayed(timerRunnable, 0);
    }


    private void sendTimeUpdate(String time) {
        Intent intent = new Intent("com.example.pulsestepapplication.UPDATE_TIME");
        intent.putExtra("time", time);
        sendBroadcast(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startTracking();
        return START_STICKY;
    }

    private void pauseTracking() {
        if (AmapActivity.isTracking) {
            AmapActivity.isPaused = true;
            AmapActivity.pauseTime = SystemClock.elapsedRealtime();
            timerHandler.removeCallbacks(timerRunnable);
        }
    }

    private void resumeTracking() {
        if (AmapActivity.isTracking) {
            AmapActivity.isPaused = false;
            AmapActivity.startTime += (SystemClock.elapsedRealtime() - AmapActivity.pauseTime);
            timerHandler.postDelayed(timerRunnable, 0);
        }
    }

    private final BroadcastReceiver pauseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            pauseTracking();
        }
    };

    private final BroadcastReceiver resumeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            resumeTracking();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.example.pulsestepapplication.PAUSE_TRACKING");
        filter.addAction("com.example.pulsestepapplication.RESUME_TRACKING");
        registerReceiver(pauseReceiver, filter);
        registerReceiver(resumeReceiver, filter);
        startTracking();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}