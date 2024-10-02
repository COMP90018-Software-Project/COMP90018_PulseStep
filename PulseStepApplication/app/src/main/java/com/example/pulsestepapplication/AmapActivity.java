package com.example.pulsestepapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.MapsInitializer;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;
import com.google.android.gms.location.FusedLocationProviderClient;

import java.util.ArrayList;
import java.util.List;

/**
 * AmapActivity handles the tracking and display of the user's movement using AMap.
 */
public class AmapActivity extends AppCompatActivity {

    // UI Components
    private MapView mMapView;
    private AMap aMap;
    private ImageButton btnPauseResume;
    private ImageButton btnShow;
    private TextView timerTextView, stepTextView, distanceTextView;
    private ImageView backButton;

    // Tracking Variables
    private final List<Polyline> polylines = new ArrayList<>();
    private final List<LatLng> currentSegmentPoints = new ArrayList<>();
    private boolean isTracking = false;
    private boolean isPaused = false;
    private boolean isLocationReady = false;
    private float totalDistance = 0.0f;
    private final int realStep = 1;
    private boolean isFirstLocationUpdate = true; // Flag to track the first location update
    private LatLng lastKnownLatLng = null; // To store the last known location
    private static final float MIN_DISTANCE_CHANGE_FOR_UPDATE = 50; // Minimum distance in meters to consider as a significant change
    private static final float MIN_ACCURACY_THRESHOLD = 50; // Minimum accuracy in meters to consider the location valid

    // Step Counter and Location
    private StepCounter stepCounter;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int ACTIVITY_RECOGNITION_REQUEST_CODE = 1000;
    private static final int LOCATION_REQUEST_CODE = 1001;
    private static final int NETWORK_REQUEST_CODE = 1002;
    private static final float DEFAULT_ZOOM_LEVEL = 15f;
    private static final float MOVE_ZOOM_LEVEL = 17f;
    private int currentStepCount = 0;

    // Timer-related
    private long startTime = 0L;
    private long pauseTime = 0L;
    private final Handler timerHandler = new Handler();
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long millis = SystemClock.elapsedRealtime() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds %= 60;
            timerTextView.setText(String.format("%02d:%02d", minutes, seconds));
            timerHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupAmapPrivacy();

        setContentView(R.layout.activity_amap);

        // Initialize views
        initializeUIComponents();

        // Initialize map and configure it
        initializeMapView(savedInstanceState);
        configureMap();

        // Set up step counter
        setupStepCounter();

