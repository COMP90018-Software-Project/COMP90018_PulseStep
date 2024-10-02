package com.example.pulsestepapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.MapsInitializer;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.PolylineOptions;

import java.util.ArrayList;

public class RunSummaryActivity extends AppCompatActivity {

    // UI Components
    private MapView mMapView;
    private AMap aMap;
    private TextView distanceTextView;
    private TextView timeTextView;
    private TextView addressTextView;
    private TextView stepCountTextView;

    // Tracking Data
    private float distance; // in kilometers
    private String time; // formatted as "MM:SS"
    private String address; // optional
    private int stepCount;
    private ArrayList<LatLng> trajectory;
    private ImageView imageview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupAmapPrivacy();

        setContentView(R.layout.activity_run_summary);

        // Initialize views
        initializeUIComponents();

        // Retrieve data from Intent
        retrieveIntentData();

        // Initialize MapView
        initializeMapView(savedInstanceState);

        // Display data
        displayData();

        // Display trajectory on map
        if (trajectory != null && !trajectory.isEmpty()) {
            displayTrajectory();
        } else {
            showDefaultBackground();
        }
    }

    /**
     * Initializes the UI components by finding them via their IDs.
     */
    private void initializeUIComponents() {
        imageview = findViewById(R.id.default_background);
        mMapView = findViewById(R.id.map_view_summary);
        distanceTextView = findViewById(R.id.run_distance);
        timeTextView = findViewById(R.id.run_summary_time);
        addressTextView = findViewById(R.id.run_summary_address);
        stepCountTextView = findViewById(R.id.run_summary_steps);
    }

    /**
     * Retrieves data passed from the tracking activity via Intent.
     */
    private void retrieveIntentData() {
        if (getIntent() != null) {
            distance = getIntent().getFloatExtra("distance", 0.0f);
            time = getIntent().getStringExtra("time");
            stepCount = getIntent().getIntExtra("stepCount", 0);
            trajectory = getIntent().getParcelableArrayListExtra("trajectory");
            address = getIntent().getStringExtra("address"); // Optional
        }
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
     * Displays the retrieved data on the UI components.
     */
    private void displayData() {
        distanceTextView.setText(String.format("%.2f km", distance));
        timeTextView.setText(time != null ? time : "00:00");
        stepCountTextView.setText(String.valueOf(stepCount));
        addressTextView.setText(address != null ? address : "N/A");
    }

    /**
     * Displays the trajectory on the map.
     */
    private void displayTrajectory() {
        if (aMap == null) return;

        // Draw the polyline
        PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(trajectory)
                .color(getResources().getColor(R.color.like_orange))
                .width(10);
        aMap.addPolyline(polylineOptions);

        // Add start and end markers
        LatLng startPoint = trajectory.get(0);
        LatLng endPoint = trajectory.get(trajectory.size() - 1);

        aMap.addMarker(new MarkerOptions()
                .position(startPoint)
                .title("Start Point")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

        aMap.addMarker(new MarkerOptions()
                .position(endPoint)
                .title("End Point")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

        // Adjust camera to include all points
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng point : trajectory) {
            builder.include(point);
        }
        LatLngBounds bounds = builder.build();
        aMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
    }

    /**
     * Shows a default background image if no trajectory is available.
     */
    private void showDefaultBackground() {
        // Optionally, you can overlay an ImageView or adjust visibility of certain views
        Toast.makeText(this, "No trajectory data available", Toast.LENGTH_SHORT).show();
        imageview.setVisibility(View.VISIBLE);
    }

    /**
     * Applies AMap privacy settings.
     */
    private void setupAmapPrivacy() {
        MapsInitializer.updatePrivacyShow(this, true, true);
        MapsInitializer.updatePrivacyAgree(this, true);
    }

    /**
     * Lifecycle methods to manage MapView's state.
     */
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
