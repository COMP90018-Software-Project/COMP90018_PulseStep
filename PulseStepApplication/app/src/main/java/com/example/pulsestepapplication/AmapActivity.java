package com.example.pulsestepapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.MapsInitializer;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.CustomMapStyleOptions;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AmapActivity extends AppCompatActivity {

    // Constants
    private static final int LOCATION_REQUEST_CODE = 1001;
    private static final int ACTIVITY_RECOGNITION_REQUEST_CODE = 1002;
    private static final float MOVE_ZOOM_LEVEL = 16f;
    private static final float DEFAULT_ZOOM_LEVEL = 15f;
    private static final String TAG = "AmapActivity";
    private double userWeight;
    // UI Components
    private ImageButton btnPauseResume;
    private TextView timerTextView, stepTextView, avgPaceTextView;
    private ImageButton btnShow;
    private ImageView backButton;
    private ImageView mapImageView;

    // Map and Location
    private MapView mMapView;
    private AMap aMap;

    private double initialLatitude;
    private double initialLongitude;

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
    private static final Double realDistance = 0.01;
    private static final double metValue = 8.0;
    private static final double locationAccuracy = 50.0;

    // Step Counter
    private StepCounter stepCounter;

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

            // Update avgPace in No-map mode
            if (!isMapMode) {
                updateAvgPaceNoMapMode();
            }
            timerHandler.postDelayed(this, 1000);
        }
    };

    private TextView cTextView;
    private ImageView waitView;
    private TextView waitTextView;

    // Mode flag: true for Map mode, false for No-map mode
    private boolean isMapMode;
    private Geocoder geocoder;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupAmapPrivacy();
        setContentView(R.layout.activity_amap);

        // Get the mode from the intent
        Intent intent = getIntent();
        isMapMode = intent.getBooleanExtra("MAP_MODE", true); // default to Map mode
        userWeight = intent.getDoubleExtra("weight", 70.0);
        // Initialize UI components
        initializeUIComponents();

        // Check permissions
        checkPermissions();
    }

    /**
     * Checks and requests necessary permissions based on the mode.
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
     * Sets up the activity based on the mode.
     */
    private void setupActivity() {
        // Initialize step counter if supported
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            initStepCounter();
        }

        // Get initial latitude and longitude if available
        Intent intent = getIntent();
        initialLatitude = intent.getDoubleExtra("LATITUDE", 0.0);
        initialLongitude = intent.getDoubleExtra("LONGITUDE", 0.0);

        // Set up map or show default map
        if (isMapMode) {
            // Initialize map view and configure map
            initializeMapView();
            // Set up map listeners
            setupMapListeners();
        } else {
            // Show default map image
            showDefaultMap();
        }

        // Set up button listeners
        setupButtonListeners();
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
        waitView = findViewById(R.id.wait);
        waitTextView = findViewById(R.id.waitText);
        backButton = findViewById(R.id.back_button_running_page);
        mapImageView = findViewById(R.id.default_image_view);
        mMapView = findViewById(R.id.amap_view);
        mMapView.setVisibility(View.GONE);
        // Set click listener for the back button
        backButton.setOnClickListener(v -> navigateToMainActivity());

        if (isMapMode) {
            // Start animation if in Map mode
            Animation rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate);
            waitView.startAnimation(rotateAnimation);
            waitTextView.setVisibility(View.VISIBLE);
        } else {
            // Hide waiting animation in No-map mode
            waitView.setVisibility(View.GONE);
            waitTextView.setVisibility(View.GONE);
        }
    }

    /**
     * Navigates back to the MainActivity.
     */
    private void navigateToMainActivity() {
        Intent intent = new Intent(AmapActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * Initializes the StepCounter and sets up its listener.
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void initStepCounter() {
        stepCounter = new StepCounter(this);
        stepCounter.setStepCounterListener(stepCount -> {
            runOnUiThread(() -> {
                if (stepCount < 10) {
                    stepTextView.setText("--");
                } else {
                    stepTextView.setText(String.valueOf(stepCount));
                }
                currentStepCount = stepCount;
            });
        });
    }

    /**
     * Initializes the map view and configures the map.
     */
    private void initializeMapView() {
        mMapView.onCreate(null);
        aMap = mMapView.getMap();
        configureMap();
    }

    /**
     * Configures the map settings.
     */
    private void configureMap() {
        if (aMap == null) return;

        aMap.setMyLocationEnabled(true);
        // Set map type
        aMap.setMapType(AMap.MAP_TYPE_NAVI);
        aMap.setMaxZoomLevel(19.0f);

        // Hide some map features
        aMap.showBuildings(false);
        aMap.showMapText(false);

        // Apply custom map style
        applyCustomMapStyle();

        // Set initial camera position
        setInitialCameraPosition();
    }

    /**
     * Sets the initial camera position.
     */
    private void setInitialCameraPosition() {
        if (initialLatitude != 0.0 && initialLongitude != 0.0) {
            LatLng initialLatLng = new LatLng(initialLatitude, initialLongitude);
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(initialLatLng)
                    .zoom(DEFAULT_ZOOM_LEVEL)
                    .tilt(0)
                    .build();
            aMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        } else {
            // Get current location and set camera position
            Location myLocation = aMap.getMyLocation();
            if (myLocation != null) {
                LatLng currentLatLng = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
                aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, DEFAULT_ZOOM_LEVEL));
            }
        }
    }

    /**
     * Applies custom map style.
     */
    private void applyCustomMapStyle() {
        try {
            // Create CustomMapStyleOptions object
            CustomMapStyleOptions customMapStyleOptions = new CustomMapStyleOptions();

            // Set style data file path (located in assets directory)
            customMapStyleOptions.setStyleDataPath(getAssetsPath("style/style.data"));

            // If there are extra texture files, set the texture file path
            customMapStyleOptions.setStyleExtraPath(getAssetsPath("style/style_extra.data"));

            // Apply custom style options to the map
            aMap.setCustomMapStyle(customMapStyleOptions);

            // Enable custom map style
            aMap.setMapCustomEnable(true);

            Log.d(TAG, "Custom map style applied successfully.");
        } catch (Exception e) {
            Log.e(TAG, "Failed to apply custom map style", e);
        }
    }

    /**
     * Gets the full path of a file in the assets directory.
     */
    private String getAssetsPath(String fileName) {
        return "file:///android_asset/" + fileName;
    }

    /**
     * Sets up the map listeners.
     */
    private void setupMapListeners() {
        // Set location change listener
        aMap.setOnMyLocationChangeListener(location -> {
            if (location != null && location.hasAccuracy() && location.getAccuracy() < locationAccuracy) {
                isLocationReady = true;
                waitView.clearAnimation();
                waitView.setVisibility(View.GONE);
                waitTextView.setVisibility(View.GONE);
                mMapView.setVisibility(View.VISIBLE);
                LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                if (isTracking && !isPaused && isLocationReady) {
                    updatePath(currentLatLng);
                    CameraPosition cameraPosition = new CameraPosition.Builder()
                            .target(currentLatLng)
                            .zoom(MOVE_ZOOM_LEVEL)
                            .tilt(0)
                            .bearing(0)
                            .build();
                    aMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 200, null);
                }
            } else {
                // If location is not ready, keep trying
                if (!isLocationReady) {
                    Log.d(TAG, "Location not ready, keep trying...");
                    aMap.setMyLocationEnabled(true);
                }
            }
        });
    }

    /**
     * Sets up the button listeners for pause/resume and show actions.
     */
    private void setupButtonListeners() {
        btnPauseResume.setOnClickListener(v -> handlePauseResumeButtonClick());
        btnShow.setOnClickListener(v -> showLastTrack());
    }

    /**
     * Handles the pause/resume button click event.
     */
    private void handlePauseResumeButtonClick() {
        if (!isMapMode) {
            // No-map mode
            if (isFirstStart) {
                startTracking();
                isFirstStart = false;
            } else if (isPaused) {
                resumeTracking();
            } else {
                pauseTracking();
            }
        } else {
            // Map mode
            if (!isLocationReady) {
                // If location is not ready, show a toast message
                Toast.makeText(this, "定位中...", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isFirstStart) {
                startTracking();
                isFirstStart = false;
            } else if (isPaused) {
                resumeTracking();
            } else {
                pauseTracking();
            }
        }
    }

    /**
     * Starts the tracking process, including location updates and step tracking.
     */
    @SuppressLint({"MissingPermission", "UseCompatLoadingForDrawables"})
    private void startTracking() {
        if (isMapMode) {
            if (aMap != null && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                aMap.setMyLocationEnabled(true);
            }
        }

        isTracking = true;
        isPaused = false;
        pathPoints.clear();
        totalDistance = 0.0f;
        startTime = SystemClock.elapsedRealtime();
        timerHandler.postDelayed(timerRunnable, 0);
        btnPauseResume.setImageDrawable(getResources().getDrawable(R.drawable.pause));
        btnShow.setVisibility(View.GONE);

        // Start step tracking
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            stepCounter.startStepTracking();
        }

        if (isMapMode) {
            // Focus on current location
            Location myLocation = aMap.getMyLocation();
            if (myLocation != null) {
                LatLng currentLatLng = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
                aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, MOVE_ZOOM_LEVEL));
            }
        }
    }

    /**
     * Resumes tracking after a pause.
     */
    @SuppressLint("UseCompatLoadingForDrawables")
    private void resumeTracking() {
        isPaused = false;
        startTime += (SystemClock.elapsedRealtime() - pauseTime);
        timerHandler.postDelayed(timerRunnable, 0);
        btnPauseResume.setImageDrawable(getResources().getDrawable(R.drawable.pause));
        btnShow.setVisibility(View.GONE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            stepCounter.startStepTracking();
        }
        pathPoints.clear();
    }

    /**
     * Pauses the tracking process.
     */
    @SuppressLint("UseCompatLoadingForDrawables")
    private void pauseTracking() {
        isPaused = true;
        pauseTime = SystemClock.elapsedRealtime();
        timerHandler.removeCallbacks(timerRunnable);
        btnPauseResume.setImageDrawable(getResources().getDrawable(R.drawable.start));
        btnShow.setVisibility(View.VISIBLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            stepCounter.stopStepTracking();
        }
        if (isMapMode) {
            drawCurrentPolyline();
        }
        trajectory.add(null);
    }

    /**
     * Updates the path with the new location and calculates the total distance.
     *
     * @param latLng The new location coordinates.
     */
    @SuppressLint("DefaultLocale")
    private void updatePath(LatLng latLng) {
        if (pathPoints.isEmpty()) {
            pathPoints.add(latLng);
            trajectory.add(latLng);
            return;
        }

        LatLng lastLatLng = pathPoints.get(pathPoints.size() - 1);
        float[] results = new float[1];
        Location.distanceBetween(lastLatLng.latitude, lastLatLng.longitude, latLng.latitude, latLng.longitude, results);

        if (results[0] > 1.0) {
            totalDistance += results[0];
            double totalDistanceKm = totalDistance / 1000.0;
            double totalTimeMinutes = elapsedTime / (1000.0 * 60.0);

            if (totalDistanceKm > realDistance && totalTimeMinutes > 0) {
                double avgPace = totalTimeMinutes / totalDistanceKm;
                double elapsedTimeInMinutes = elapsedTime / 60000.0;
                double caloriesBurned = calculateCalories(userWeight, elapsedTimeInMinutes, metValue);
                cTextView.setText(String.format("%d", Math.round(caloriesBurned)));
                if (avgPace >= 1.0 && avgPace <= 30.0) {
                    avgPaceTextView.setText(String.format("%d'%02d\"", (int) avgPace, (int) ((avgPace * 60) % 60)));
                } else {
                    avgPaceTextView.setText("--'--\"");
                }
            } else {
                avgPaceTextView.setText("--'--\"");
            }

            pathPoints.add(latLng);
            trajectory.add(latLng);
            drawCurrentPolyline();
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
            cTextView.setText(String.format("%d", Math.round(caloriesBurned)));
            // Update avgPaceTextView
            runOnUiThread(() -> {
                avgPaceTextView.setText(String.format("%d'%02d\"", (int) avgPace, (int) ((avgPace * 60) % 60)));
            });
        } else {
            runOnUiThread(() -> {
                avgPaceTextView.setText("--'--\"");
            });
        }
    }
    private double calculateCalories(double weight, double durationInMinutes, double metValue) {

        double durationInHours = durationInMinutes / 60.0;
        return metValue * weight * durationInHours;
    }
    /**
     * Draws the current polyline on the map.
     */
    private void drawCurrentPolyline() {
        if (!pathPoints.isEmpty()) {
            PolylineOptions polylineOptions = new PolylineOptions()
                    .addAll(pathPoints)
                    .color(getResources().getColor(R.color.like_orange))
                    .width(20);
            if (polyLines.isEmpty() || isPaused) {
                Polyline polyline = aMap.addPolyline(polylineOptions);
                polyLines.add(polyline);
            } else {
                polyLines.get(polyLines.size() - 1).remove();
                Polyline polyline = aMap.addPolyline(polylineOptions);
                polyLines.set(polyLines.size() - 1, polyline);
            }
        }
    }
    /**
     * Converts a LatLng point to a human-readable address string.
     *
     * @param latLng The LatLng object representing the location.
     * @return A string containing the country and city, or "Unknown Location" if not available.
     */
    private String getAddressFromLatLng(com.google.android.gms.maps.model.LatLng latLng) {
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
     * Shows the last tracked path and navigates to the summary page.
     */
    private void showLastTrack() {
        float distanceInKm;
        if (isMapMode) {
            distanceInKm = totalDistance / 1000;
        } else {
            // In No-map mode, calculate distance based on steps
            float averageStepLength = 0.75f;
            float distance = currentStepCount * averageStepLength; // in meters
            distanceInKm = distance / 1000.0f;
        }

        String timeElapsed = timerTextView.getText().toString();
        int stepCount = currentStepCount;

        // Get the last location's address
        String address = "Unknown Location";
        if (isMapMode && initialLatitude != 0.0 && initialLongitude != 0.0) {
            com.google.android.gms.maps.model.LatLng initialLatLng = new com.google.android.gms.maps.model.LatLng(initialLatitude, initialLongitude);
            address = getAddressFromLatLng(initialLatLng);
        }
        String avg = avgPaceTextView.getText().toString();

        // Create Intent to Summary Activity
        Intent intent = new Intent(AmapActivity.this, AmapRunSummaryActivity.class);
        intent.putExtra("distance", distanceInKm);
        intent.putExtra("avgPace", avg);
        intent.putExtra("time", timeElapsed);
        intent.putExtra("address", address);
        intent.putExtra("stepCount", stepCount);
        intent.putExtra("MODE", isMapMode ? "MAP" : "NO_MAP");
        intent.putExtra("calories", cTextView.getText().toString());

        if (isMapMode) {
            // Collect trajectory points
            ArrayList<LatLng> trajectoryList = new ArrayList<>(trajectory);
            intent.putParcelableArrayListExtra("trajectory", trajectoryList);
        }

        startActivity(intent);
        finish();
    }

    /**
     * Shows a default map image when location permission is not granted or in No-map mode.
     */
    private void showDefaultMap() {
        // Display default image
        Bitmap defaultBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bg_workout);
        if (defaultBitmap != null) {
            mapImageView.setImageBitmap(defaultBitmap);
            mapImageView.setVisibility(View.VISIBLE);
        }
        // Hide map view
        if (mMapView != null) {
            mMapView.setVisibility(View.GONE);
        }
        // Ensure step count and timer views are visible
        stepTextView.setVisibility(View.VISIBLE);
        timerTextView.setVisibility(View.VISIBLE);
    }

    /**
     * Handles permission changes when the activity resumes.
     */
    private void handlePermissionChanges() {
        if (isMapMode) {
            // Check if location permission has been revoked
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // Permission revoked, switch to No-map mode or handle accordingly
                Toast.makeText(this, "定位权限已被禁用，切换到无地图模式", Toast.LENGTH_SHORT).show();
                isMapMode = false;
                if (aMap != null) {
                    aMap.clear();
                }
                showDefaultMap();
            } else {
                // Permission granted, re-initialize map if needed
                if (aMap == null) {
                    initializeMapView();
                    setupMapListeners();
                }
                aMap.setMyLocationEnabled(true);
                isLocationReady = false;
                waitView.setVisibility(View.VISIBLE);
                waitTextView.setVisibility(View.VISIBLE);
                // Restart location updates
                setupMapListeners();
            }
        }
        // Check for activity recognition permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                // Permission revoked, navigate back to Workout page
                Toast.makeText(this, "计步器权限已被禁用", Toast.LENGTH_SHORT).show();
                navigateToWorkoutPage();
            }
        }
    }

    /**
     * Navigates back to the WorkoutActivity.
     */
    private void navigateToWorkoutPage() {
        Intent intent = new Intent(AmapActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * Sets up Amap privacy settings.
     */
    private void setupAmapPrivacy() {
        MapsInitializer.updatePrivacyShow(this, true, true);
        MapsInitializer.updatePrivacyAgree(this, true);
    }

    /**
     * Saves the instance state to handle configuration changes.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mMapView != null) {
            mMapView.onSaveInstanceState(outState);
        }
        outState.putBoolean("isTracking", isTracking);
        outState.putBoolean("isPaused", isPaused);
        outState.putLong("startTime", startTime);
        outState.putLong("pauseTime", pauseTime);
        outState.putBoolean("isMapMode", isMapMode);
    }

    /**
     * Restores the instance state after configuration changes.
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (mMapView != null) {
            mMapView.onCreate(savedInstanceState);
        }
        isTracking = savedInstanceState.getBoolean("isTracking", false);
        isPaused = savedInstanceState.getBoolean("isPaused", false);
        startTime = savedInstanceState.getLong("startTime", 0L);
        pauseTime = savedInstanceState.getLong("pauseTime", 0L);
        isMapMode = savedInstanceState.getBoolean("isMapMode", true);

        if (isTracking && !isPaused) {
            startTracking();
        }
    }

    /**
     * Restores the step counter listener when the activity starts.
     */
    @Override
    protected void onStart() {
        super.onStart();
        if (isTracking && !isPaused && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && stepCounter != null) {
            stepCounter.registerListener();
        }
    }

    /**
     * Handles tracking state and permission changes when the activity resumes.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (mMapView != null) {
            mMapView.onResume();
        }

        // Handle permission changes
        handlePermissionChanges();

        if (isTracking && !isPaused) {
            startTracking();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && stepCounter != null) {
                stepCounter.registerListener();
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && stepCounter != null) {
                stepCounter.unregisterListener();
            }
        }

        btnShow.setVisibility(View.VISIBLE);
    }

    /**
     * Removes location updates and unregisters the step counter when the activity stops.
     */
    @Override
    protected void onStop() {
        super.onStop();

        // Unregister step counter
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && stepCounter != null) {
            stepCounter.unregisterListener();
        }

        // Pause the timer if tracking
        if (isTracking) {
            pauseTracking();
        }
    }

    /**
     * Pauses the map view.
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (mMapView != null) {
            mMapView.onPause();
        }
    }

    /**
     * Destroys the map view and cleans up resources.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMapView != null) {
            mMapView.onDestroy();
        }
        timerHandler.removeCallbacks(timerRunnable);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && stepCounter != null) {
            stepCounter.unregisterListener();
        }
    }

    /**
     * Handles the result of permission requests.
     */
    @SuppressLint("NewApi")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ACTIVITY_RECOGNITION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onStepCounterPermissionGranted();
                checkPermissions(); // Check for other permissions
            } else {
                Toast.makeText(this, "计步器权限被拒绝", Toast.LENGTH_SHORT).show();
                navigateToWorkoutPage();
            }
        } else if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Location permission granted, initialize map
                checkPermissions(); // Check for other permissions
            } else {
                Toast.makeText(this, "定位权限被拒绝", Toast.LENGTH_SHORT).show();
                navigateToWorkoutPage();
            }
        }
    }

    /**
     * Handles actions after step counter permission is granted.
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void onStepCounterPermissionGranted() {
        if (stepCounter != null) {
            stepCounter.startStepTracking();
        }
    }

    /**
     * Handles the back button pressed event.
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
