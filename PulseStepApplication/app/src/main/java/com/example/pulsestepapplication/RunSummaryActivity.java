package com.example.pulsestepapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.amap.api.maps.AMap;
import com.amap.api.maps.MapView;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapsInitializer;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.PolylineOptions;
import com.amap.api.maps.model.Polyline;

import java.util.ArrayList;

public class RunSummaryActivity extends AppCompatActivity {

    private static final float MOVE_ZOOM_LEVEL = 18;
    // UI Components
    private MapView mMapView;
    private AMap aMap;
    private TextView distanceTextView, timeTextView, addressTextView, stepsTextView;

    // Trajectory Data
    private ArrayList<LatLng> trajectory;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupAmapPrivacy();

        setContentView(R.layout.activity_run_summary);

        // Initialize views
        initializeUIComponents();

        // Initialize MapView
        initializeMapView(savedInstanceState);

        // Retrieve data from Intent
        retrieveIntentData();

        // Display trajectory on map if available
        if (trajectory != null && !trajectory.isEmpty()) {
            displayTrajectory();
        } else {
            // Show default background image or handle accordingly
            Toast.makeText(this, "No trajectory data available", Toast.LENGTH_SHORT).show();
            // Optionally, you can display the default background image which is already set in XML
        }

        // Display stats
        displayStats();
    }

    /**
     * Initializes the UI components by finding them via their IDs.
     */
    private void initializeUIComponents() {
        mMapView = findViewById(R.id.map_view_summary);
        distanceTextView = findViewById(R.id.run_distance);
        timeTextView = findViewById(R.id.run_summary_time);
        addressTextView = findViewById(R.id.run_summary_address);
        stepsTextView = findViewById(R.id.run_summary_steps);
    }

    /**
     * Initializes the MapView and restores its state.
     *
     * @param savedInstanceState The saved instance state.
     */
    private void initializeMapView(Bundle savedInstanceState) {
        mMapView.onCreate(savedInstanceState);
        aMap = mMapView.getMap();

        // Configure MapView settings if needed
        aMap.getUiSettings().setAllGesturesEnabled(false); // Disable gestures if map is just for display
        aMap.setMapType(AMap.MAP_TYPE_NORMAL);
    }

    /**
     * Retrieves data passed via Intent.
     */
    private void retrieveIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            float distance = intent.getFloatExtra("distance", 0.0f);
            String time = intent.getStringExtra("time");
            String address = intent.getStringExtra("address");
            int steps = intent.getIntExtra("steps", 0);
            trajectory = intent.getParcelableArrayListExtra("trajectory");

            // Set the data to views
            distanceTextView.setText(String.format("%.2f", distance));
            timeTextView.setText(time);
            addressTextView.setText(address);
            stepsTextView.setText(String.valueOf(steps));
        }
    }

    /**
     * Displays the trajectory on the map by drawing polylines.
     */
    private void displayTrajectory() {
        if (aMap == null || trajectory == null || trajectory.isEmpty()) return;

        PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(trajectory)
                .width(10)
                .color(getResources().getColor(R.color.like_orange)); // Customize color as needed

        aMap.addPolyline(polylineOptions);

        // Move camera to the start of the trajectory
        LatLng startPoint = trajectory.get(0);
        aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(startPoint, MOVE_ZOOM_LEVEL));
    }

    /**
     * Displays the run statistics in the respective TextViews.
     */
    private void displayStats() {
        // Already set in retrieveIntentData()
        // Additional formatting can be done here if needed
    }

    /**
     * Applies AMap privacy settings.
     */
    private void setupAmapPrivacy() {
        MapsInitializer.updatePrivacyShow(this, true, true);
        MapsInitializer.updatePrivacyAgree(this, true);
    }

    // Lifecycle Methods for MapView
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
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mMapView != null) {
            mMapView.onSaveInstanceState(outState);
        }
    }

}
