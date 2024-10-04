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
            trajectory = intent.getParcelableArrayListExtra("trajectory");
            address = intent.getStringExtra("address");
            avgPace = intent.getStringExtra("avgPace");
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
        if (aMap == null || trajectory == null || trajectory.isEmpty()) return;

        // Define a dashed pattern for gaps in the trajectory
        List<PatternItem> dashedPattern = Arrays.asList(new Dash(30), new Gap(20));

        LatLng startPoint = null;
        LatLng endPoint = null;

        // Builder to adjust camera bounds to include all points
        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        // Polyline options for the current solid segment
        PolylineOptions polylineOptions = new PolylineOptions()
                .color(getResources().getColor(R.color.like_orange))
                .width(10);

        // Iterate over the trajectory
        for (int i = 0; i < trajectory.size(); i++) {
            LatLng point = trajectory.get(i);

            if (point == null) {
                // If a break is encountered, draw the current solid polyline if it has points
                if (!polylineOptions.getPoints().isEmpty()) {
                    aMap.addPolyline(polylineOptions);
                    polylineOptions = new PolylineOptions()
                            .color(getResources().getColor(R.color.like_orange))
                            .width(10);
                }

                // Attempt to draw a dashed line between previous and next valid points
                LatLng previousPoint = null;
                for (int j = i - 1; j >= 0; j--) {
                    previousPoint = trajectory.get(j);
                    if (previousPoint != null) break;
                }
                LatLng nextPoint = null;
                for (int j = i + 1; j < trajectory.size(); j++) {
                    nextPoint = trajectory.get(j);
                    if (nextPoint != null) break;
                }
                if (previousPoint != null && nextPoint != null) {
                    // Draw a dashed line connecting previousPoint and nextPoint
                    if (trajectory.indexOf(nextPoint) != trajectory.size() - 1) {
                        PolylineOptions dashedLineOptions = new PolylineOptions()
                                .add(previousPoint)
                                .add(nextPoint)
                                .color(getResources().getColor(R.color.like_orange))
                                .width(10)
                                .setDottedLine(true);
                        aMap.addPolyline(dashedLineOptions);
                    }}
            } else {
                // Add point to the current solid polyline
                polylineOptions.add(point);
                if (startPoint == null) {
                    startPoint = point;
                }
                endPoint = point;
                builder.include(point);
            }
        }

        // Draw the last solid polyline if it has points
        if (!polylineOptions.getPoints().isEmpty()) {
            aMap.addPolyline(polylineOptions);
        }

        if (startPoint != null) {
            // Add a marker at the starting point
            aMap.addMarker(new MarkerOptions()
                    .position(startPoint)
                    .title("Start Point")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

            // Add a marker at the ending point
            aMap.addMarker(new MarkerOptions()
                    .position(endPoint)
                    .title("End Point")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        }

        // Adjust the camera to include all points in the trajectory
        LatLngBounds bounds = builder.build();
        aMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));

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
