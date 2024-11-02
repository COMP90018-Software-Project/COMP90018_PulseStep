package com.example.pulsestepapplication;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

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
    private TextView caloriesTextView;

    // Tracking Data
    private float distanceInKm; // in kilometers
    private float totalDistance; // in kilometers
    private String time; // formatted as "MM:SS"
    private String address; // optional
    private int stepCount;
    private ArrayList<LatLng> trajectory;
    private CardView mapCard;
    private String avgPace;
    private TextView avgPaceTextView;
    private Button finishButton;
    private String calories;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_run_summary);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize UI components
        initializeUIComponents();

        // Retrieve data from Intent
        retrieveIntentData();

        // Display data
        displayData();

        // Initialize and set up the map
        setupMap(savedInstanceState);

        // Get user UID
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String userUID = currentUser.getUid();

        // Get passed startDateTime and finishDateTime
        String startDateTime = getIntent().getStringExtra("startDateTime");
        String finishDateTime = getIntent().getStringExtra("finishDateTime");
        Log.d("RunSummary", "startDateTime: " + startDateTime);
        Log.d("RunSummary", "finishDateTime: " + finishDateTime);

        finishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Map<String, Object> userRunningDetails = new HashMap<>();

                userRunningDetails.put("userId", userUID);
                userRunningDetails.put("startDateTime", startDateTime);
                userRunningDetails.put("finishDateTime", finishDateTime);
                userRunningDetails.put("activeTime", time);
                userRunningDetails.put("avgPace", avgPace);
                userRunningDetails.put("distanceInKm", distanceInKm);
                userRunningDetails.put("calories", calories);
                userRunningDetails.put("stepCount", stepCount);
                userRunningDetails.put("location", address);


                // Save data to Firestore
                db.collection("run").add(userRunningDetails)
                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {
                                Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                                updateUserActiveTime(userUID, time);
                                updateUserDailyRunningInfo(userUID, time, totalDistance, Double.parseDouble(calories));
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG, "Error adding document", e);
                            }
                        });


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
        caloriesTextView = findViewById(R.id.run_summary_calories);
        mapCard = findViewById(R.id.map_container);
        finishButton = findViewById(R.id.bt_finish_run);
    }

    /**
     * Retrieves data passed from the tracking activity via Intent.
     */
    private void retrieveIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            distanceInKm = intent.getFloatExtra("distanceInKm", 0.0f);
            totalDistance = intent.getFloatExtra("totalDistance", 0.0f);
            time = intent.getStringExtra("time");
            stepCount = intent.getIntExtra("stepCount", 0);
            String mode = intent.getStringExtra("MODE");
            if(distanceInKm > 0.01 && Objects.equals(mode, "MAP")){
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
        distanceTextView.setText(String.format("%.2f", distanceInKm));
        timeTextView.setText(time != null ? time : "00:00");
        stepCountTextView.setText(String.valueOf(stepCount));
        addressTextView.setText(address != null ? address : "N/A");
        avgPaceTextView.setText(avgPace);
        caloriesTextView.setText(calories);

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
                    if (trajectory.indexOf(nextPoint) != trajectory.size() - 1) {
                        PolylineOptions dashedLineOptions = new PolylineOptions()
                                .add(previousPoint)
                                .add(nextPoint)
                                .color(getResources().getColor(R.color.like_orange))
                                .width(10)
                                .pattern(dashedPattern);
                        googleMap.addPolyline(dashedLineOptions);
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

    /**
     * Update the user daily and monthly active time in users database.
     */
    private void updateUserActiveTime(String userId, String activeTime) {
        // Convert active time from string MM:SS to seconds.
        int timeInSeconds = convertTimeToSeconds(activeTime);

        // Retrieve current date and month
        String today = getCurrentDate();
        String currentMonth = getCurrentMonth();

        //  Update dailyActive and monthlyActive in users
        DocumentReference userRef = db.collection("users").document(userId);
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // Get current dailyActive and monthlyActive data
                Map<String, Long> dailyActive = (Map<String, Long>) documentSnapshot.get("dailyActive");
                Map<String, Long> monthlyActive = (Map<String, Long>) documentSnapshot.get("monthlyActive");

                // Initialize it if dailyActive or monthlyActive not exist
                if (dailyActive == null) {
                    dailyActive = new HashMap<>();
                }
                if (monthlyActive == null) {
                    monthlyActive = new HashMap<>();
                }

                // Calculate new active time
                long updatedDailyActive = dailyActive.containsKey(today) ? dailyActive.get(today) + timeInSeconds : timeInSeconds;
                long updatedMonthlyActive = monthlyActive.containsKey(currentMonth) ? monthlyActive.get(currentMonth) + timeInSeconds : timeInSeconds;

                // Update Firestore data
                Map<String, Object> updates = new HashMap<>();
                updates.put("dailyActive." + today, updatedDailyActive);
                updates.put("monthlyActive." + currentMonth, updatedMonthlyActive);

                userRef.update(updates).addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User active time updated successfully.");
                }).addOnFailureListener(e -> {
                    Log.w(TAG, "Error updating user active time", e);
                });
            } else {
                // If user not exist
                Log.w(TAG, "User document does not exist.");
            }
        }).addOnFailureListener(e -> {
            Log.w(TAG, "Error fetching user document", e);
        });
    }


    private void updateUserDailyRunningInfo(String userId, String activeTime, float distance, double calories) {
        // Convert active time from string MM:SS to seconds.
        int timeInSeconds = convertTimeToSeconds(activeTime);

        // Retrieve current date
        String today = getCurrentDate();

        // Update dailyActive and monthlyActive in users
        DocumentReference userRef = db.collection("users").document(userId);
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // Get current dailyRunningInfo data
                Map<String, Map<String, Object>> dailyRunningInfo = (Map<String, Map<String, Object>>) documentSnapshot.get("dailyRunningInfo");

                // Initialize if not exist
                if (dailyRunningInfo == null) {
                    dailyRunningInfo = new HashMap<>();
                }

                // Calculate new active time
                long updatedDailyActiveTime = dailyRunningInfo.containsKey(today) && dailyRunningInfo.get(today).get("activeTime") instanceof Long
                        ? (Long) dailyRunningInfo.get(today).get("activeTime") + timeInSeconds
                        : timeInSeconds;

                // Calculate new distance, handle the conversion from Double to Float
                float updatedDailyDistance = dailyRunningInfo.containsKey(today) && dailyRunningInfo.get(today).get("distance") instanceof Double
                        ? ((Double) dailyRunningInfo.get(today).get("distance")).floatValue() + distance
                        : distance;

                // Calculate new calories, handle the conversion from Double to Float if needed
                double updatedDailyCalories = dailyRunningInfo.containsKey(today) && dailyRunningInfo.get(today).get("calories") instanceof Double
                        ? (Double) dailyRunningInfo.get(today).get("calories") + calories
                        : calories;

                // Create or update daily activity entry with time, distance, and calories
                Map<String, Object> dailyData = new HashMap<>();
                dailyData.put("activeTime", updatedDailyActiveTime);
                dailyData.put("distance", updatedDailyDistance);
                dailyData.put("calories", updatedDailyCalories);

                // Update Firestore data
                Map<String, Object> updates = new HashMap<>();
                updates.put("dailyRunningInfo." + today, dailyData);

                userRef.update(updates).addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User daily running info updated successfully.");
                }).addOnFailureListener(e -> {
                    Log.w(TAG, "Error updating user daily running info", e);
                });
            } else {
                // If user does not exist
                Log.w(TAG, "User document does not exist.");
            }
        }).addOnFailureListener(e -> {
            Log.w(TAG, "Error fetching user document", e);
        });
    }


    /**
     * Convert active time from string MM:SS to seconds.
     */
    private int convertTimeToSeconds(String time) {
        if (time != null && !time.isEmpty()) {
            String[] parts = time.split(":");
            int minutes = Integer.parseInt(parts[0]);
            int seconds = Integer.parseInt(parts[1]);
            return minutes * 60 + seconds;
        } else {
            return 0;
        }
    }

    /**
     * Method used to get current date in the format: YYYY-MM-DD
     */
    private String getCurrentDate() {
        Date date = Calendar.getInstance().getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return dateFormat.format(date);
    }

    /**
     * Method used to get current month in the format: YYYY-MM
     */
    private String getCurrentMonth() {
        Date date = Calendar.getInstance().getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        return dateFormat.format(date);
    }


}