        // Set up button listeners
        setupButtonListeners();
    }

    /**
     * Initializes the UI components by finding them via their IDs.
     */
    private void initializeUIComponents() {
        mMapView = findViewById(R.id.amap_view);
        btnPauseResume = findViewById(R.id.btn_stop);
        btnShow = findViewById(R.id.btn_show);
        timerTextView = findViewById(R.id.timer_text_view);
        stepTextView = findViewById(R.id.step_text_view);
        distanceTextView = findViewById(R.id.distance_text_view);
        backButton = findViewById(R.id.back_button_running_page);

        // Set click listener for the back button
        backButton.setOnClickListener(v -> navigateToMainActivity());
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
     * Initializes the MapView and restores its state.
     *
     * @param savedInstanceState The saved instance state.
     */
    private void initializeMapView(Bundle savedInstanceState) {
        mMapView.onCreate(savedInstanceState);
        aMap = mMapView.getMap();
    }

    /**
     * Configures the AMap with custom styles and location settings.
     */
    private void configureMap() {
        if (aMap == null) return;

        // Set map type (e.g., normal, satellite)
        aMap.setMapType(AMap.MAP_TYPE_NORMAL);

        // Set up custom location marker style
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        myLocationStyle.interval(2000); // Set the location update interval

        // Set a custom icon for the location marker
        Bitmap originalIconBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.img);
        if (originalIconBitmap != null) {
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalIconBitmap, 80, 80, false);
            myLocationStyle.myLocationIcon(BitmapDescriptorFactory.fromBitmap(resizedBitmap));
        }

        // Set the accuracy circle color (optional)
        myLocationStyle.strokeColor(0x00000000); // Transparent color
        myLocationStyle.radiusFillColor(0x30000000); // Semi-transparent color

        aMap.setMyLocationStyle(myLocationStyle);
        aMap.setMyLocationEnabled(true); // Enable location blue dot

        // Hide certain map features (if necessary)
        aMap.showBuildings(false); // Hide buildings
        aMap.showMapText(false); // Hide POI names

        // Set up the location change listener to update the path
        aMap.setOnMyLocationChangeListener(location -> {
            if (location != null) {
                isLocationReady = true;
                if (isTracking) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    LatLng newPoint = new LatLng(latitude, longitude);
                    updatePath(newPoint);
                    checkBoundaryAndAdjustZoom(newPoint);
                }
            }
        });
    }

    /**
     * Sets up the StepCounter and initializes its listener.
     */
    private void setupStepCounter() {
        if (!checkActivityRecognitionPermission()) {
            requestActivityRecognitionPermission();
            return;
        }
        stepCounter = new StepCounter(this);
        stepCounter.setStepCounterListener(stepCount -> runOnUiThread(() -> {
            stepTextView.setText(String.valueOf(stepCount));
            currentStepCount = stepCount;
            if (stepCount > realStep && isLocationReady) {
                startTracking();
            }
        }));
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
        if (!isTracking && !isPaused) {
            startTracking();
            btnPauseResume.setImageDrawable(getResources().getDrawable(R.drawable.pause));
            btnShow.setVisibility(View.GONE);
        } else if (isTracking && !isPaused) {
            pauseTracking();
            btnPauseResume.setImageDrawable(getResources().getDrawable(R.drawable.start));
            btnShow.setVisibility(View.VISIBLE);
        } else if (isTracking && isPaused) {
            resumeTracking();
            btnPauseResume.setImageDrawable(getResources().getDrawable(R.drawable.pause));
            btnShow.setVisibility(View.GONE);
        }
    }

    /**
     * Starts the tracking process, including location updates and step tracking.
     */
    private void startTracking() {
        isTracking = true;
        isPaused = false;
        totalDistance = 0.0f;
        distanceTextView.setText("0.0 km");
        currentSegmentPoints.clear();
        startNewSegment();
        startTime = SystemClock.elapsedRealtime();
        timerHandler.postDelayed(timerRunnable, 0);
        aMap.setMyLocationEnabled(true);
        stepCounter.startStepTracking();

        // Get current location and focus the map
        if (aMap.getMyLocation() != null) {
            LatLng currentLocation = new LatLng(aMap.getMyLocation().getLatitude(), aMap.getMyLocation().getLongitude());
            aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, MOVE_ZOOM_LEVEL));
        }
    }

    /**
     * Pauses the tracking process.
     */
    private void pauseTracking() {
        stepCounter.stopStepTracking();
        isPaused = true;
        pauseTime = SystemClock.elapsedRealtime();
        timerHandler.removeCallbacks(timerRunnable);
        btnPauseResume.setImageDrawable(getResources().getDrawable(R.drawable.start));
        btnShow.setVisibility(View.VISIBLE);
        drawCurrentPolyline();
    }

    /**
     * Resumes the tracking process after a pause.
     */
    private void resumeTracking() {
        stepCounter.startStepTracking();
        isPaused = false;
        startTime += (SystemClock.elapsedRealtime() - pauseTime);
        timerHandler.postDelayed(timerRunnable, 0);
        startNewSegment();
    }

    /**
     * Starts a new segment for path tracking.
     */
    private void startNewSegment() {
        PolylineOptions polylineOptions = new PolylineOptions().width(10).color(0xFFFF0000);
        Polyline newPolyline = aMap.addPolyline(polylineOptions);
        polylines.add(newPolyline);
        currentSegmentPoints.clear();
    }

    /**
     * Updates the path with the new location and calculates the total distance.
     *
     * @param latLng The new location coordinates.
     */
    private void updatePath(LatLng latLng) {
        if (!currentSegmentPoints.isEmpty()) {
            LatLng lastLatLng = currentSegmentPoints.get(currentSegmentPoints.size() - 1);
            float[] results = new float[1];
            Location.distanceBetween(lastLatLng.latitude, lastLatLng.longitude, latLng.latitude, latLng.longitude, results);
            totalDistance += results[0];
            distanceTextView.setText(String.format("%.2f km", totalDistance / 1000));
        }
        currentSegmentPoints.add(latLng);
        if (!polylines.isEmpty()) {
            Polyline currentPolyline = polylines.get(polylines.size() - 1);
            currentPolyline.setPoints(new ArrayList<>(currentSegmentPoints));
        }
    }

    /**
     * Draws the current polyline on the map.
     */
    private void drawCurrentPolyline() {
        if (!currentSegmentPoints.isEmpty()) {
            PolylineOptions polylineOptions = new PolylineOptions().addAll(currentSegmentPoints).color(getResources().getColor(R.color.like_orange)).width(10);
            if (polylines.isEmpty() || isPaused) {
                Polyline polyline = aMap.addPolyline(polylineOptions);
                polylines.add(polyline);
            } else {
                polylines.get(polylines.size() - 1).remove();
                Polyline polyline = aMap.addPolyline(polylineOptions);
                polylines.set(polylines.size() - 1, polyline);
            }
        }
    }

    /**
     * Shows the last tracked path on the map with start and end markers.
     */
    private void showLastTrack() {
        if (polylines.isEmpty()) {
            Toast.makeText(this, "No track to show", Toast.LENGTH_SHORT).show();
            return;
        }

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        LatLng startPoint = null;
        LatLng endPoint = null;
        for (Polyline polyline : polylines) {
            List<LatLng> points = polyline.getPoints();
            if (points.size() > 0) {
                if (startPoint == null) {
                    startPoint = points.get(0);
                }
                endPoint = points.get(points.size() - 1);
            }
            for (LatLng point : points) {
                builder.include(point);
            }
        }
        LatLngBounds bounds = builder.build();
        aMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));

        if (totalDistance > 0.02) { // If distance is greater than 20 meters
            // Add start marker
            if (startPoint != null) {
                aMap.addMarker(new MarkerOptions()
                        .position(startPoint)
                        .title("Start Point")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            }
            // Add end marker
            if (endPoint != null) {
                aMap.addMarker(new MarkerOptions()
                        .position(endPoint)
                        .title("End Point")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            }
        }

        stopTracking();
        removeUserLocationMarker();
        resetStepCounter();
    }

    /**
     * Checks if the new point is within the current map bounds and adjusts the zoom if necessary.
     *
     * @param newPoint The new location point.
     */
    private void checkBoundaryAndAdjustZoom(LatLng newPoint) {
        if (!aMap.getProjection().getVisibleRegion().latLngBounds.contains(newPoint)) {
            aMap.animateCamera(CameraUpdateFactory.newLatLngBounds(getLatLngBounds(), 100));
        }
    }

    /**
     * Calculates the LatLngBounds that include all tracked points.
     *
     * @return The calculated LatLngBounds.
     */
    private LatLngBounds getLatLngBounds() {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (Polyline polyline : polylines) {
            for (LatLng point : polyline.getPoints()) {
                builder.include(point);
            }
        }
        return builder.build();
    }

    /**
     * Removes the user location marker from the map.
     */
    private void removeUserLocationMarker() {
        // Assuming you have a reference to the user location marker, remove it here
        // Example:
        // if (userLocationMarker != null) {
        //     userLocationMarker.remove();
        // }
        aMap.setMyLocationEnabled(false);
    }

    /**
     * Resets the step counter.
     */
    private void resetStepCounter() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && stepCounter != null) {
            stepCounter.resetStepTracking();
        }
    }

    /**
     * Stops the tracking process and resets related variables.
     */
    private void stopTracking() {
        isTracking = false;
        isPaused = false;
        timerHandler.removeCallbacks(timerRunnable);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && stepCounter != null) {
            stepCounter.stopStepTracking();
        }
    }

    /**
     * Applies AMap privacy settings.
     */
    private void setupAmapPrivacy() {
        MapsInitializer.updatePrivacyShow(this, true, true);
        MapsInitializer.updatePrivacyAgree(this, true);
    }

    /**
     * Checks and requests necessary permissions.
     */
    private void checkPermissions() {
        if (!checkNetworkPermission()) {
            requestNetworkPermission();
        }
        if (!checkLocationPermission()) {
            requestLocationPermission();
        }
        if (!checkActivityRecognitionPermission()) {
            requestActivityRecognitionPermission();
        }
    }

    private boolean checkNetworkPermission() {
        // Placeholder for checking network-related permissions
        // Android does not directly require permissions for network use, but permissions like ACCESS_NETWORK_STATE can be checked here if needed.
        return true; // Assume permission is granted for simplicity
    }

    private boolean checkLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean checkActivityRecognitionPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
    }

    private void requestActivityRecognitionPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, ACTIVITY_RECOGNITION_REQUEST_CODE);
    }

    private void requestNetworkPermission() {
        // Placeholder for requesting network-related permissions if required in the future
    }

    /**
     * Handles the result of permission requests.
     *
     * @param requestCode  The request code passed in requestPermissions().
     * @param permissions  The requested permissions.
     * @param grantResults The grant results for the corresponding permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                configureMap();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == ACTIVITY_RECOGNITION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupStepCounter();
            } else {
                Toast.makeText(this, "Activity recognition permission denied", Toast.LENGTH_SHORT).show();
                Log.e("StepCounter", "Activity recognition permission denied");
            }
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
        if (mMapView != null) {
            mMapView.onSaveInstanceState(outState);
        }
    }

    /**
     * Resumes the MapView and handles tracking state.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (mMapView != null) {
            mMapView.onResume();
        }

        // If tracking is not paused, re-enable map tracking
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
     * Pauses the MapView and removes location updates.
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (mMapView != null) {
            mMapView.onPause();
        }
    }

    /**
     * Destroys the MapView and cleans up resources.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMapView != null) {
            mMapView.onDestroy();
        }
        timerHandler.removeCallbacks(timerRunnable);
    }
}
