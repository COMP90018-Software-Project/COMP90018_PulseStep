package com.example.pulsestepapplication;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.MapsInitializer;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CustomMapStyleOptions;
import com.amap.api.maps.model.LatLng;

import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.PatternItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * AmapRunSummaryActivity displays the summary of a run, including distance, time, address, step count,
 * and the trajectory on an AMap.
 */
public class AmapRunSummaryActivity extends AppCompatActivity {

    // UI Components
    private AMap aMap;
    private MapView mapView;
    private TextView distanceTextView;
    private TextView timeTextView;
    private TextView addressTextView;
    private TextView stepCountTextView;
    private ImageView defaultBackground;
    private TextView caloriesTextView;
    // Tracking Data
    private float distance; // in kilometers
    private String time; // formatted as "MM:SS"
    private String address; // optional
    private int stepCount;
    private ArrayList<LatLng> trajectory;
    private CardView mapCard;
    private String avgPace;
    private TextView avgPaceTextView;
    private Button finishButton;

    private static final String TAG = "AmapRunSummaryActivity";
    private String calories;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupAmapPrivacy();
        setContentView(R.layout.activity_amap_run_summary);

        // Initialize UI components
        initializeUIComponents();

        // Retrieve data from Intent
        retrieveIntentData();

        // Display data
        displayData();

        // Initialize and set up the map
        setupMap(savedInstanceState);

        finishButton.setOnClickListener(v -> {
            Intent intent = new Intent(AmapRunSummaryActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra("fragment", "WorkoutFragment");
            startActivity(intent);
            finish();
        });
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
        caloriesTextView = findViewById(R.id.run_summary_calories);
        defaultBackground = findViewById(R.id.default_background);
        mapCard = findViewById(R.id.map_container);
        finishButton = findViewById(R.id.bt_finish_run);
        mapView = findViewById(R.id.map_view_summary);
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
            if(distance > 0.01){
                trajectory = intent.getParcelableArrayListExtra("trajectory");
            }else{
                trajectory = null;
            }
            address = intent.getStringExtra("address");
            avgPace = intent.getStringExtra("avgPace");
            calories = intent.getStringExtra("calories");
        }
    }

    /**
     * Displays the retrieved data on the UI components.
     */
    private void displayData() {
        Log.d("DEBUG", "Average Pace: " + avgPace);
        distanceTextView.setText(String.format("%.2f", distance));
        timeTextView.setText(time != null ? time : "00:00");
        stepCountTextView.setText(String.valueOf(stepCount));
        addressTextView.setText(address != null ? address : "N/A");
        avgPaceTextView.setText(avgPace);
        caloriesTextView.setText(calories);
    }

    /**
     * Sets up the AMap.
     *
     * @param savedInstanceState The saved instance state.
     */
    private void setupMap(Bundle savedInstanceState) {
        mapView.onCreate(savedInstanceState);
        aMap = mapView.getMap();

        if (trajectory != null && !trajectory.isEmpty()) {
            displayTrajectoryOnMap();
        } else {
            showDefaultBackground();
        }
    }

    /**
     * Displays the trajectory on the AMap with start and end markers.
     */
    private void displayTrajectoryOnMap() {
        // Ensure that the map and trajectory data are available
        if (aMap == null || trajectory == null || trajectory.isEmpty()) return;

        LatLng startPoint = null;
        LatLng endPoint = null;

        // Builder to adjust the camera bounds to include all trajectory points
        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        // Initialize PolylineOptions for the current solid segment
        PolylineOptions solidLineOptions = new PolylineOptions()
                .color(getResources().getColor(R.color.like_orange)) // Set the color for the polyline
                .width(10); // Set the width of the polyline

        // Iterate over the trajectory list
        for (int i = 0; i < trajectory.size(); i++) {
            LatLng point = trajectory.get(i);

            if (point == null) {
                // Encountered a break in the trajectory, draw the current solid polyline if it has points
                if (!solidLineOptions.getPoints().isEmpty()) {
                    aMap.addPolyline(solidLineOptions); // Add the solid polyline to the map
                    // Reset PolylineOptions for the next solid segment
                    solidLineOptions = new PolylineOptions()
                            .color(getResources().getColor(R.color.like_orange))
                            .width(10);
                }

                // Attempt to draw a dashed line between the previous and next valid points
                LatLng previousPoint = null;
                // Find the previous non-null point
                for (int j = i - 1; j >= 0; j--) {
                    previousPoint = trajectory.get(j);
                    if (previousPoint != null) break;
                }

                LatLng nextPoint = null;
                // Find the next non-null point
                for (int j = i + 1; j < trajectory.size(); j++) {
                    nextPoint = trajectory.get(j);
                    if (nextPoint != null) break;
                }

                if (previousPoint != null && nextPoint != null) {
                    // Ensure that the nextPoint is not the last point in the trajectory to avoid connecting to the endpoint
                    if (trajectory.indexOf(nextPoint) != trajectory.size() - 1) {
                        // Create PolylineOptions for the dashed line
                        PolylineOptions dashedLineOptions = new PolylineOptions()
                                .add(previousPoint) // Start point of the dashed line
                                .add(nextPoint)     // End point of the dashed line
                                .color(getResources().getColor(R.color.like_orange)) // Set the color
                                .width(10) // Set the width
                                .setDottedLine(true);// Apply the dashed pattern
                        aMap.addPolyline(dashedLineOptions); // Add the dashed polyline to the map
                    }
                }
            } else {
                // Add the current point to the solid polyline
                solidLineOptions.add(point);
                if (startPoint == null) {
                    startPoint = point; // Set the start point if it's the first point
                }
                endPoint = point; // Update the end point to the current point
                builder.include(point); // Include the point in the LatLngBounds builder
            }
        }

        // Draw the last solid polyline if it has points
        if (!solidLineOptions.getPoints().isEmpty()) {
            aMap.addPolyline(solidLineOptions);
        }

        if (startPoint != null) {
            // Add a marker at the starting point with a green icon
            aMap.addMarker(new MarkerOptions()
                    .position(startPoint)
                    .title("Start Point")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

            // Add a marker at the ending point with a red icon
            aMap.addMarker(new MarkerOptions()
                    .position(endPoint)
                    .title("End Point")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        }

        // Adjust the camera to include all points within the trajectory
        LatLngBounds bounds = builder.build();
        aMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100)); // 100 is the padding in pixels

        // Hide the default background view if the trajectory is present
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
     * Lifecycle methods to manage MapView's state.
     */
    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    /**
     * 设置高德地图隐私
     */
    private void setupAmapPrivacy() {
        MapsInitializer.updatePrivacyShow(this, true, true);
        MapsInitializer.updatePrivacyAgree(this, true);
    }
}
