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
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    private String avgPace;
    private TextView avgPaceTextView;

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
        avgPaceTextView = findViewById(R.id.run_summary_avg_pace);
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
            address = intent.getStringExtra("address");
            avgPace = intent.getStringExtra("avgPave");
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
        avgPaceTextView.setText(avgPace);
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
    // Check if the map and trajectory data are valid
    private void displayTrajectoryOnMap() {
    if (googleMap == null || trajectory == null || trajectory.isEmpty()) return;

    // Define a dashed pattern for the disconnected segments
    List<PatternItem> dashedPattern = Arrays.asList(new Dash(30), new Gap(20));

    // Variables to keep track of the start and end points
    LatLng startPoint = null;
    LatLng endPoint = null;

    // Adjust camera bounds to include all points
    LatLngBounds.Builder builder = new LatLngBounds.Builder();

    // Variable to hold the points for the current solid polyline
    PolylineOptions polylineOptions = new PolylineOptions()
            .color(getResources().getColor(R.color.like_orange))
            .width(10);
        // To track the previous point to connect with dashed lines when a break is found
        LatLng previousPoint = null;

        for (int i = 0; i < trajectory.size(); i++) {
            LatLng point = trajectory.get(i);
            if (point == null) {
                // If we encounter a break (null) and we have a previous point
                if (previousPoint != null) {
                    // Look ahead to the next valid point after the null
                    int nextIndex = i + 1;
                    while (nextIndex < trajectory.size() && trajectory.get(nextIndex) == null) {
                        nextIndex++;
                    }
                    if (nextIndex < trajectory.size()) {
                        LatLng nextPoint = trajectory.get(nextIndex);

                        // Draw a dashed line connecting the previous point to the next point
                        PolylineOptions dashedLineOptions = new PolylineOptions()
                                .add(previousPoint)
                                .add(nextPoint)
                                .color(getResources().getColor(R.color.like_orange))
                                .width(10)
                                .pattern(dashedPattern);
                        googleMap.addPolyline(dashedLineOptions);
                    }
                }

                // Draw the current solid polyline
                googleMap.addPolyline(polylineOptions);

                // Reset polylineOptions for the next segment
                polylineOptions = new PolylineOptions()
                        .color(getResources().getColor(R.color.like_orange))
                        .width(10);

                // Reset the previousPoint as we encountered a break
                previousPoint = null;
            } else {
                // Add the point to the current polyline segment
                polylineOptions.add(point);

                // Set the startPoint if it is the first point
                if (startPoint == null) {
                    startPoint = point;
                }

                // Update the endPoint to the current point
                endPoint = point;

                // Include the point in the camera bounds
                builder.include(point);

                // Update the previous point
                previousPoint = point;
            }
        }

        // Draw the last solid polyline segment
        if (polylineOptions.getPoints().size() > 0) {
            googleMap.addPolyline(polylineOptions);
        }

        if (startPoint != null) {
            // Add a marker at the starting point
            googleMap.addMarker(new MarkerOptions()
                    .position(startPoint)
                    .title("Start Point")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

            // Add a marker at the ending point
            googleMap.addMarker(new MarkerOptions()
                    .position(endPoint)
                    .title("End Point")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        }

        // Adjust the camera to include all points in the trajectory
        LatLngBounds bounds = builder.build();
        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));

        // Hide the default background if the trajectory is present
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
