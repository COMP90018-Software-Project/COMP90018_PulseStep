package com.example.pulsestepapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
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

public class AmapActivity extends AppCompatActivity {

    // UI Components
    private MapView mMapView;
    private AMap aMap;
    private ImageButton btnPauseResume;
    private ImageButton btnShow;
    private TextView timerTextView, stepTextView, distanceTextView;

    // Tracking Variables
    private List<Polyline> polylines = new ArrayList<>();
    private List<LatLng> currentSegmentPoints = new ArrayList<>();
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
    private Handler timerHandler = new Handler();
    private Runnable timerRunnable = new Runnable() {
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupAmapPrivacy();

        setContentView(R.layout.activity_amap);

        // Initialize views
        mMapView = findViewById(R.id.amap_view);
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
                Intent intent = new Intent(AmapActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
        // Map and Step Counter setup
        initializeMapView(savedInstanceState);
        configureMap();

        setupStepCounter();
        setupButtonListeners();

    }

    // Step Counter Setup
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
    // Set up Button Listeners
    private void setupButtonListeners() {
        btnPauseResume.setOnClickListener(v -> handlePauseResumeButtonClick());
        btnShow.setOnClickListener(v -> showLastTrack());
    }
    // MapView Initialization
    private void initializeMapView(Bundle savedInstanceState) {
        mMapView = findViewById(R.id.amap_view);
        if (mMapView != null) {
            mMapView.onCreate(savedInstanceState);
            aMap = mMapView.getMap();
        }
    }

    private void configureMap() {
        if (aMap == null) return;
        // Set map type (e.g., normal, satellite, night mode, etc.)
        aMap.setMapType(AMap.MAP_TYPE_NORMAL); // Change to other types if needed

        // Set up custom location marker style
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        myLocationStyle.interval(2000); // Set the location update interval

        // Set a custom icon for the location marker
        Bitmap originalIconBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.img);
        int width = 80;  // Set the desired width (in pixels)
        int height = 80; // Set the desired height (in pixels)
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalIconBitmap, width, height, false);

        // Set the resized bitmap as the custom location marker
        myLocationStyle.myLocationIcon(BitmapDescriptorFactory.fromBitmap(resizedBitmap));

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


    // Button Handlers
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
            // Animate btnShow
            // animateShowButton();
            btnShow.setVisibility(View.GONE);
        }
    }

    private void animateShowButton() {
        // Calculate the start and end positions
        int[] btnPauseResumeLocation = new int[2];
        btnPauseResume.getLocationOnScreen(btnPauseResumeLocation);

        int[] btnShowLocation = new int[2];
        btnShow.getLocationOnScreen(btnShowLocation);

        float startX = btnPauseResumeLocation[0] + btnPauseResume.getWidth() / 2f - btnShow.getWidth() / 2f;
        float startY = btnPauseResumeLocation[1] + btnPauseResume.getHeight() / 2f - btnShow.getHeight() / 2f;

        // Set the initial position of btnShow
        btnShow.setTranslationX(startX);
        btnShow.setTranslationY(startY);

        // Perform the animation
        btnShow.animate()
                .translationX(btnShowLocation[0] - startX) // Move to the original position
                .translationY(btnShowLocation[1] - startY)
                .setDuration(300) // Duration of the animation
                .start();
    }

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

        // 获取当前位置并聚焦到地图上
        if (aMap.getMyLocation() != null) {
            LatLng currentLocation = new LatLng(aMap.getMyLocation().getLatitude(), aMap.getMyLocation().getLongitude());
            aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, MOVE_ZOOM_LEVEL));
        }
    }

    private void pauseTracking() {
        stepCounter.stopStepTracking();
        isPaused = true;
        pauseTime = SystemClock.elapsedRealtime();
        timerHandler.removeCallbacks(timerRunnable);
        //Toast.makeText(this, "Paused Tracking", Toast.LENGTH_SHORT).show();
    }

    private void resumeTracking() {
        stepCounter.startStepTracking();
        isPaused = false;
        startTime += (SystemClock.elapsedRealtime() - pauseTime);
        timerHandler.postDelayed(timerRunnable, 0);
        startNewSegment();
        //Toast.makeText(this, "Resumed Tracking", Toast.LENGTH_SHORT).show();
    }

    private void startNewSegment() {
        PolylineOptions polylineOptions = new PolylineOptions().width(10).color(0xFFFF0000);
        Polyline newPolyline = aMap.addPolyline(polylineOptions);
        polylines.add(newPolyline);
        currentSegmentPoints.clear();
    }

    private void updatePath(LatLng latLng) {
        //if (currentStepCount > realStep) {
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
        //}
    }}

    private void showLastTrack() {
        if (polylines.isEmpty()) {
            Toast.makeText(this, "No track to show", Toast.LENGTH_SHORT).show();
            return;
        }

        aMap.animateCamera(CameraUpdateFactory.newLatLngBounds(getLatLngBounds(), 100));
        isTracking = false;
        isPaused = false;
        timerHandler.removeCallbacks(timerRunnable);
        aMap.setMyLocationEnabled(false);
        stepCounter.stopStepTracking();

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
        if (startPoint != null) {
            aMap.addMarker(new MarkerOptions()
                    .position(startPoint)
                    .title("Start Point")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))); // 使用绿色标记
        }
        if (endPoint != null) {
            aMap.addMarker(new MarkerOptions()
                    .position(endPoint)
                    .title("End Point")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))); // 使用红色标记
        }
    }

    private void checkBoundaryAndAdjustZoom(LatLng newPoint) {
        if (!aMap.getProjection().getVisibleRegion().latLngBounds.contains(newPoint)) {
            aMap.animateCamera(CameraUpdateFactory.newLatLngBounds(getLatLngBounds(), 100));
        }
    }

    private LatLngBounds getLatLngBounds() {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (Polyline polyline : polylines) {
            for (LatLng point : polyline.getPoints()) {
                builder.include(point);
            }
        }
        return builder.build();
    }

    // Permission Handling
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
            }
        }
    }

    // Map Privacy Settings
    private void setupAmapPrivacy() {
        MapsInitializer.updatePrivacyShow(this, true, true);
        MapsInitializer.updatePrivacyAgree(this, true);
    }

    // Lifecycle Methods
    @Override
    protected void onResume() {
        super.onResume();
        if (mMapView != null) {
            mMapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mMapView != null) {
            mMapView.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMapView != null) {
            mMapView.onDestroy();
        }
        timerHandler.removeCallbacks(timerRunnable);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mMapView != null) {
            mMapView.onSaveInstanceState(outState);
        }
    }
}
