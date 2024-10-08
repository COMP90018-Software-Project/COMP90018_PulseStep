package com.example.pulsestepapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
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
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import android.widget.ImageButton;
import android.widget.ImageView;
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

public class GoogleMapActivity extends AppCompatActivity implements OnMapReadyCallback {
    // Constants
    private static final String TAG = "GoogleMapActivity";
    private static final int LOCATION_REQUEST_CODE = 1001;
    private static final int ACTIVITY_RECOGNITION_REQUEST_CODE = 1002;
    private static final int BACKGROUND_LOCATION_REQUEST_CODE = 1003; // Unique Request Code
    private static final float MOVE_ZOOM_LEVEL = 17f;
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
    private final List<Polyline> polyLines = new ArrayList<>();
    private final List<LatLng> pathPoints = new ArrayList<>();
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

            // Update average pace in No-map mode
            if (!isMapMode) {
                updateAvgPaceNoMapMode();
            }
            timerHandler.postDelayed(this, 1000);
        }
    };

    // Location Timeout Handler
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
                Toast.makeText(GoogleMapActivity.this, "Location timeout, signal is weak!", Toast.LENGTH_LONG).show();
            }
        }
    };

    // Mode flag: true for Map mode, false for No-map mode
    private boolean isMapMode;
    private boolean isServiceRunning = false;

    private String userName;
    private int userAge;
    private double userWeight;
    private Geocoder geocoder;

    // Flags to track permission states
    private boolean hasRequestedBackgroundPermission = false;
    private boolean hasDeniedBackgroundPermission = false;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_map);

        // Get the mode from the intent
        Intent intent = getIntent();
        isMapMode = intent.getBooleanExtra("MAP_MODE", true); // default to Map mode
        userName = intent.getStringExtra("name");
        userAge = intent.getIntExtra("age", 25);
        userWeight = intent.getDoubleExtra("weight", 70.0);

        // Initialize UI components
        initializeUIComponents();

        // Check permissions
        checkPermissions();
    }

    /**
     * Initializes the UI components by finding them via their IDs.
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
        // Hide the map initially
        View mapFragment = findViewById(R.id.google_map);
        if (mapFragment != null) {
            mapFragment.setVisibility(View.GONE);  // Hide the map at first
        }
        // Set click listener for the back button
        backButton.setOnClickListener(v -> navigateToMainActivity());

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
            // Show the map once location is ready

            if (mapFragment != null) {
                mapFragment.setVisibility(View.VISIBLE);  // Show the map when location is ready
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
     * Navigates back to the MainActivity.
     */
    private void navigateToMainActivity() {
        Intent intent = new Intent(GoogleMapActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * Checks and requests necessary permissions.
     */
    private void checkPermissions() {
        // Check for activity recognition permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                // Request activity recognition permission
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, ACTIVITY_RECOGNITION_REQUEST_CODE);
                return;
            }
        }

        if (isMapMode) {
            // Check for location permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // Request location permission
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
                return;
            }
        }

        // Permissions are granted, proceed with setup
        setupActivity();
    }

    /**
     * Sets up the activity based on the selected mode.
     */
    private void setupActivity() {
        // Initialize location services if in Map mode
        if (isMapMode) {
            // Set up map fragment
            setupMapFragment();
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            setupLocationCallback();
        } else {
            showDefaultMap();
        }

        // Set up button listeners
        setupButtonListeners();

        // Start the tracking service
        startTrackingService();
    }

    /**
     * Sets up the SupportMapFragment and initializes the map asynchronously.
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
     * Sets up the button listeners for pause/resume and show actions.
     */
    private void setupButtonListeners() {
        btnPauseResume.setOnClickListener(v -> handleStartStopButtonClick());
        btnShow.setOnClickListener(v -> showLastTrack());
    }

    /**
     * Handles the start/stop button click event.
     */
    private void handleStartStopButtonClick() {
        if (isTracking) {
            pauseTracking();
        } else {
            if (isMapMode && !isLocationReady) {
                Toast.makeText(this, "Obtaining location, please wait...", Toast.LENGTH_SHORT).show();
                return;
            }
            if (isMapMode) {
                checkAndRequestBackgroundLocationPermission();
            } else {
                resumeTracking();
            }
        }
    }

    /**
     * Checks and requests background location permission if needed.
     */
    private void checkAndRequestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Check if background location permission is already granted
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // Check if we've already requested this permission
                if (!hasRequestedBackgroundPermission) {
                    hasRequestedBackgroundPermission = true;
                    // Show rationale if needed
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                        showBackgroundPermissionRationale();
                    } else {
                        // Directly request the permission
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, BACKGROUND_LOCATION_REQUEST_CODE);
                    }
                } else {
                    // Permission has been denied previously
                    if (hasDeniedBackgroundPermission) {
                        // Inform the user and guide them to settings
                        showPermissionDeniedDialog();
                    }
                }
            } else {
                // Permission already granted
                resumeTracking();
            }
        } else {
            // Background location permission is not required below Android Q
            resumeTracking();
        }
    }

    /**
     * Shows a rationale dialog for background location permission.
     */
    private void showBackgroundPermissionRationale() {
        new AlertDialog.Builder(this)
                .setTitle("Background Location Permission")
                .setMessage("This app requires background location access to track your activities even when the app is not in use.")
                .setPositiveButton("Allow", (dialog, which) -> {
                    // Request the permission
                    ActivityCompat.requestPermissions(GoogleMapActivity.this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, BACKGROUND_LOCATION_REQUEST_CODE);
                })
                .setNegativeButton("Deny", (dialog, which) -> {
                    // User declined, set flag
                    hasDeniedBackgroundPermission = true;
                    Toast.makeText(GoogleMapActivity.this, "Background location permission denied. Tracking will pause when the app is not in use.", Toast.LENGTH_LONG).show();
                })
                .create()
                .show();
    }

    /**
     * Shows a dialog directing the user to app settings to enable background location.
     */
    private void showPermissionDeniedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Background Location Permission Denied")
                .setMessage("To enable background tracking, please allow background location access in the app settings.")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    // Open app settings
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", getPackageName(), null));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    Toast.makeText(GoogleMapActivity.this, "Background location permission denied. Tracking will pause when the app is not in use.", Toast.LENGTH_LONG).show();
                })
                .create()
                .show();
    }

    /**
     * Starts the tracking process.
     */
    @SuppressLint({"MissingPermission", "UseCompatLoadingForDrawables"})
    private void resumeTracking() {
        isTracking = true;
        isPaused = false;
        if (isFirstStart) {
            pathPoints.clear();
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
            // Adjust startTime to account for pause duration
            long pauseDuration = SystemClock.elapsedRealtime() - pauseTime;
            startTime += pauseDuration;
            timerHandler.postDelayed(timerRunnable, 0);
        }
        btnPauseResume.setImageDrawable(getResources().getDrawable(R.drawable.pause));
        btnShow.setVisibility(View.GONE);

        // Send broadcast to service to resume step counting
        Intent resumeIntent = new Intent(LocationTrackingService.ACTION_RESUME_STEP_COUNTING);
        LocalBroadcastManager.getInstance(this).sendBroadcast(resumeIntent);
    }

    /**
     * Pauses the tracking process.
     */
    private void pauseTracking() {
        isTracking = false;
        isPaused = true;
        pauseTime = SystemClock.elapsedRealtime();
        timerHandler.removeCallbacks(timerRunnable);
        btnPauseResume.setImageDrawable(getResources().getDrawable(R.drawable.start));
        btnShow.setVisibility(View.VISIBLE);
        if (isMapMode) {
            drawCurrentPolyline();
        }
        trajectory.add(null);

        // Send broadcast to service to pause step counting
        Intent pauseIntent = new Intent(LocationTrackingService.ACTION_PAUSE_STEP_COUNTING);
        LocalBroadcastManager.getInstance(this).sendBroadcast(pauseIntent);
    }

    /**
     * Starts the tracking service.
     */
    private void startTrackingService() {
        Intent serviceIntent = new Intent(this, LocationTrackingService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
        isServiceRunning = true;
    }

    /**
     * Stops the tracking service.
     */
    private void stopTrackingService() {
        Intent serviceIntent = new Intent(this, LocationTrackingService.class);
        stopService(serviceIntent);
        isServiceRunning = false;
    }

    /**
     * Displays a default map image when location permission is not granted or in No-map mode.
     */
    private void showDefaultMap() {
        // Display default image
        mapImageView.setImageResource(R.drawable.bg_workout);
        mapImageView.setVisibility(View.VISIBLE);
        // Hide map fragment
        View mapFragment = findViewById(R.id.google_map);
        if (mapFragment != null) {
            mapFragment.setVisibility(View.GONE);
        }
    }

    /**
     * Called when the Google Map is ready. Configures map settings.
     *
     * @param map The GoogleMap object that is ready to be used.
     */
    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        applyCustomMapStyle();
        googleMap.setMaxZoomPreference(MAX_ZOOM_LEVEL);
        // Set the initial camera position to the user's last known location
        Intent intent = getIntent();
        initialLatitude = intent.getDoubleExtra("LATITUDE", 0.0);
        initialLongitude = intent.getDoubleExtra("LONGITUDE", 0.0);
        if (initialLatitude != 0.0 && initialLongitude != 0.0) {
            LatLng initialLatLng = new LatLng(initialLatitude, initialLongitude);
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(initialLatLng)   // Set the center of the map
                    .zoom(DEFAULT_ZOOM_LEVEL) // Set the zoom level
                    .tilt(0)                // Set tilt to 0 to ensure a 2D view
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
     * Applies a custom style to the Google Map from a raw resource file.
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
     * Sets up the location callback.
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
                        // Show the map once location is ready
                        View mapFragment = findViewById(R.id.google_map);
                        if (mapFragment != null) {
                            mapFragment.setVisibility(View.VISIBLE);  // Show the map when location is ready
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
                            Log.d(TAG, "Location accuracy insufficient, continuing attempts...");
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
     * Starts requesting location updates.
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
     * Requests location updates.
     */
    @SuppressLint("MissingPermission")
    private void requestLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(5000)
                .setMinUpdateIntervalMillis(2000)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setMinUpdateDistanceMeters(2)
                .build();

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    /**
     * Updates the path with the new location and calculates the total distance.
     *
     * @param latLng The new location coordinates.
     */
    @SuppressLint("DefaultLocale")
    private void updatePath(LatLng latLng) {
        if (!isTracking || isPaused) {
            return;
        }

        // If pathPoints is empty, we are processing the first location point
        if (pathPoints.isEmpty()) {
            pathPoints.add(latLng); // Add the first point directly but don't draw yet
            trajectory.add(latLng);
            return; // Skip drawing to wait for the next point
        }

        // Calculate the distance between the current point and the last added point
        LatLng lastLatLng = pathPoints.get(pathPoints.size() - 1);
        float[] results = new float[1];
        Location.distanceBetween(lastLatLng.latitude, lastLatLng.longitude, latLng.latitude, latLng.longitude, results);

        // Check if the distance between locations is significant (> 1 meter)
        if (results[0] > DISTANCE_THRESHOLD_METERS) {
            // Update total distance only if the user has moved more than the threshold
            totalDistance += results[0];
            double totalDistanceKm = totalDistance / 1000.0;
            double totalTimeMinutes = elapsedTime / (1000.0 * 60.0);

            // Ensure that distance and time are both valid before calculating pace
            if (totalDistanceKm > realDistance && totalTimeMinutes > 0) {
                double avgPace = totalTimeMinutes / totalDistanceKm;
                double elapsedTimeInMinutes = elapsedTime / 60000.0;
                double caloriesBurned = calculateCalories(userWeight, elapsedTimeInMinutes, metValue);
                runOnUiThread(() -> cTextView.setText(String.format("%d", Math.round(caloriesBurned))));
                // Check if the calculated pace is within a reasonable range
                if (avgPace >= 1.0 && avgPace <= 30.0) {
                    runOnUiThread(() -> avgPaceTextView.setText(String.format("%d'%02d\"", (int) avgPace, (int) ((avgPace * 60) % 60))));
                } else {
                    runOnUiThread(() -> avgPaceTextView.setText("--'--\""));
                }
            } else {
                runOnUiThread(() -> avgPaceTextView.setText("--'--\""));
            }

            // Only add the current point to pathPoints and draw the line if it meets criteria
            pathPoints.add(latLng);
            trajectory.add(latLng);
            drawCurrentPolyline();
        }
    }

    /**
     * Draws the current polyline on the map.
     */
    private void drawCurrentPolyline() {
        if (!pathPoints.isEmpty() && googleMap != null) {
            PolylineOptions polylineOptions = new PolylineOptions().addAll(pathPoints).color(getResources().getColor(R.color.like_orange)).width(10);
            if (polyLines.isEmpty() || isPaused) {
                Polyline polyline = googleMap.addPolyline(polylineOptions);
                polyLines.add(polyline);
            } else {
                polyLines.get(polyLines.size() - 1).remove();
                Polyline polyline = googleMap.addPolyline(polylineOptions);
                polyLines.set(polyLines.size() - 1, polyline);
            }
        }
    }

    /**
     * Updates the average pace in No-map mode based on steps and time.
     */
    @SuppressLint("DefaultLocale")
    private void updateAvgPaceNoMapMode() {
        // Assume average step length in meters
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
     * Calculates calories burned based on weight, duration, and MET value.
     *
     * @param weight            User's weight in kilograms.
     * @param durationInMinutes Duration of activity in minutes.
     * @param metValue          MET value of the activity.
     * @return Calories burned.
     */
    private double calculateCalories(double weight, double durationInMinutes, double metValue) {
        double durationInHours = durationInMinutes / 60.0;
        return metValue * weight * durationInHours;
    }

    /**
     * Converts a LatLng point to a human-readable address string.
     *
     * @param latLng The LatLng object representing the location.
     * @return A string containing the country and city, or "Unknown Location" if not available.
     */
    private String getAddressFromLatLng(LatLng latLng) {
        String address = "Unknown Location";

        // Ensure Geocoder is initialized
        if (geocoder == null) {
            if (Geocoder.isPresent()) {
                geocoder = new Geocoder(this, Locale.getDefault());
            } else {
                Log.e(TAG, "Geocoder not available.");
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
                Log.e(TAG, "No address found for the location.");
            }
        } catch (IOException e) {
            Log.e(TAG, "Geocoder IOException: " + e.getMessage());
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid latitude or longitude values.");
            e.printStackTrace();
        }

        return address;
    }

    /**
     * Shows the last tracked path on the map with start and end markers.
     */
    private void showLastTrack() {
        // Calculate distance
        float distanceInKm;
        if (isMapMode) {
            distanceInKm = totalDistance / 1000.0f;
        } else {
            // In No-map mode, calculate distance based on steps
            float averageStepLength = 0.75f;
            float distance = currentStepCount * averageStepLength; // in meters
            distanceInKm = distance / 1000.0f;
        }

        String timeElapsed = timerTextView.getText().toString();
        int stepCount = currentStepCount;

        String avg = avgPaceTextView.getText().toString();
        // Get the last location's address
        String address = "Unknown Location";
        if (isMapMode && initialLatitude != 0.0 && initialLongitude != 0.0) {
            LatLng initialLatLng = new LatLng(initialLatitude, initialLongitude);
            address = getAddressFromLatLng(initialLatLng);
        }
        // Create Intent to RunSummaryActivity
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
     * Handles permission changes when the activity resumes.
     */
    private void handlePermissionChanges() {
        if (isMapMode) {
            // Check if location permission has been revoked
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // Permission revoked, switch to No-map mode or handle accordingly
                isMapMode = false;
                if (googleMap != null) {
                    googleMap.clear();
                }
                showDefaultMap();
            }
        }
        // Check for activity recognition permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                // Permission revoked, navigate back to MainActivity
                Toast.makeText(this, "Activity recognition permission is required", Toast.LENGTH_SHORT).show();
                navigateToWorkoutPage();
            }
        }
    }

    /**
     * Navigates back to the MainActivity.
     */
    private void navigateToWorkoutPage() {
        Intent intent = new Intent(GoogleMapActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * Handles tracking state and permission changes when the activity resumes.
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
    }

    /**
     * Unregisters the BroadcastReceiver when the activity is paused.
     */
    @Override
    protected void onPause() {
        super.onPause();
        // Unregister BroadcastReceiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(trackingReceiver);
    }

    /**
     * Removes timer callbacks when the activity is stopped.
     */
    @Override
    protected void onStop() {
        super.onStop();
        // Pause the timer if tracking
        if (isTracking) {
            timerHandler.removeCallbacks(timerRunnable);
        }

        // Check if the app is going to the background
        if (isTracking && isMapMode && !isBackgroundLocationPermissionGranted()) {
            pauseTracking();
            Toast.makeText(this, "Tracking paused because background location permission is not granted.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Checks if background location permission is granted.
     *
     * @return true if granted, false otherwise
     */
    private boolean isBackgroundLocationPermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
        // Below Android Q, background location permission is not required
        return true;
    }

    /**
     * Handles the result of permission requests.
     *
     * @param requestCode  The request code passed in requestPermissions().
     * @param permissions  The requested permissions.
     * @param grantResults The grant results for the corresponding permissions.
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
                resumeTracking();
            } else {
                // Permission denied
                hasDeniedBackgroundPermission = true;
                Toast.makeText(this, "Background location permission denied. Tracking will pause when the app is not in use.", Toast.LENGTH_LONG).show();
                // Optionally, you can show a dialog guiding the user to settings
                showPermissionDeniedDialog();
            }
        } else if (requestCode == ACTIVITY_RECOGNITION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Activity recognition permission granted
                checkPermissions(); // Check for other permissions
            } else {
                // Permission denied, exit to workout page
                Toast.makeText(this, "Activity recognition permission is required", Toast.LENGTH_SHORT).show();
                navigateToWorkoutPage();
            }
        } else if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Location permission granted
                checkPermissions(); // Check for other permissions
            } else {
                // Permission denied, exit to workout page
                Toast.makeText(this, "Location permission is required in Map mode", Toast.LENGTH_SHORT).show();
                navigateToWorkoutPage();
            }
        }
    }

    /**
     * BroadcastReceiver to receive location and step updates from the service.
     */
    private BroadcastReceiver trackingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.example.pulsestepapplication.LOCATION_UPDATE".equals(intent.getAction())) {
                double lat = intent.getDoubleExtra("lat", 0.0);
                double lng = intent.getDoubleExtra("lng", 0.0);
                // Update the path on the map
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
     * Updates the step count on the UI.
     *
     * @param stepCount The current step count.
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    /**
     * Stops the service when the activity is destroyed.
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
