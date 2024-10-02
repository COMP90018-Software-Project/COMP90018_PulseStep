package com.example.pulsestepapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
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
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

public class GoogleMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    // Constants
    private static final int LOCATION_REQUEST_CODE = 1001;
    private static final int ACTIVITY_RECOGNITION_REQUEST_CODE = 1002;
    private static final float MOVE_ZOOM_LEVEL = 17f;
    private static final float DEFAULT_ZOOM_LEVEL = 15f;

    // UI Components
    private ImageButton btnPauseResume;
    private TextView timerTextView, distanceTextView, stepTextView;
    private ImageButton btnShow;

    // Map and Location
    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    // Tracking Variables
    private final List<Polyline> polyLines = new ArrayList<>();
    private final List<LatLng> pathPoints = new ArrayList<>();
    private boolean isTracking = false;
    private boolean isPaused = false;
    private boolean isFirstStart = true;
    private boolean isLocationReady = false;
    private float totalDistance = 0.0f;
    private int currentStepCount = 0;
    private final int realStep = 1;
    // Step Counter
    private StepCounter stepCounter;

    // Timer Variables
    private long startTime = 0L;
    private long pauseTime = 0L;
    private final Handler timerHandler = new Handler();
    private final Runnable timerRunnable = new Runnable() {
        @SuppressLint("DefaultLocale")
        @Override
        public void run() {
            long millis = SystemClock.elapsedRealtime() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;
            timerTextView.setText(String.format("%02d:%02d", minutes, seconds));
            timerHandler.postDelayed(this, 1000);
        }
    };

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_map);

        // Initialize views
        btnPauseResume = findViewById(R.id.btn_stop);
        btnShow = findViewById(R.id.btn_show);
        timerTextView = findViewById(R.id.timer_text_view);
        stepTextView = findViewById(R.id.step_text_view);
        distanceTextView = findViewById(R.id.distance_text_view);
        // Find the back button by its ID
        ImageView backButton = findViewById(R.id.back_button_running_page);

        // Set click listener for the back button
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Finish the current activity and return to the RunSummaryActivity page
                Intent intent = new Intent(GoogleMapActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize step counter
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            initStepCounter();
        }

        // Set up map fragment
        setupMapFragment();

        // Set up button listeners
        setupButtonListeners();

        // Set up location update callback
        setupLocationCallback();
        // 获取权限状态
        Intent intent = getIntent();
        boolean locationGranted = intent.getBooleanExtra("LOCATION_GRANTED", false);
        boolean activityRecognitionGranted = intent.getBooleanExtra("ACTIVITY_RECOGNITION_GRANTED", false);

        if (locationGranted) {
            onLocationPermissionGranted();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !activityRecognitionGranted) {
                // 仅在定位权限被授予且活动识别权限未被授予时请求活动识别权限
                checkAndRequestStepCounterPermission();
            }
        } else {
            // 定位权限未被授予，不请求活动识别权限
            checkAndRequestStepCounterPermission();
            showDefaultMap();
        }
    }

    // Attempt to apply custom map style
    private void applyCustomMapStyle() {
        try {
            boolean success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style));
            if (!success) {
                Log.e("GoogleMapActivity", "Style parsing failed.");
            } else {
                Log.d("GoogleMapActivity", "Map style applied successfully.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e("GoogleMapActivity", "Can't find style. Error: ", e);
        }
    }

    // Called when the map is ready. If permissions are granted, enable location; otherwise, show default map
    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        // Remove previous marker if it exists
        if (userLocationMarker != null) {
            userLocationMarker.remove();
            googleMap.setMyLocationEnabled(false);

        }
        // Attempt to apply custom style
        applyCustomMapStyle();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // If no permission, display default map
            showDefaultMap();
        } else {
            onLocationPermissionGranted();
        }
    }

    // Initialize step counter
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void initStepCounter() {
        stepCounter = new StepCounter(this);
        stepCounter.setStepCounterListener(stepCount -> runOnUiThread(() -> {
            try {
                stepTextView.setText(String.valueOf(stepCount));
                currentStepCount = stepCount;
            } catch (Exception e) {
                Log.e("StepCounter", "Error updating step count", e);
            }
        }));
    }

    // Set up the map fragment
    private void setupMapFragment() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.google_map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Log.e("GoogleMapActivity", "Map fragment is null");
        }
    }

    // Set up button listeners
    private void setupButtonListeners() {
        btnPauseResume.setOnClickListener(v -> handlePauseResumeButtonClick());
        btnShow.setOnClickListener(v -> showLastTrack());
    }

    // Set up location update callback
    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (!isTracking || isPaused) {
                    return;
                }

                for (Location location : locationResult.getLocations()) {
                    // Remove previous marker if it exists
                    if (userLocationMarker != null) {
                        userLocationMarker.remove();
                        googleMap.setMyLocationEnabled(false);
                    }
                    if (location.hasAccuracy() && location.getAccuracy() < 50.0) {
                        isLocationReady = true;
                    }

                    if (isLocationReady && currentStepCount > realStep) {
                        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                        // Decode custom icon and resize
                        Bitmap iconBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.img);
                        if (iconBitmap != null) {
                            // Resize bitmap
                            int width = 80; // Set desired width
                            int height = 80; // Set desired height
                            Bitmap resizedBitmap = Bitmap.createScaledBitmap(iconBitmap, width, height, false);

                            // Remove previous marker
                            if (userLocationMarker != null) {
                                userLocationMarker.remove();
                            }

                            userLocationMarker = googleMap.addMarker(new MarkerOptions()
                                    .position(currentLatLng)
                                    .icon(BitmapDescriptorFactory.fromBitmap(resizedBitmap)));
                        } else {
                            Log.e("GoogleMapActivity", "Failed to decode custom_location_icon.");
                        }

                        updatePath(currentLatLng);
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, MOVE_ZOOM_LEVEL));
                    } else if (isLocationReady) {
                        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                        // Decode custom icon and resize
                        Bitmap iconBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.img);
                        if (iconBitmap != null) {
                            // Resize bitmap
                            int width = 80; // Set desired width
                            int height = 80; // Set desired height
                            Bitmap resizedBitmap = Bitmap.createScaledBitmap(iconBitmap, width, height, false);

                            // Remove previous marker
                            if (userLocationMarker != null) {
                                userLocationMarker.remove();
                            }

                            userLocationMarker = googleMap.addMarker(new MarkerOptions()
                                    .position(currentLatLng)
                                    .icon(BitmapDescriptorFactory.fromBitmap(resizedBitmap)));
                        } else {
                            Log.e("GoogleMapActivity", "Failed to decode custom_location_icon.");
                        }
                    }
                }
            }
        };
    }

    private Marker userLocationMarker;

    // Separate permission checking method
    // Check and request step counter permission
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void checkAndRequestStepCounterPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            // Request permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, ACTIVITY_RECOGNITION_REQUEST_CODE);
        } else {
            // Permission already granted
            onStepCounterPermissionGranted();
        }
    }

    // Handle actions after GPS permission is granted
    @SuppressLint("MissingPermission")
    private void onLocationPermissionGranted() {
        if (googleMap != null) {
            googleMap.setMyLocationEnabled(true);
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, DEFAULT_ZOOM_LEVEL));
                }
            });
        }
    }

    // Handle actions after step counter permission is granted
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void onStepCounterPermissionGranted() {
        if (stepCounter != null) {
            stepCounter.startStepTracking();
        }
    }

    // Handle pause/resume button click event
    @SuppressLint("SetTextI18n")
    private void handlePauseResumeButtonClick() {
        if (isFirstStart) {
            if (checkPermissionsForTracking()) {
                startTracking();
                isFirstStart = false;
            }
        } else if (isPaused) {
            resumeTracking();
        } else {
            pauseTracking();
        }
    }

    // Check all permissions required for tracking
    private boolean checkPermissionsForTracking() {
        boolean locationGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean stepCounterGranted = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            stepCounterGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED;
        }

        /*if (!locationGranted) {
            Toast.makeText(this, "Please grant location permission to enable map functionality", Toast.LENGTH_SHORT).show();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !stepCounterGranted) {
            Toast.makeText(this, "Please grant step counter permission to enable step tracking", Toast.LENGTH_SHORT).show();
        }

         */

        // Return whether step counter permission has been granted (Location permission affects only map functionality, not step tracking or timing)
        return stepCounterGranted;
    }

    // Start tracking
    @SuppressLint({"SetTextI18n", "MissingPermission"})
    private void startTracking() {
        if (googleMap!= null){
            googleMap.setMyLocationEnabled(false);
        }
        isTracking = true;
        isPaused = false;
        pathPoints.clear();
        totalDistance = 0.0f;
        distanceTextView.setText(String.format("%.2f km", totalDistance / 1000));
        startTime = SystemClock.elapsedRealtime();
        timerHandler.postDelayed(timerRunnable, 0);
        btnPauseResume.setImageDrawable(getResources().getDrawable(R.drawable.pause));
        btnShow.setVisibility(View.GONE);
        requestLocationUpdates(); // If GPS permission is granted, this enables location updates

        // Start step tracking
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            stepCounter.startStepTracking();
        }

        // Zoom to current location (if available)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, MOVE_ZOOM_LEVEL));
                }
            });
        }
    }

    // Resume tracking
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

    // Pause tracking
    private void pauseTracking() {
        isPaused = true;
        pauseTime = SystemClock.elapsedRealtime();
        timerHandler.removeCallbacks(timerRunnable);
        btnPauseResume.setImageDrawable(getResources().getDrawable(R.drawable.start));
        btnShow.setVisibility(View.VISIBLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            stepCounter.stopStepTracking();
        }
        drawCurrentPolyline();
    }

    // Request location updates
    private void requestLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(10000)
                .setMinUpdateIntervalMillis(2000)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    // Update path
    @SuppressLint("DefaultLocale")
    private void updatePath(LatLng latLng) {
        if (currentStepCount > realStep) {
            if (!pathPoints.isEmpty()) {
                LatLng lastLatLng = pathPoints.get(pathPoints.size() - 1);
                float[] results = new float[1];
                Location.distanceBetween(lastLatLng.latitude, lastLatLng.longitude, latLng.latitude, latLng.longitude, results);
                totalDistance += results[0];
                distanceTextView.setText(String.format("%.2f km", totalDistance / 1000));
            }
            pathPoints.add(latLng);
            drawCurrentPolyline();
        }
    }

    // Draw the current polyline
    private void drawCurrentPolyline() {
        if (!pathPoints.isEmpty()) {
            PolylineOptions polylineOptions = new PolylineOptions().addAll(pathPoints);
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

    // Show the last track
    private void showLastTrack() {
        if (polyLines.isEmpty()) {
            Toast.makeText(this, "Path too short", Toast.LENGTH_SHORT).show();
            return;
        }

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        // Record start and end points
        LatLng startPoint = null;
        LatLng endPoint = null;
        for (Polyline polyline : polyLines) {
            List<LatLng> points = polyline.getPoints();
            // Get start and end points
            if (points.size() > 0) {
                if (startPoint == null) {
                    startPoint = points.get(0); // Start point of the line
                }
                endPoint = points.get(points.size() - 1); // End point of the line
            }
            for (LatLng point : points) {
                builder.include(point);
            }
        }
        LatLngBounds bounds = builder.build();
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
        if (totalDistance > 0.02) { // If walking distance is greater than 20 meters
            // Add start marker
            if (startPoint != null) {
                googleMap.addMarker(new MarkerOptions()
                        .position(startPoint)
                        .title("Start Point") // Customize title as needed
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))); // Use green marker
            }
            // Add end marker
            if (endPoint != null) {
                googleMap.addMarker(new MarkerOptions()
                        .position(endPoint)
                        .title("End Point") // Customize title as needed
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))); // Use red marker
            }
        }

        stopTracking();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (userLocationMarker != null) {
            userLocationMarker.remove();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            stepCounter.resetStepTracking();
        }
        if (googleMap != null) {
            googleMap.setMyLocationEnabled(false);
        }
    }

    // Stop tracking
    private void stopTracking() {
        isTracking = false;
        isPaused = false;
        timerHandler.removeCallbacks(timerRunnable);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            stepCounter.stopStepTracking();
        }
    }

    // Handle permission request results
    @SuppressLint("NewApi")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ACTIVITY_RECOGNITION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onStepCounterPermissionGranted();
            } else {
                Toast.makeText(this, "Activity recognition permission denied", Toast.LENGTH_SHORT).show();
                Log.e("StepCounter", "Activity recognition permission denied");
            }
        }
    }

    // Check and request GPS location permission

    // Display default map when location permission is not granted
    private void showDefaultMap() {
        // Display default image
        Bitmap defaultBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bg_workout);
        if (defaultBitmap != null) {
            // You can set the bitmap to an ImageView here
            ImageView mapImageView = findViewById(R.id.default_image_view);
            mapImageView.setImageBitmap(defaultBitmap);
            mapImageView.setVisibility(View.VISIBLE);
        }
        // Ensure step count and timer views are visible
        stepTextView.setVisibility(View.VISIBLE);
        timerTextView.setVisibility(View.VISIBLE);
    }

    // Restore step counter listener on start
    @Override
    protected void onStart() {
        super.onStart();
        // Restore step counter listener
        if (isTracking && !isPaused && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            stepCounter.registerListener();
        }
    }

    // Handle tracking state on resume
    @Override
    protected void onResume() {
        super.onResume();

        // If tracking is not paused, re-enable map tracking
        if (isTracking && !isPaused) {
            startTracking();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                stepCounter.registerListener();
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                stepCounter.unregisterListener();
            }
        }

        btnShow.setVisibility(View.VISIBLE);
    }

    // Remove location updates and unregister step counter on stop
    @Override
    protected void onStop() {
        super.onStop();

        // Remove location updates and unregister step counter in onStop
        fusedLocationClient.removeLocationUpdates(locationCallback);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            stepCounter.unregisterListener();
        }

        // If tracking, pause the timer
        if (isTracking) {
            pauseTracking();
        }
    }

    // Clean up resources on destroy
    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Remove location updates to prevent memory leaks
        fusedLocationClient.removeLocationUpdates(locationCallback);

        // Remove timer callbacks to prevent memory leaks
        timerHandler.removeCallbacks(timerRunnable);

        // Clean up step counter
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            stepCounter.unregisterListener();
        }
    }

    // Save instance state
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save tracking and timer state
        outState.putBoolean("isTracking", isTracking);
        outState.putBoolean("isPaused", isPaused);
        outState.putLong("startTime", startTime);
        outState.putLong("pauseTime", pauseTime);
    }

    // Restore instance state
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Restore tracking and timer state
        isTracking = savedInstanceState.getBoolean("isTracking", false);
        isPaused = savedInstanceState.getBoolean("isPaused", false);
        startTime = savedInstanceState.getLong("startTime", 0L);
        pauseTime = savedInstanceState.getLong("pauseTime", 0L);

        if (isTracking && !isPaused) {
            startTracking();
        }
    }

}
