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

    // Define broadcast action strings
    public static final String ACTION_PAUSE_STEP_COUNTING = "com.example.pulsestepapplication.ACTION_PAUSE_STEP_COUNTING";
    public static final String ACTION_RESUME_STEP_COUNTING = "com.example.pulsestepapplication.ACTION_RESUME_STEP_COUNTING";

    // Location-related variables
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    // StepCounter
    private StepCounter stepCounter;

    // BroadcastReceiver to receive pause and resume commands for step counting
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
                    // Get the background location permission status
                    boolean isBackgroundPermissionGranted = intent.getBooleanExtra("background_permission_granted", false);
                    // Adjust location updates based on permission status
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

        // Initialize StepCounter
        stepCounter = new StepCounter(this);
        stepCounter.setStepCounterListener(new StepCounter.StepCounterListener() {
            @Override
            public void onStepCountUpdated(int stepCount) {
                sendStepUpdate(stepCount);
            }

            @Override
            public void onPermissionRequired() {
                Log.e(TAG, "ACTIVITY_RECOGNITION permission is required");
                // Since the service cannot directly request permissions, ensure that permission is requested and granted in the activity
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

    /**
     * Create the notification channel for the foreground service
     */
    private void createNotificationChannel() {
        // Create notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Location Tracking";
            String description = "Notifies users that the location tracking service is running";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * Build the notification for the foreground service
     *
     * @return NotificationCompat.Builder object
     */
    private NotificationCompat.Builder getNotificationBuilder() {
        // Create the notification
        Intent notificationIntent = new Intent(this, GoogleMapActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("PulseStep Running")
                .setContentText("Location tracking service is running in the background")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        return builder;
    }

    /**
     * Set up the location callback to receive location updates
     */
    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (android.location.Location location : locationResult.getLocations()) {
                    // Handle location updates
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    sendLocationUpdate(latLng);
                    Log.d(TAG, "Location update: " + latLng.toString());
                }
            }
        };
    }

    /**
     * Start requesting location updates
     *
     * @param isBackgroundPermissionGranted Whether background location permission has been granted
     */
    @SuppressLint("MissingPermission")
    private void startLocationUpdates(boolean isBackgroundPermissionGranted) {
        LocationRequest locationRequest;
        if (isBackgroundPermissionGranted) {
            // Background location permission granted, use high accuracy
            locationRequest = new LocationRequest.Builder(5000)
                    .setMinUpdateIntervalMillis(3000)
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .build();
        } else {
            // Background location permission not granted, only foreground use, reduce accuracy to save battery
            locationRequest = new LocationRequest.Builder(5000)
                    .setMinUpdateIntervalMillis(3000)
                    .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                    .build();
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    /**
     * Stop requesting location updates
     */
    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    /**
     * Send location update broadcast to the activity
     *
     * @param latLng Updated location coordinates
     */
    private void sendLocationUpdate(LatLng latLng) {
        Intent intent = new Intent("com.example.pulsestepapplication.LOCATION_UPDATE");
        intent.putExtra("lat", latLng.latitude);
        intent.putExtra("lng", latLng.longitude);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * Send step update broadcast to the activity
     *
     * @param stepCount Updated step count
     */
    private void sendStepUpdate(int stepCount) {
        Intent intent = new Intent("com.example.pulsestepapplication.STEP_UPDATE");
        intent.putExtra("stepCount", stepCount);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Check permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Missing FOREGROUND_SERVICE_LOCATION permission");
                stopSelf();
                return START_NOT_STICKY;
            }
        }
        // Start foreground service
        startForeground(NOTIFICATION_ID, getNotificationBuilder().build());

        // Get the background location permission status
        boolean isBackgroundPermissionGranted = false;
        if (intent != null) {
            isBackgroundPermissionGranted = intent.getBooleanExtra("background_permission_granted", false);
        }

        // Start location updates
        startLocationUpdates(isBackgroundPermissionGranted);
        // Start step counting
        //stepCounter.startStepTracking();
        Log.d(TAG, "Service onStartCommand");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
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
