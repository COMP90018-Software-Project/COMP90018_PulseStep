package com.example.pulsestepapplication;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import androidx.core.app.ActivityCompat;

import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.FusedLocationProviderClient;

import com.google.android.gms.location.LocationCallback;

import com.google.android.gms.location.LocationRequest;

import com.google.android.gms.location.LocationServices;

import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.Priority;

import com.google.android.gms.maps.CameraUpdateFactory;

import com.google.android.gms.maps.GoogleMap;

import com.google.android.gms.maps.OnMapReadyCallback;

import com.google.android.gms.maps.SupportMapFragment;

import com.google.android.gms.maps.model.CameraPosition;

import com.google.android.gms.maps.model.LatLng;

import com.google.android.gms.maps.model.MapStyleOptions;

import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.util.ArrayList;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class GoogleMapActivity extends AppCompatActivity implements OnMapReadyCallback {
    // Constants
    private static final String TAG = "GoogleMapActivity";
    private static final int LOCATION_REQUEST_CODE = 1001;
    private static final int ACTIVITY_RECOGNITION_REQUEST_CODE = 1002;
    private static final int BACKGROUND_LOCATION_REQUEST_CODE = 1003; // Unique request code
    private static final float MOVE_ZOOM_LEVEL = 16f;
    private static final float DEFAULT_ZOOM_LEVEL = 15f;
    private static final float MAX_ZOOM_LEVEL = 19f;
    private static final float DISTANCE_THRESHOLD_METERS = 1.0f; // Distance threshold in meters

    // UI Components
    private ImageButton btnPauseResume;
    private TextView timerTextView, stepTextView, avgPaceTextView;
    private ImageButton btnShow;
    private ImageView backButton;
    private ImageView mapImageView;
    private TextView cTextView;
    private ImageView waitView;
    private TextView waitTextView;

    // Map and Location
    private GoogleMap googleMap;
    private double initialLatitude;
    private double initialLongitude;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    // Tracking Variables
    private final List<List<LatLng>> allPathPoints = new ArrayList<>(); // Modified to hold segments
    private List<LatLng> pathPoints; // Modified to be a segment
    private final List<Polyline> polyLines = new ArrayList<>();
    private final List<LatLng> trajectory = new ArrayList<>();
    private boolean isTracking = false;
    private boolean isPaused = false;
    private boolean isFirstStart = true;
    private boolean isLocationReady = false;
    private float totalDistance = 0.0f;
    private int currentStepCount = 0;
    private static final Double realDistance = 0.05;
    private static final double metValue = 8.0;
    private static final int LOCATION_TIMEOUT = 10000; // Location timeout in milliseconds

    //Button
    private boolean isLongPress = false;
    private Handler handler = new Handler();
    private ProgressBar progressBar;
    private int progressStatus = 0;
    private boolean isRunning = false;
    private boolean hasTriggeredSuccess = false;

    // Timer Variables
    private long startTime = 0L;
    private long pauseTime = 0L;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private long elapsedTime;
    private final Runnable timerRunnable = new Runnable() {
        @SuppressLint("DefaultLocale")
        @Override
        public void run() {
            long millis = SystemClock.elapsedRealtime() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds %= 60;
            timerTextView.setText(String.format("%02d:%02d", minutes, seconds));
            elapsedTime = millis;

            // Update average pace in non-map mode
            if (!isMapMode) {
                updateAvgPaceNoMapMode();
            }
            timerHandler.postDelayed(this, 1000);
        }
    };

    // Location timeout handling
    private final Handler locationTimeoutHandler = new Handler(Looper.getMainLooper());
    private final Runnable locationTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isLocationReady) {
                isLocationReady = true;
                waitView.clearAnimation();
                waitView.setVisibility(View.GONE);
                waitTextView.setVisibility(View.GONE);
                btnPauseResume.setClickable(true);
                if (ActivityCompat.checkSelfPermission(GoogleMapActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(GoogleMapActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                googleMap.setMyLocationEnabled(true);
                Toast.makeText(GoogleMapActivity.this, "Location timeout, weak signal!", Toast.LENGTH_LONG).show();
            }
        }
    };

    // Mode flags: map mode is true, non-map mode is false
    private boolean isMapMode;
    private boolean isServiceRunning = false;

    private String userName;
    private int userAge;
    private double userWeight;
    private Geocoder geocoder;

    // SharedPreferences related
    private static final String PREFS_NAME = "LocationPrefs";
    private static final String KEY_HAS_DENIED_BACKGROUND_PERMISSION = "hasDeniedBackgroundPermission";
    private SharedPreferences sharedPreferences;

    // Flags to track permission states
    private boolean hasRequestedBackgroundPermission = false;
    private boolean hasDeniedBackgroundPermission = false;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_map);

        // Get mode
        Intent intent = getIntent();
        isMapMode = intent.getBooleanExtra("MAP_MODE", true); // Default to map mode
        userName = intent.getStringExtra("name");
        userAge = intent.getIntExtra("age", 25);
        userWeight = intent.getDoubleExtra("weight", 70.0);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        hasDeniedBackgroundPermission = sharedPreferences.getBoolean(KEY_HAS_DENIED_BACKGROUND_PERMISSION, false);

        // Initialize UI Components
        initializeUIComponents();

        // Check permissions
        checkPermissions();
    }

    /**
     * Initialize UI Components by finding them by ID
     */
    private void initializeUIComponents() {
        btnPauseResume = findViewById(R.id.btn_stop);
        btnShow = findViewById(R.id.btn_show);
        timerTextView = findViewById(R.id.timer_text_view);
        stepTextView = findViewById(R.id.step_text_view);
        avgPaceTextView = findViewById(R.id.avg_text_view);
        cTextView = findViewById(R.id.calories_text_view);
        backButton = findViewById(R.id.back_button_running_page);
        mapImageView = findViewById(R.id.default_image_view);
        waitView = findViewById(R.id.wait);
        waitTextView = findViewById(R.id.waitText);
        // Initially hide the map
        View mapFragment = findViewById(R.id.google_map);
        if (mapFragment != null) {
            mapFragment.setVisibility(View.GONE);  // Initially hide the map
        }
        // Set up back button click listener
        backButton.setOnClickListener(v -> popUpConfirmDialog());

        if (isMapMode) {
            // Start rotation animation
            Animation rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate);
            waitView.startAnimation(rotateAnimation);
            waitView.setVisibility(View.VISIBLE);
            waitTextView.setVisibility(View.VISIBLE);
            btnPauseResume.setClickable(false);
        } else {
            waitView.setVisibility(View.GONE);
            waitTextView.setVisibility(View.GONE);
            // Show default map
            if (mapFragment != null) {
                mapFragment.setVisibility(View.VISIBLE);  // Show map when location is ready
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            if (googleMap != null) {
                googleMap.setMyLocationEnabled(true);
            }
            btnPauseResume.setClickable(true);
        }
    }

    /**
     * Shows a confirmation dialog to exit the current running activity.
     * - "Yes" will finish the activity and navigate to the WorkoutFragment.
     * - "No" will close the dialog without exiting.
     */
    private void popUpConfirmDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_custom, null);
        builder.setView(dialogView);
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();

        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams layoutParams = window.getAttributes();
            layoutParams.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.8);

            int offsetInDp = 100;
            float scale = getResources().getDisplayMetrics().density;
            layoutParams.y = (int) (offsetInDp * scale + 0.5f);
            layoutParams.dimAmount = 0.7f;
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

            window.setAttributes(layoutParams);
        }

        Button positiveButton = dialogView.findViewById(R.id.positive_button);
        Button negativeButton = dialogView.findViewById(R.id.negative_button);

        positiveButton.setOnClickListener(v -> {
            Intent intent = new Intent();
            setResult(RESULT_OK, intent);  // Set the result to pass back to MainActivity
            finish();  // Close GoogleMapActivity and return to the previous Activity (WorkoutFragment)
            dialog.dismiss();
        });

        negativeButton.setOnClickListener(v -> dialog.dismiss());
    }

    /**
     * Update the visibility of the "Grant Permissions" button after permission state changes
     */
    private void handlePermissionChanges() {
        if (isMapMode) {
            // Check if location permission has been revoked
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // Permission revoked, switch to non-map mode or other handling
                isMapMode = false;
                if (googleMap != null) {
                    googleMap.clear();
                }
                showDefaultMap();
            }
        }
        // Check activity recognition permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                // Permission revoked, navigate back to MainActivity
                Toast.makeText(this, "Activity recognition permission is required", Toast.LENGTH_SHORT).show();
                navigateToWorkoutPage();
            }
        }

    }

    /**
     * Navigate back to MainActivity
     */
    private void navigateToMainActivity() {
        Intent intent = new Intent(GoogleMapActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * Check and request necessary permissions
     */
    private void checkPermissions() {
        // Check activity recognition permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                // Request activity recognition permission
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, ACTIVITY_RECOGNITION_REQUEST_CODE);
                return;
            }
        }

        if (isMapMode) {
            // Check location permissions
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // Request location permissions
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
                return;
            }
        }

        // Permissions granted, continue setup
        setupActivity();
    }

    /**
     * Setup activity based on selected mode
     */
    private void setupActivity() {
        // If it's map mode, initialize the map
        if (isMapMode) {
            setupMapFragment();
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            setupLocationCallback();
        } else {
            showDefaultMap();
        }

        // Set button listeners
        setupButtonListeners();

        // Start tracking service
        startTrackingService();
    }

    /**
     * Setup SupportMapFragment and asynchronously initialize the map
     */
    private void setupMapFragment() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.google_map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Log.e(TAG, "Map fragment is null");
        }
    }

    /**
     * Setup button click listeners
     */
    @SuppressLint("ClickableViewAccessibility")
    private void setupButtonListeners() {
        progressBar = findViewById(R.id.progressBar);
        btnPauseResume.setOnClickListener(v -> handleStartStopButtonClick());
        CircularProgressDrawable circularProgressDrawable = new CircularProgressDrawable(this);
        circularProgressDrawable.setColor(ContextCompat.getColor(this, R.color.light_orange));
        progressBar.setProgressDrawable(circularProgressDrawable);
        btnShow.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (isRunning || hasTriggeredSuccess) {
                        return true;
                    }
                    isRunning = true;


                    progressBar.setVisibility(View.VISIBLE);
                    progressStatus = 0;
                    circularProgressDrawable.setProgress(progressStatus);

                    if (!hasTriggeredSuccess) {
                        ObjectAnimator scaleXDown = ObjectAnimator.ofFloat(btnShow, "scaleX", 1f, 1.3f);
                        ObjectAnimator scaleYDown = ObjectAnimator.ofFloat(btnShow, "scaleY", 1f, 1.3f);
                        AnimatorSet animatorSetDown = new AnimatorSet();
                        animatorSetDown.playTogether(scaleXDown, scaleYDown);
                        animatorSetDown.setDuration(2000);
                        animatorSetDown.start();
                    }

                    new Thread(new Runnable() {
                        public void run() {
                            while (progressStatus < 100 && isRunning) {
                                progressStatus += 1;
                                handler.post(new Runnable() {
                                    public void run() {
                                        circularProgressDrawable.setProgress(progressStatus);
                                    }
                                });
                                try {
                                    Thread.sleep(20);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }

                            if (progressStatus >= 100 && isRunning) {
                                isLongPress = true;
                                handler.post(() -> {
                                    showLastTrack();
                                    isRunning = false;
                                    hasTriggeredSuccess = true;
                                });
                            }
                        }
                    }).start();
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (!hasTriggeredSuccess) {
                        ObjectAnimator scaleXUp = ObjectAnimator.ofFloat(btnShow, "scaleX", 1.3f, 1f);
                        ObjectAnimator scaleYUp = ObjectAnimator.ofFloat(btnShow, "scaleY", 1.3f, 1f);
                        AnimatorSet animatorSetUp = new AnimatorSet();
                        animatorSetUp.playTogether(scaleXUp, scaleYUp);
                        animatorSetUp.setDuration(300);
                        animatorSetUp.start();
                    }
                    isRunning = false;
                    if (progressStatus < 100) {
                        progressBar.setVisibility(View.GONE);
                        handler.removeCallbacksAndMessages(null);
                        progressStatus = 0;
                    }
                    break;
            }
            return true;
        });

    }

    /**
     * Handle the start/pause button click event
     */
    private void handleStartStopButtonClick() {
        if (isTracking) {
            pauseTracking();
        } else {
            if (isMapMode && !isLocationReady) {
                Toast.makeText(this, "Getting location, please wait...", Toast.LENGTH_SHORT).show();
                return;
            }
            if (isMapMode) {
                if (isBackgroundLocationPermissionGranted()) {
                    // Background location permission granted, resume tracking
                    resumeTracking();
                } else {
                    if (!hasRequestedBackgroundPermission && !hasDeniedBackgroundPermission) {
                        // Background location permission has not been requested yet, request it
                        checkAndRequestBackgroundLocationPermission();
                    } else if (hasDeniedBackgroundPermission) {
                        // User has denied background location permission, show guidance dialog
                        // showPermissionDeniedDialog();
                    }
                    // Even if background location permission is not granted, foreground tracking is still allowed
                    resumeTracking();
                }
            } else {
                // Non-map mode, resume tracking normally
                resumeTracking();
            }
        }
    }

    /**
     * Check and request background location permission
     */
    private void checkAndRequestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Check if background location permission is granted
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // Check if we should show the permission request rationale
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                    // Show permission request rationale
                    showBackgroundPermissionRationale();
                } else {
                    // Request permission directly
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, BACKGROUND_LOCATION_REQUEST_CODE);
                }
            }
        }
    }

    private void showBackgroundPermissionRationale() {
        // Inflate the custom dialog layout
        View dialogView = getLayoutInflater().inflate(R.layout.custom_dialog, null);

        // Initialize the dialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialog);
        builder.setView(dialogView);
        builder.setCancelable(false); // Prevent dismissal on outside touch

        // Create the AlertDialog
        AlertDialog dialog = builder.create();

        // Initialize buttons
        Button btnAllow = dialogView.findViewById(R.id.btn_positive);
        Button btnDeny = dialogView.findViewById(R.id.btn_negative);

        // Set click listeners
        btnAllow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityCompat.requestPermissions(GoogleMapActivity.this,
                        new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                        BACKGROUND_LOCATION_REQUEST_CODE);
                dialog.dismiss();
            }
        });

        btnDeny.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hasDeniedBackgroundPermission = true;
                sharedPreferences.edit().putBoolean(KEY_HAS_DENIED_BACKGROUND_PERMISSION, true).apply();
                Toast.makeText(GoogleMapActivity.this,
                        "Background location permission denied. The app will stop tracking in the background.",
                        Toast.LENGTH_LONG).show();
                dialog.dismiss();
            }
        });

        // Show the dialog before modifying window attributes
        dialog.show();

        // Modify the dialog window to position it at the bottom
        Window window = dialog.getWindow();
        if (window != null) {
            // Remove default background to apply custom background with rounded corners
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            // Set dialog to match parent width
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            //params.height = 700;
            params.gravity = Gravity.BOTTOM;
            window.setAttributes(params);

            // Optional: Add animations
            window.getAttributes().windowAnimations = R.style.DialogAnimation; // Define in styles.xml
        }
    }

    /**
     * Show a dialog when permission is denied, guiding the user to manually grant permission in settings
     */
    private void showPermissionDeniedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Background location permission denied")
                .setMessage("To enable background tracking, please allow background location permission in app settings.")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    // Open app settings page
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", getPackageName(), null));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    Toast.makeText(this, "Background location permission denied, the app will stop tracking in the background.", Toast.LENGTH_LONG).show();
                })
                .create()
                .show();
    }

    /**
     * Starts the tracking process.
     */
    @SuppressLint({"MissingPermission", "UseCompatLoadingForDrawables"})
    private void resumeTracking() {
        // If already tracking and not paused, do nothing
        if (isTracking && !isPaused) {
            Log.d(TAG, "resumeTracking() already tracking and not paused, skipping execution.");
            return;
        }

        isTracking = true;
        isPaused = false;

        if (isFirstStart) {
            // Start a new pathPoints list for the new segment
            pathPoints = new ArrayList<>();
            allPathPoints.add(pathPoints);

            totalDistance = 0.0f;
            startTime = SystemClock.elapsedRealtime();
            timerHandler.postDelayed(timerRunnable, 0);
            isFirstStart = false;

            if (isMapMode) {
                // Get the last known location and move the camera
                fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                    if (location != null) {
                        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, MOVE_ZOOM_LEVEL));
                    }
                });
            }
        } else {
            // Only adjust startTime when resuming from pause
            long pauseDuration = SystemClock.elapsedRealtime() - pauseTime;
            startTime += pauseDuration;
            timerHandler.postDelayed(timerRunnable, 0);

            // Start a new pathPoints list for the new segment
            pathPoints = new ArrayList<>();
            allPathPoints.add(pathPoints);
        }

        btnPauseResume.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pause));
        btnShow.setVisibility(View.GONE);

        // Send broadcast to the service to resume step counting
        Intent resumeIntent = new Intent(LocationTrackingService.ACTION_RESUME_STEP_COUNTING);
        // Pass whether background location permission is granted
        resumeIntent.putExtra("background_permission_granted", isBackgroundLocationPermissionGranted());
        LocalBroadcastManager.getInstance(this).sendBroadcast(resumeIntent);
    }

    /**
     * Pause tracking
     */
    private void pauseTracking() {
        isTracking = false;
        isPaused = true;
        pauseTime = SystemClock.elapsedRealtime();
        timerHandler.removeCallbacks(timerRunnable);
        btnPauseResume.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.start));
        btnShow.setVisibility(View.VISIBLE);
        trajectory.add(null);
        // Draw the current polyline and clear the current pathPoints
        drawCurrentPolyline();
        pathPoints = null;

        // Send broadcast to the service to pause step counting
        Intent pauseIntent = new Intent(LocationTrackingService.ACTION_PAUSE_STEP_COUNTING);
        LocalBroadcastManager.getInstance(this).sendBroadcast(pauseIntent);
    }

    /**
     * Start the tracking service
     */
    private void startTrackingService() {
        Intent serviceIntent = new Intent(this, LocationTrackingService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
        isServiceRunning = true;
    }

    /**
     * Stop the tracking service
     */
    private void stopTrackingService() {
        Intent serviceIntent = new Intent(this, LocationTrackingService.class);
        stopService(serviceIntent);
        isServiceRunning = false;
    }

    /**
     * Show the default map (non-map mode)
     */
    private void showDefaultMap() {
        // Show default image
        mapImageView.setImageResource(R.drawable.bg_workout);
        mapImageView.setVisibility(View.VISIBLE);
        // Hide map fragment
        View mapFragment = findViewById(R.id.google_map);
        if (mapFragment != null) {
            mapFragment.setVisibility(View.GONE);
        }
    }

    /**
     * Called when the map is ready to configure map settings
     *
     * @param map Prepared GoogleMap object
     */
    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        applyCustomMapStyle();
        googleMap.setMaxZoomPreference(MAX_ZOOM_LEVEL);
        // Set initial camera position to the user's last known location
        Intent intent = getIntent();
        initialLatitude = intent.getDoubleExtra("LATITUDE", 0.0);
        initialLongitude = intent.getDoubleExtra("LONGITUDE", 0.0);
        if (initialLatitude != 0.0 && initialLongitude != 0.0) {
            LatLng initialLatLng = new LatLng(initialLatitude, initialLongitude);
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(initialLatLng)   // Set the center of the map
                    .zoom(DEFAULT_ZOOM_LEVEL) // Set zoom level
                    .tilt(0)                // Set tilt angle to 0 for 2D view
                    .build();

            googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
        if (ActivityCompat.checkSelfPermission(GoogleMapActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        googleMap.setMyLocationEnabled(false);
        googleMap.setBuildingsEnabled(false);

        // Start requesting location updates
        onLocationPermissionGranted();
    }

    /**
     * Apply custom map style
     */
    private void applyCustomMapStyle() {
        try {
            boolean success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style));
            if (!success) {
                Log.e(TAG, "Map style parsing failed.");
            } else {
                Log.d(TAG, "Map style applied successfully.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Map style resource not found", e);
        }
    }

    /**
     * Set location callback
     */
    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @SuppressLint("MissingPermission")
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                // Update location regardless of tracking state
                for (Location location : locationResult.getLocations()) {
                    if (location.hasAccuracy() && location.getAccuracy() < 50.0) {
                        isLocationReady = true;
                        waitView.clearAnimation();
                        waitView.setVisibility(View.GONE);
                        waitTextView.setVisibility(View.GONE);
                        // Once the location is ready, show the map
                        View mapFragment = findViewById(R.id.google_map);
                        if (mapFragment != null) {
                            mapFragment.setVisibility(View.VISIBLE);  // Show map after location is ready
                        }
                        btnPauseResume.setClickable(true);
                        if (ActivityCompat.checkSelfPermission(GoogleMapActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(GoogleMapActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                        googleMap.setMyLocationEnabled(true);
                        locationTimeoutHandler.removeCallbacks(locationTimeoutRunnable);
                        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        if (isFirstStart) {
                            // Initially move the camera to the current location
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, DEFAULT_ZOOM_LEVEL));
                        }
                    } else {
                        if (!isLocationReady) {
                            Log.d(TAG, "Location accuracy insufficient, trying again...");
                        }
                    }

                    if (isTracking && !isPaused && isLocationReady) {
                        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        updatePath(currentLatLng);
                        CameraPosition cameraPosition = new CameraPosition.Builder()
                                .target(currentLatLng)
                                .zoom(MOVE_ZOOM_LEVEL)
                                .tilt(0)
                                .bearing(0)
                                .build();
                        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 200, null);
                    }
                }
            }
        };
    }

    /**
     * Start requesting location updates
     */
    @SuppressLint("MissingPermission")
    private void onLocationPermissionGranted() {
        if (googleMap != null) {
            googleMap.setMyLocationEnabled(false);
            googleMap.setBuildingsEnabled(false);

            // Start requesting location updates
            requestLocationUpdates();
            // Start location timeout handling
            locationTimeoutHandler.postDelayed(locationTimeoutRunnable, LOCATION_TIMEOUT);
        }
    }

    /**
     * Request location updates
     */
    @SuppressLint("MissingPermission")
    private void requestLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(7000)
                .setMinUpdateIntervalMillis(3000)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setMinUpdateDistanceMeters(5)
                .build();

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    /**
     * Update the path based on the new location and calculate the total distance
     *
     * @param latLng New location coordinates
     */
    @SuppressLint("DefaultLocale")
    private void updatePath(LatLng latLng) {
        if (!isTracking || isPaused || pathPoints == null) {
            return;
        }

        // If pathPoints is empty, handle the first location point
        if (pathPoints.isEmpty()) {
            pathPoints.add(latLng); // Directly add the first point but do not draw yet
            trajectory.add(latLng);
            return; // Skip drawing, wait for the next point
        }

        // Calculate the distance between the current point and the last added point
        LatLng lastLatLng = pathPoints.get(pathPoints.size() - 1);
        float[] results = new float[1];
        Location.distanceBetween(lastLatLng.latitude, lastLatLng.longitude, latLng.latitude, latLng.longitude, results);

        // Check if the distance between the two points is significant (> 1 meter)
        if (results[0] > DISTANCE_THRESHOLD_METERS) {
            // Only update total distance if the user moved beyond the threshold
            totalDistance += results[0];
            double totalDistanceKm = totalDistance / 1000.0;
            double totalTimeMinutes = elapsedTime / (1000.0 * 60.0);

            // Ensure both distance and time are valid before calculating pace
            if (totalDistanceKm > realDistance && totalTimeMinutes > 0) {
                double avgPace = totalTimeMinutes / totalDistanceKm;
                double elapsedTimeInMinutes = elapsedTime / 60000.0;
                double caloriesBurned = calculateCalories(userWeight, elapsedTimeInMinutes, metValue);
                runOnUiThread(() -> cTextView.setText(String.format("%d", Math.round(caloriesBurned))));
                // Check if the pace is within a reasonable range
                if (avgPace >= 1.0 && avgPace <= 30.0) {
                    runOnUiThread(() -> avgPaceTextView.setText(String.format("%d'%02d\"", (int) avgPace, (int) ((avgPace * 60) % 60))));
                } else {
                    runOnUiThread(() -> avgPaceTextView.setText("--'--\""));
                }
            } else {
                runOnUiThread(() -> avgPaceTextView.setText("--'--\""));
            }

            // Add the new point to the current pathPoints
            pathPoints.add(latLng);
            trajectory.add(latLng);

            // Draw the polyline
            drawCurrentPolyline();
        }
    }

    /**
     * Draw the current polylines for all segments
     */
    private void drawCurrentPolyline() {
        if (googleMap == null) return;

        // Remove existing polylines from the map
        for (Polyline polyline : polyLines) {
            polyline.remove();
        }
        polyLines.clear();

        // Draw each segment separately
        for (List<LatLng> segment : allPathPoints) {
            if (!segment.isEmpty()) {
                PolylineOptions polylineOptions = new PolylineOptions()
                        .addAll(segment)
                        .color(getResources().getColor(R.color.like_orange))
                        .width(20);
                Polyline polyline = googleMap.addPolyline(polylineOptions);
                polyLines.add(polyline);
            }
        }
    }

    /**
     * Update average pace in non-map mode based on steps and time
     */
    @SuppressLint("DefaultLocale")
    private void updateAvgPaceNoMapMode() {
        // Assuming average step length is 0.75 meters
        float averageStepLength = 0.75f;
        float distance = currentStepCount * averageStepLength; // in meters
        double distanceKm = distance / 1000.0;
        double totalTimeMinutes = elapsedTime / (1000.0 * 60.0);
        if (distanceKm > realDistance && totalTimeMinutes > 0) {
            double avgPace = totalTimeMinutes / distanceKm;
            double elapsedTimeInMinutes = elapsedTime / 60000.0;
            double caloriesBurned = calculateCalories(userWeight, elapsedTimeInMinutes, metValue);
            runOnUiThread(() -> cTextView.setText(String.format("%d", Math.round(caloriesBurned))));
            // Update avgPaceTextView
            runOnUiThread(() -> avgPaceTextView.setText(String.format("%d'%02d\"", (int) avgPace, (int) ((avgPace * 60) % 60))));
        } else {
            runOnUiThread(() -> avgPaceTextView.setText("--'--\""));
        }
    }

    /**
     * Calculate the calories burned based on weight, duration, and MET value
     *
     * @param weight            User weight in kilograms
     * @param durationInMinutes Duration of activity in minutes
     * @param metValue          MET value of the activity
     * @return Calories burned
     */
    private double calculateCalories(double weight, double durationInMinutes, double metValue) {
        double durationInHours = durationInMinutes / 60.0;
        return metValue * weight * durationInHours;
    }

    /**
     * Convert LatLng point to a readable address string
     *
     * @param latLng Location coordinates
     * @return String containing country and city, or "unknown location"
     */
    private String getAddressFromLatLng(LatLng latLng) {
        String address = "Unknown location";

        // Ensure Geocoder is initialized
        if (geocoder == null) {
            if (Geocoder.isPresent()) {
                geocoder = new Geocoder(this, Locale.getDefault());
            } else {
                Log.e(TAG, "Geocoder is not available.");
                return address;
            }
        }

        try {
            // Get address from latitude and longitude
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);

            if (addresses != null && !addresses.isEmpty()) {
                Address addr = addresses.get(0);
                String country = addr.getCountryName(); // Country
                String city = addr.getLocality();       // City

                if (country != null && city != null) {
                    address = country + ", " + city;
                } else if (country != null) {
                    address = country;
                } else if (city != null) {
                    address = city;
                }
            } else {
                Log.e(TAG, "No address found for location.");
            }
        } catch (IOException e) {
            Log.e(TAG, "Geocoder IOException: " + e.getMessage());
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid latitude or longitude value.");
            e.printStackTrace();
        }

        return address;
    }

    /**
     * Show the last tracking path, including start and end markers
     */
    private void showLastTrack() {
        // Calculate distance
        float distanceInKm;
        if (isMapMode) {
            distanceInKm = totalDistance / 1000.0f;
        } else {
            // Non-map mode, calculate distance based on steps
            float averageStepLength = 0.75f;
            float distance = currentStepCount * averageStepLength; // in meters
            distanceInKm = distance / 1000.0f;
        }

        String timeElapsed = timerTextView.getText().toString();
        int stepCount = currentStepCount;

        String avg = avgPaceTextView.getText().toString();
        // Get address of the last location
        String address = "Unknown location";
        if (isMapMode && initialLatitude != 0.0 && initialLongitude != 0.0) {
            LatLng initialLatLng = new LatLng(initialLatitude, initialLongitude);
            address = getAddressFromLatLng(initialLatLng);
        }
        // Create an intent to jump to RunSummaryActivity
        Intent intent = new Intent(GoogleMapActivity.this, RunSummaryActivity.class);
        intent.putExtra("distance", distanceInKm);
        intent.putExtra("avgPace", avg);
        intent.putExtra("time", timeElapsed);
        intent.putExtra("address", address);
        intent.putExtra("stepCount", stepCount);
        intent.putExtra("calories", cTextView.getText().toString());
        intent.putExtra("MODE", isMapMode ? "MAP" : "NO_MAP");

        if (isMapMode) {
            // Collect trajectory points
            ArrayList<LatLng> trajectoryList = new ArrayList<>(trajectory);
            intent.putParcelableArrayListExtra("trajectory", trajectoryList);
        }

        startActivity(intent);
        finish();
    }

    /**
     * Navigate back to MainActivity
     */
    private void navigateToWorkoutPage() {
        Intent returnIntent = new Intent();
        setResult(Activity.RESULT_OK, returnIntent);
        finish();
    }

    /**
     * Called when the activity resumes
     */
    @Override
    protected void onResume() {
        super.onResume();

        // Handle permission changes
        handlePermissionChanges();

        // Register BroadcastReceiver
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.example.pulsestepapplication.LOCATION_UPDATE");
        filter.addAction("com.example.pulsestepapplication.STEP_UPDATE");
        LocalBroadcastManager.getInstance(this).registerReceiver(trackingReceiver, filter);

        if (isTracking && !isPaused) {
            timerHandler.postDelayed(timerRunnable, 0);
            btnShow.setVisibility(View.GONE);
        } else {
            btnShow.setVisibility(View.VISIBLE);
        }

        // Check if the user has granted background location permission from the settings page
        if (isMapMode && hasDeniedBackgroundPermission) {
            // Re-check background location permission
            if (isBackgroundLocationPermissionGranted()) {
                hasDeniedBackgroundPermission = false;
                sharedPreferences.edit().putBoolean(KEY_HAS_DENIED_BACKGROUND_PERMISSION, false).apply();

                // Resume tracking
                resumeTracking();
            }
        }
    }

    /**
     * Called when the activity is paused
     */
    @Override
    protected void onPause() {
        super.onPause();
        // Unregister BroadcastReceiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(trackingReceiver);
    }

    /**
     * Called when the activity is stopped
     */
    @Override
    protected void onStop() {
        super.onStop();
        // Remove timer callbacks
        if (isTracking) {
            timerHandler.removeCallbacks(timerRunnable);
        }

        // Check if the app has entered the background
        if (isTracking && isMapMode && !isBackgroundLocationPermissionGranted()) {
            pauseTracking();
            Toast.makeText(this, "Tracking paused due to lack of background location permission.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Check if background location permission is granted
     *
     * @return True if granted, otherwise false
     */
    private boolean isBackgroundLocationPermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
        // No background location permission needed for Android Q and below
        return true;
    }

    /**
     * Handle permission request results
     *
     * @param requestCode  Request code
     * @param permissions  Requested permissions
     * @param grantResults Permission results
     */
    @SuppressLint("NewApi")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == BACKGROUND_LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Background location permission granted
                hasDeniedBackgroundPermission = false;
                sharedPreferences.edit().putBoolean(KEY_HAS_DENIED_BACKGROUND_PERMISSION, false).apply();
                //resumeTracking();
            } else {
                // Background location permission denied
                hasDeniedBackgroundPermission = true;
                sharedPreferences.edit().putBoolean(KEY_HAS_DENIED_BACKGROUND_PERMISSION, true).apply();
                //Toast.makeText(this, "Background location permission denied, the app will stop tracking in the background.", Toast.LENGTH_LONG).show();
                // Show guidance dialog, guiding the user to manually grant permission in settings
                //showPermissionDeniedDialog();

            }
        } else if (requestCode == ACTIVITY_RECOGNITION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Activity recognition permission granted
                checkPermissions(); // Check other permissions
            } else {
                // Permission denied, navigate back to Workout page
                Toast.makeText(this, "Activity recognition permission is required", Toast.LENGTH_SHORT).show();
                navigateToWorkoutPage();
            }
        } else if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Location permission granted
                checkPermissions(); // Check other permissions
            } else {
                // Permission denied, navigate back to Workout page
                Toast.makeText(this, "Map mode requires location permission", Toast.LENGTH_SHORT).show();
                navigateToWorkoutPage();
            }
        }
    }

    /**
     * BroadcastReceiver to receive location and step updates from the service
     */
    private BroadcastReceiver trackingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.example.pulsestepapplication.LOCATION_UPDATE".equals(intent.getAction())) {
                double lat = intent.getDoubleExtra("lat", 0.0);
                double lng = intent.getDoubleExtra("lng", 0.0);
                // Update path on the map
                updatePath(new LatLng(lat, lng));
            } else if ("com.example.pulsestepapplication.STEP_UPDATE".equals(intent.getAction())) {
                int stepCount = intent.getIntExtra("stepCount", 0);
                Log.d(TAG, "Received step count update: " + stepCount);
                // Update step count display
                updateStepCount(stepCount);
            }
        }
    };

    /**
     * Update the step count on the UI
     *
     * @param stepCount Current step count
     */
    private void updateStepCount(int stepCount) {
        runOnUiThread(() -> {
            if (stepCount < 5) {
                stepTextView.setText("--");
            } else {
                stepTextView.setText(String.valueOf(stepCount));
            }
            currentStepCount = stepCount;
        });
    }

    /**
     * Called when the back button is pressed
     */
    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        popUpConfirmDialog();
    }

    /**
     * Called when the activity is destroyed, stopping the service and removing location updates
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTrackingService();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        timerHandler.removeCallbacks(timerRunnable);
        locationTimeoutHandler.removeCallbacks(locationTimeoutRunnable);
    }
}
