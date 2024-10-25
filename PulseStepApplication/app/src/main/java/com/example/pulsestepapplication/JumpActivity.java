package com.example.pulsestepapplication;

import static com.example.pulsestepapplication.R.layout.activity_jump;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class JumpActivity extends AppCompatActivity {
    private static final String TAG = "JumpRopeActivity";
    // private static final int realCount = 0;
    private static final double metValue = 9.0; // around 0.14 - 0.2 /min => 9.0 /Hour


    private String userName;
    private int userAge;
    private double userWeight;
    // UI Components
    private ImageButton btnPauseResume;
    private TextView timerTextView, jumpTextView, avgJumpTextView;
    private ImageView backButton;
    private ImageButton btnShow;

    // Geocoder for address conversion
    private Geocoder geocoder;


    // Step Counter
    private JumpCounter jumpCounter;
    private ImageView jumpImageView;

    // Timer Variables
    private boolean isTracking = false;
    private boolean isPaused = false;
    private boolean isFirstStart = true;
    private float totalJumpCount = 0;
    private int currentJumpCount = 0;

    private double initialLatitude;
    private double initialLongitude;

    private long startTime = 0L;
    private long pauseTime = 0L;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private long elapsedTime;



    private final Runnable timerRunnable = new Runnable() {
        @SuppressLint("DefaultLocale")
        @Override
        public void run() {
            long millis = SystemClock.elapsedRealtime() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds %= 60;
            timerTextView.setText(String.format("%02d:%02d", minutes, seconds));
            elapsedTime = millis;

            // Update average pace
            updateAvgJumpCount();

            timerHandler.postDelayed(this, 1000);
        }
    };

    private TextView cTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(activity_jump);

        // Get the mode from the intent
        Intent intent = getIntent();
        userName = intent.getStringExtra("name");
        userAge = intent.getIntExtra("age", 25);
        userWeight = intent.getDoubleExtra("weight", 70.0);

        // Initialize UI components
        initializeUIComponents();

        // Check permissions
        setupActivity();




//        // Find the back button by its ID
//        ImageView backButton = findViewById(R.id.back_button_jump_page);
//
//        // Set click listener for the back button
//        backButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                // Finish the current activity and return to the RunSummaryActivity page
//                Intent intent = new Intent(JumpActivity.this, JumpSummaryActivity.class);
//                startActivity(intent);
//            }
//        });
    }



    /**
     * Sets up the activity based on the mode.
     */
    private void setupActivity() {
        // Initialize step counter if supported
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            initJumpCounter();
        }

        // Initialize Geocoder
        if (Geocoder.isPresent()) {
            geocoder = new Geocoder(this, Locale.getDefault());
        } else {
            Log.e(TAG, "Geocoder not available on this device.");
        }

        // Get initial latitude and longitude if available
        Intent intent = getIntent();
        initialLatitude = intent.getDoubleExtra("LATITUDE", 0.0);
        initialLongitude = intent.getDoubleExtra("LONGITUDE", 0.0);


        // Set up Jump fragment
        showDefaultBackground();

        // Set up button listeners
        setupButtonListeners();
    }


    /**
     * Initializes the Jump UI components by finding them via their IDs.
     */
    private void initializeUIComponents() {
        btnPauseResume = findViewById(R.id.jump_btn_stop);
        timerTextView = findViewById(R.id.jump_timer_text_view);
        jumpTextView = findViewById(R.id.jump_count_text_view);
        avgJumpTextView = findViewById(R.id.jump_avg_text_view);
        cTextView = findViewById(R.id.jump_calories_text_view);
        backButton = findViewById(R.id.back_button_jump_page);
        btnShow = findViewById(R.id.jump_btn_show);
        jumpImageView = findViewById(R.id.jump_default_image_view);

        // Set click listener for the back button
        backButton.setOnClickListener(v -> popUpConfirmDialog());
        //backButton.setOnClickListener(v -> navigateToMainActivity());

    }


    /**
     * Sets up the button listeners for pause/resume and show actions.
     */
    private void setupButtonListeners() {
        btnPauseResume.setOnClickListener(v -> handlePauseResumeButtonClick());
        btnShow.setOnClickListener(v -> showJumpResult());
    }

    /**
     * Handles the pause/resume button click event.
     */
    private void handlePauseResumeButtonClick() {

        if (isFirstStart) {
            startTracking();
            isFirstStart = false;
        } else if (isPaused) {
            resumeTracking();
        } else {
            pauseTracking();
        }

    }

    /**
     * Starts the tracking process, including location updates and step tracking.
     */
    @SuppressLint({"MissingPermission", "UseCompatLoadingForDrawables"})
    private void startTracking() {
        isTracking = true;
        isPaused = false;
        startTime = SystemClock.elapsedRealtime();
        totalJumpCount = 0;
        timerHandler.postDelayed(timerRunnable, 0);
        btnPauseResume.setImageDrawable(getResources().getDrawable(R.drawable.pause));
        btnShow.setVisibility(View.GONE);

        // Start step tracking
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            jumpCounter.startJumpTracking();
        }
    }

    /**
     * Resumes tracking after a pause.
     */
    @SuppressLint("UseCompatLoadingForDrawables")
    private void resumeTracking() {
        isPaused = false;
        startTime += (SystemClock.elapsedRealtime() - pauseTime);
        timerHandler.postDelayed(timerRunnable, 0);
        btnPauseResume.setImageDrawable(getResources().getDrawable(R.drawable.pause));
        btnShow.setVisibility(View.GONE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            jumpCounter.startJumpTracking();
        }
    }

    /**
     * Pauses the tracking process.
     */
    @SuppressLint("UseCompatLoadingForDrawables")
    private void pauseTracking() {
        isPaused = true;
        pauseTime = SystemClock.elapsedRealtime();
        timerHandler.removeCallbacks(timerRunnable);
        btnPauseResume.setImageDrawable(getResources().getDrawable(R.drawable.start));
        btnShow.setVisibility(View.VISIBLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            jumpCounter.stopJumpTracking();
        }
    }


    private void popUpConfirmDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(JumpActivity.this);
        builder.setTitle("Confirm Exit");
        builder.setMessage("Are you sure you want to exit the jump activity?");
        builder.setCancelable(false);
        builder.setPositiveButton("Yes", (dialog, which) -> {
            // Finish the current activity and return to the RunSummaryActivity page
            Intent intent = new Intent(JumpActivity.this, JumpSummaryActivity.class);
            startActivity(intent);
            finish();
        });
        builder.setNegativeButton("No", (dialog, which) -> dialog.dismiss());
        builder.create().show();

    }



    /**
     * Initializes the StepCounter and sets up its listener.
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void initJumpCounter() {
        jumpCounter = new JumpCounter(this);
        jumpCounter.setJumpCounterListener(jumpCount -> {
            runOnUiThread(() -> {
                currentJumpCount = Math.max(0, jumpCount-1);
                if (currentJumpCount < 1) {
                    jumpTextView.setText("0");
                } else {
                    jumpTextView.setText(String.valueOf(currentJumpCount));
                }
            });
        });
    }

    /**
     * Restores the step counter listener when the activity starts.
     */
    @Override
    protected void onStart() {
        super.onStart();
        if (isTracking && !isPaused && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && jumpCounter != null) {
            jumpCounter.registerListener();
        }
        // Start the foreground service to keep the tracking in background
        Intent serviceIntent = new Intent(this, JumpTrackingService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && jumpCounter != null) {
            jumpCounter.unregisterListener();
        }
    }
    /**
     * Handles tracking state and permission changes when the activity resumes.
     */
    @Override
    protected void onResume() {
        super.onResume();

//        // Handle permission changes
//        handlePermissionChanges();

        if (isTracking && !isPaused) {
            startTracking();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && jumpCounter != null) {
                jumpCounter.registerListener();
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && jumpCounter != null) {
                jumpCounter.unregisterListener();
            }
        }

        btnShow.setVisibility(View.VISIBLE);
    }

    /**
     * Removes location updates and unregisters the step counter when the activity stops.
     */
    @Override
    protected void onStop() {
        super.onStop();

        // Unregister step counter
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && jumpCounter != null) {
            jumpCounter.unregisterListener();
        }

        // Pause the timer if tracking
        if (isTracking) {
            pauseTracking();
        }

