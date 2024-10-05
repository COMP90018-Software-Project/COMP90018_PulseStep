package com.example.pulsestepapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
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

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;

import com.google.android.gms.maps.model.LatLng;

import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;

import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GoogleMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    // Constants
    private static final int LOCATION_REQUEST_CODE = 1001;
    private static final int ACTIVITY_RECOGNITION_REQUEST_CODE = 1002;
    private static final float MOVE_ZOOM_LEVEL = 17f;
    private static final float DEFAULT_ZOOM_LEVEL = 15f;
    private static final float MAX_ZOOM_LEVEL = 19f;
    private static final String TAG = "GoogleMapActivity";
    private String userName;
    private int userAge;
    private double userWeight;
    // UI Components
    private ImageButton btnPauseResume;
    private TextView timerTextView, stepTextView, avgPaceTextView;
    private ImageButton btnShow;
    private ImageView backButton;
    private ImageView mapImageView;

    // Map and Location
    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private double initialLatitude;
    private double initialLongitude;

    // Tracking Variables
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
    private static final int realStep = -1;
    private static final Double realDistance = 0.05;
    // Geocoder for address conversion
    private Geocoder geocoder;

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
    private static final int LOCATION_TIMEOUT = 10000;
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
                Toast.makeText(GoogleMapActivity.this, "Unable to get accurate location", Toast.LENGTH_LONG).show();
            }
        }
    };
    private Marker userLocationMarker;
    private TextView cTextView;
    private ImageView waitView;
    private TextView waitTextView;

    // Mode flag: true for Map mode, false for No-map mode
    private boolean isMapMode;

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
        // Initialize Geocoder
        if (Geocoder.isPresent()) {
            geocoder = new Geocoder(this, Locale.getDefault());
        } else {
            Log.e(TAG, "Geocoder not available on this device.");
        }

        // Initialize location services if in Map mode
        if (isMapMode) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        }

        // Initialize step counter if supported
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            initStepCounter();
        }

        // Get initial latitude and longitude if available
        Intent intent = getIntent();
        initialLatitude = intent.getDoubleExtra("LATITUDE", 0.0);
        initialLongitude = intent.getDoubleExtra("LONGITUDE", 0.0);

        // Set up map fragment or show default map
        if (isMapMode) {
            setupMapFragment();
            // Set up location update callback
            setupLocationCallback();
        } else {
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
        btnPauseResume.setOnClickListener(v -> handlePauseResumeButtonClick());
        btnShow.setOnClickListener(v -> showLastTrack());
    }

    /**
     * Configures the LocationCallback to handle location updates.
     */
    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {

            @SuppressLint("MissingPermission")
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                // Update location regardless of tracking state to determine when location is ready
                for (Location location : locationResult.getLocations()) {
                    if (location.hasAccuracy() && location.getAccuracy() < 50.0) {
                        isLocationReady = true;
                        waitView.clearAnimation();
                        waitView.setVisibility(View.GONE);
                        waitTextView.setVisibility(View.GONE);
                        // Enable the start button when location is ready
                        btnPauseResume.setEnabled(true);
                        btnPauseResume.setClickable(true);
                        locationTimeoutHandler.removeCallbacks(locationTimeoutRunnable);
                        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        googleMap.setMyLocationEnabled(true);
                    } else {
                        // If location is not ready, keep trying
                        if (!isLocationReady) {
                            Log.d(TAG, "Location not ready, keep trying...");

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
     * Updates the user's location marker on the map with a custom icon.
     *
     * @param latLng The current location coordinates.
     */
    private void updateUserLocationMarker(LatLng latLng) {
        // Decode and resize custom icon
        Bitmap iconBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.img);
        if (iconBitmap != null) {
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(iconBitmap, 150, 150, false);

            // Remove previous marker
            if (userLocationMarker != null) {
                userLocationMarker.remove();
            }

            // Add new marker
            userLocationMarker = googleMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .icon(BitmapDescriptorFactory.fromBitmap(resizedBitmap)));
        } else {
            Log.e(TAG, "Failed to decode custom_location_icon.");
        }
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
     * Called when the Google Map is ready. Configures map settings and retrieves the user's location.
     *
     * @param map The GoogleMap object that is ready to be used.
     */
    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        applyCustomMapStyle();
        googleMap.setMaxZoomPreference(MAX_ZOOM_LEVEL);
        // Set the initial camera position to the user's last known location
        if (initialLatitude != 0.0 && initialLongitude != 0.0) {
            LatLng initialLatLng = new LatLng(initialLatitude, initialLongitude);
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(initialLatLng)   // Set the center of the map
                    .zoom(DEFAULT_ZOOM_LEVEL) // Set the zoom level
                    .tilt(0)                // Set tilt to 0 to ensure a 2D view
                    .build();

            googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }

        // Check if location permissions are granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(false);
            googleMap.setBuildingsEnabled(false);
            onLocationPermissionGranted();
        }
    }

    /**
     * Handles actions after GPS location permission is granted.
     */
    @SuppressLint("MissingPermission")
    private void onLocationPermissionGranted() {
        if (googleMap != null) {
            googleMap.setMyLocationEnabled(false);
            googleMap.setBuildingsEnabled(false);

            // Start location updates to determine when location is ready
            requestLocationUpdates();
        }
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
                Toast.makeText(this, "Getting accurate positioning", Toast.LENGTH_SHORT).show();
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
            if (googleMap != null && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                googleMap.setMyLocationEnabled(true);
                googleMap.setBuildingsEnabled(false);
            }
            // Start location updates
            requestLocationUpdates();
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
            // Zoom to current location if available
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, MOVE_ZOOM_LEVEL));
                }
            });
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
     * Requests location updates with high accuracy.
     */
    private void requestLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(3000)
                .setMinUpdateIntervalMillis(1000)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
            locationTimeoutHandler.postDelayed(locationTimeoutRunnable, LOCATION_TIMEOUT);
        }
    }

    /**
     * Updates the path with the new location and calculates the total distance.
     *
     * @param latLng The new location coordinates.
     */
    @SuppressLint("DefaultLocale")
    private void updatePath(LatLng latLng) {
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
        if (results[0] > 1.0) {
            // Update total distance only if the user has moved more than 1 meter
            totalDistance += results[0];
            double totalDistanceKm = totalDistance / 1000.0;
            double totalTimeMinutes = elapsedTime / (1000.0 * 60.0);

            // Ensure that distance and time are both valid before calculating pace
            if (totalDistanceKm > 0 && totalTimeMinutes > 0) {
                double avgPace = totalTimeMinutes / totalDistanceKm;
                double elapsedTimeInMinutes = elapsedTime / 60000.0;
                double metValue = 8.0;
                double caloriesBurned = calculateCalories(userWeight, elapsedTimeInMinutes, metValue);
                cTextView.setText(String.format("%d kcal", Math.round(caloriesBurned)));
                // Check if the calculated pace is within a reasonable range
                if (avgPace >= 1.0 && avgPace <= 30.0) {
                    avgPaceTextView.setText(String.format("%d'%02d\"", (int) avgPace, (int) ((avgPace * 60) % 60)));
                } else {
                    avgPaceTextView.setText("--'--\"");
                }
            } else {
                avgPaceTextView.setText("--'--\"");
            }

            // Only add the current point to pathPoints and draw the line if it meets criteria
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
        if (distanceKm > 0 && totalTimeMinutes > 0) {
            double avgPace = totalTimeMinutes / distanceKm;
            double elapsedTimeInMinutes = elapsedTime / 60000.0;
            double metValue = 8.0; //
            double caloriesBurned = calculateCalories(userWeight, elapsedTimeInMinutes, metValue);
            cTextView.setText(String.format("%d kcal", Math.round(caloriesBurned)));
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

    /**
     * Draws the current polyline on the map.
     */
    private void drawCurrentPolyline() {
        if (!pathPoints.isEmpty()) {
            PolylineOptions polylineOptions = new PolylineOptions().addAll(pathPoints).color(getResources().getColor(R.color.like_orange)).width(30);
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
    private double calculateCalories(double weight, double durationInMinutes, double metValue) {

        double durationInHours = durationInMinutes / 60.0;
        return metValue * weight * durationInHours;
    }
    /**
     * Displays a default map image when location permission is not granted or in No-map mode.
     */
    private void showDefaultMap() {
        // Display default image
        Bitmap defaultBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bg_workout);
        if (defaultBitmap != null) {
            mapImageView.setImageBitmap(defaultBitmap);
            mapImageView.setVisibility(View.VISIBLE);
        }
        // Hide map fragment
        View mapFragment = findViewById(R.id.google_map);
        if (mapFragment != null) {
            mapFragment.setVisibility(View.GONE);
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
                isMapMode = false;
                if (googleMap != null) {
                    googleMap.clear();
                }
                showDefaultMap();
            } else {
                // Permission granted, re-initialize map if needed
                if (googleMap == null) {
                    setupMapFragment();
                }
                //googleMap.setMyLocationEnabled(true);
                isLocationReady = false;
                waitView.setVisibility(View.VISIBLE);
                waitTextView.setVisibility(View.VISIBLE);
                //btnPauseResume.setEnabled(false);
                // Restart location updates
                setupLocationCallback();
                requestLocationUpdates();
            }
        }
        // Check for activity recognition permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                // Permission revoked, navigate back to Workout page
                //Toast.makeText(this, "activity recognition permission is required", Toast.LENGTH_SHORT).show();
                navigateToWorkoutPage();
            }
        }
    }

    /**
     * Navigates back to the WorkoutActivity.
     */
    private void navigateToWorkoutPage() {
        Intent intent = new Intent(GoogleMapActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
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
    @Override
    protected void onPause() {
        super.onPause();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && stepCounter != null) {
            stepCounter.unregisterListener();
        }
    }
    /**
     * Handles tracking state and permission changes when the activity resumes.
     */
    @Override
    protected void onResume() {
        super.onResume();

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

        // Remove location updates
        if (isMapMode && fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }

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
     * Cleans up resources when the activity is destroyed.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Remove location updates to prevent memory leaks
        if (isMapMode && fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }

        // Remove timer callbacks to prevent memory leaks
        timerHandler.removeCallbacks(timerRunnable);

        // Clean up step counter
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && stepCounter != null) {
            stepCounter.unregisterListener();
        }
    }

    /**
     * Saves the instance state to handle configuration changes.
     *
     * @param outState The Bundle in which to place saved state.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("isTracking", isTracking);
        outState.putBoolean("isPaused", isPaused);
        outState.putLong("startTime", startTime);
        outState.putLong("pauseTime", pauseTime);
        outState.putBoolean("isMapMode", isMapMode);
    }

    /**
     * Restores the instance state after configuration changes.
     *
     * @param savedInstanceState The Bundle containing the saved state.
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
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
        if (requestCode == ACTIVITY_RECOGNITION_REQUEST_CODE) {
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
