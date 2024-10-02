package com.example.pulsestepapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

/**
 * RunSummaryActivity displays the summary of a run, including distance, time, address, step count,
 * and the trajectory on a Google Map.
 */
public class RunSummaryActivity extends AppCompatActivity implements OnMapReadyCallback {

    // UI Components
    private GoogleMap googleMap;
    private SupportMapFragment mapFragment;
    private TextView distanceTextView;
    private TextView timeTextView;
    private TextView addressTextView;
    private TextView stepCountTextView;
    private ImageView defaultBackground;

    // Tracking Data
    private float distance; // in kilometers
    private String time; // formatted as "MM:SS"
    private String address; // optional
    private int stepCount;
    private ArrayList<LatLng> trajectory;
    private CardView mapCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_run_summary);

        // Initialize UI components
        initializeUIComponents();

        // Retrieve data from Intent
        retrieveIntentData();

        // Display data
        displayData();

        // Initialize and set up the map
        setupMap(savedInstanceState);
    }

    /**
     * Initializes the UI components by finding them via their IDs.
     */
    private void initializeUIComponents() {
        distanceTextView = findViewById(R.id.run_distance);
        timeTextView = findViewById(R.id.run_summary_time);
        addressTextView = findViewById(R.id.run_summary_address);
        stepCountTextView = findViewById(R.id.run_summary_steps);
        defaultBackground = findViewById(R.id.default_background);
        mapCard = findViewById(R.id.map_container);
    }

    /**
     * Retrieves data passed from the tracking activity via Intent.
     */
    private void retrieveIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            distance = intent.getFloatExtra("distance", 0.0f);
            time = intent.getStringExtra("time");
            stepCount = intent.getIntExtra("stepCount", 0);
            trajectory = intent.getParcelableArrayListExtra("trajectory");
            address = intent.getStringExtra("address"); // Optional
        }
    }

    /**
     * Displays the retrieved data on the UI components.
     */
    private void displayData() {
        distanceTextView.setText(String.format("%.2f", distance));
        timeTextView.setText(time != null ? time : "00:00");
        stepCountTextView.setText(String.valueOf(stepCount));
        addressTextView.setText(address != null ? address : "N/A");
    }

    /**
     * Sets up the Google Map.
     *
     * @param savedInstanceState The saved instance state.
     */
    private void setupMap(Bundle savedInstanceState) {
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_fragment_summary);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Toast.makeText(this, "Error initializing map.", Toast.LENGTH_SHORT).show();
            showDefaultBackground();
        }
    }

    /**
     * Callback when the Google Map is ready to be used.
     *
     * @param map The GoogleMap instance.
     */
    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;

        if (trajectory != null && !trajectory.isEmpty()) {
            displayTrajectoryOnMap();
        } else {
            showDefaultBackground();
        }
    }

    /**
     * Displays the trajectory on the Google Map with start and end markers.
     */
    private void displayTrajectoryOnMap() {
        if (googleMap == null || trajectory == null || trajectory.isEmpty()) return;

        // Draw the polyline
        PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(trajectory)
                .color(getResources().getColor(R.color.like_orange))
                .width(10);
        googleMap.addPolyline(polylineOptions);

        // Add start and end markers
        LatLng startPoint = trajectory.get(0);
        LatLng endPoint = trajectory.get(trajectory.size() - 1);

        googleMap.addMarker(new MarkerOptions()
                .position(startPoint)
                .title("Start Point")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

        googleMap.addMarker(new MarkerOptions()
                .position(endPoint)
                .title("End Point")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

        // Adjust camera to include all points
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng point : trajectory) {
            builder.include(point);
        }
        LatLngBounds bounds = builder.build();

        // Adjust the camera to the calculated bounds before displaying the path
        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));

        // Hide default background if trajectory is present
        defaultBackground.setVisibility(View.GONE);
    }


    /**
     * Shows a default background image if no trajectory is available.
     */
    private void showDefaultBackground() {
        // Show default background image
        mapCard.setVisibility(View.GONE);
        defaultBackground.setVisibility(View.VISIBLE);
    }

    /**
     * Lifecycle methods to manage MapFragment's state.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (mapFragment != null) {
            mapFragment.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapFragment != null) {
            mapFragment.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapFragment != null) {
            mapFragment.onDestroy();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapFragment != null) {
            mapFragment.onSaveInstanceState(outState);
        }
    }
}
