package com.example.pulsestepapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.gms.common.SignInButton;
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
    private Button finishButton;

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

        finishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RunSummaryActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra("fragment", "WorkoutFragment"); // 可选：传递参数以指示返回到WorkoutFragment
                startActivity(intent);
                finish();
            }
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
                    googleMap.addPolyline(polylineOptions);
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
                    PolylineOptions dashedLineOptions = new PolylineOptions()
                            .add(previousPoint)
                            .add(nextPoint)
                            .color(getResources().getColor(R.color.like_orange))
                            .width(10)
                            .pattern(dashedPattern);
                    googleMap.addPolyline(dashedLineOptions);
                }
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