//        // stopService(new Intent(this, JumpTrackingService.class));
//        Intent serviceIntent = new Intent(this, JumpTrackingService.class);
//        stopService(serviceIntent);
    }

    /**
     * Cleans up resources when the activity is destroyed.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Remove timer callbacks to prevent memory leaks
        timerHandler.removeCallbacks(timerRunnable);

        // Clean up jump counter
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && jumpCounter != null) {
            jumpCounter.unregisterListener();
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
        outState.putBoolean("isTracking", isTracking);
        outState.putBoolean("isPaused", isPaused);
        outState.putLong("startTime", startTime);
        outState.putLong("pauseTime", pauseTime);
    }

    /**
     * Restores the instance state after configuration changes.
     *
     * @param savedInstanceState The Bundle containing the saved state.
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        isTracking = savedInstanceState.getBoolean("isTracking", false);
        isPaused = savedInstanceState.getBoolean("isPaused", false);
        startTime = savedInstanceState.getLong("startTime", 0L);
        pauseTime = savedInstanceState.getLong("pauseTime", 0L);

        if (isTracking && !isPaused) {
            startTracking();
        }
    }



    /**
     * Displays a default map image when location permission is not granted or in No-map mode.
     */
    private void showDefaultBackground() {
        // Display default image
        jumpImageView.setImageResource(R.drawable.bg_jump);
        jumpImageView.setVisibility(View.VISIBLE);

            // Ensure step count and timer views are visible
        jumpTextView.setVisibility(View.VISIBLE);
        timerTextView.setVisibility(View.VISIBLE);
    }



    /**
     * Shows the speed gif end markers.
     */
    private void showJumpResult() {

        // Display the end markers
        String timeElapsed = timerTextView.getText().toString();

        String avg = avgJumpTextView.getText().toString() + " jumps/min";

        // Get the last location's address
        String address = "Unknown Location";
        if (initialLatitude != 0.0 && initialLongitude != 0.0) {
            LatLng initialLatLng = new LatLng(initialLatitude, initialLongitude);
            address = getAddressFromLatLng(initialLatLng);
        }

        // Create Intent to RunSummaryActivity
        Intent intent = new Intent(JumpActivity.this, JumpSummaryActivity.class);
        intent.putExtra("jumpCount", currentJumpCount);
        intent.putExtra("avgSpeed", avg);
        intent.putExtra("time", timeElapsed);
        intent.putExtra("address", address);
        intent.putExtra("calories", cTextView.getText().toString());

        // Start the RunSummaryActivity
        startActivity(intent);
        finish();
    }

    /**
     * Updates the average pace in No-map mode based on steps and time.
     */
    @SuppressLint("DefaultLocale")
    private void updateAvgJumpCount() {

        double totalTimeMinutes = elapsedTime / (1000.0 * 60.0);
        // if (currentJumpCount > realCount && totalTimeMinutes > 0) {
        if (totalTimeMinutes > 0) {
            double avgJump = currentJumpCount/totalTimeMinutes ;
            double elapsedTimeInMinutes = elapsedTime / 60000.0;

            double caloriesBurned = calculateCalories(userWeight, elapsedTimeInMinutes, metValue);
            cTextView.setText(String.format("%d", Math.round(caloriesBurned)));
            // Update avgPaceTextView
            runOnUiThread(() -> {
                avgJumpTextView.setText(String.format("%.2f", avgJump));
            });
        } else {
            runOnUiThread(() -> {
                avgJumpTextView.setText("0.00");
            });
        }
    }


    // Calculate calories burned based on
    //                                    weight,    durationInMinutes,     MET value
    private double calculateCalories(double weight, double durationInMinutes, double metValue) {

        double durationInHours = durationInMinutes / 60.0;
        return metValue * weight * durationInHours;
    }








    /**
     * Converts a LatLng point to a human-readable address string.
     *
     * @param latLng The LatLng object representing the location.
     * @return A string containing the country and city, or "Unknown Location" if not available.
     */
    private String getAddressFromLatLng(LatLng latLng) {
        String address = "Unknown Location";

        // Ensure Geocoder is initialized
        if (geocoder == null) {
            if (Geocoder.isPresent()) {
                geocoder = new Geocoder(this, Locale.getDefault());
            } else {
                Log.e(TAG, "Geocoder not available.");
                return address;
            }
        }

        try {
            // Get address from latitude and longitude
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);

            if (addresses != null && !addresses.isEmpty()) {
                Address addr = addresses.get(0);
                String country = addr.getCountryName(); // Country
                String city = addr.getLocality();       // City

                if (country != null && city != null) {
                    address = country + ", " + city;
                } else if (country != null) {
                    address = country;
                } else if (city != null) {
                    address = city;
                }
            } else {
                Log.e(TAG, "No address found for the location.");
            }
        } catch (IOException e) {
            Log.e(TAG, "Geocoder IOException: " + e.getMessage());
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid latitude or longitude values.");
            e.printStackTrace();
        }

        return address;
    }


}