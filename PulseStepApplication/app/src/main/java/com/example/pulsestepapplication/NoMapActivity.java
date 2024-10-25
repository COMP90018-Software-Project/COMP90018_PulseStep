package com.example.pulsestepapplication;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import pl.droidsonroids.gif.GifDrawable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NoMapActivity extends AppCompatActivity {
    // Constants
    private static final String TAG = "NoMapActivity";
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final int BACKGROUND_LOCATION_REQUEST_CODE = 1002;
    private static final float MOVE_ZOOM_LEVEL = 17f;
    private static final float DEFAULT_ZOOM_LEVEL = 15f;
    private static final float MAX_ZOOM_LEVEL = 19f;
    private static final float DISTANCE_THRESHOLD_METERS = 1.0f; // Distance threshold in meters

    // UI Components
    private ImageButton btnPauseResume;
    private TextView timerTextView, stepTextView, avgPaceTextView;
    private ImageButton btnShow;
    private ImageView backButton;
    private ImageView runImageView;
    private TextView cTextView;
    private ImageView waitView;
    private TextView waitTextView;
    private ImageView muteMusicView;

    // Map and Location
    private GoogleMap googleMap;
    private double initialLatitude;
    private double initialLongitude;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    // Tracking Variables
    private final List<Polyline> polyLines = new ArrayList<>();
    private final List<LatLng> pathPoints = new ArrayList<>();
    private final List<LatLng> trajectory = new ArrayList<>();
    private boolean isTracking = false;
    private boolean isPaused = false;
    private boolean isFirstStart = true;
    private boolean isLocationReady = false;
    private float totalDistance = 0.0f;
    private int currentStepCount = 0;
    private static final Double realDistance = 0.01;
    private static final double metValue = 8.0;
    private static final int LOCATION_TIMEOUT = 10000; // Location timeout in milliseconds

    //Button
    private boolean isLongPress = false;
    private Handler handler = new Handler();
    private ProgressBar progressBar;
    private int progressStatus = 0;
    private boolean isRunning = false;
    private boolean hasTriggeredSuccess = false;
    // Timer Variables
    private long startTime = 0L;
    private long pauseTime = 0L;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private long elapsedTime;
    private boolean startRealTimeUpdate=false;
    private volatile boolean isStepCountingActive = false;
    private Handler stepCountHandler = new Handler(Looper.getMainLooper());
    private Runnable stepCountTimeoutRunnable;
    private static final long STEP_COUNT_TIMEOUT = 2000; // 2 seconds
    private GifDrawable gifDrawable;

    // music player
    private MusicPlayer musicPlayer;
    private boolean shouldLoop = true;

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

            if (!isStepCountingActive) {
                updateAvgPaceNoMapMode();
            }
            timerHandler.postDelayed(this, 1000);
        }
    };


    // Mode flag: true for Map mode, false for No-map mode
    private boolean isMapMode;
    private boolean isServiceRunning = false;

    private String userName;
    private int userAge;
    private double userWeight;
    private Geocoder geocoder;
    private double avgPace;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_no_map);

        // Get the mode from the intent
        Intent intent = getIntent();
        isMapMode = intent.getBooleanExtra("MAP_MODE", true); // default to Map mode
        userName = intent.getStringExtra("name");
        userAge = intent.getIntExtra("age", 25);
        userWeight = intent.getDoubleExtra("weight", 70.0);

        // Initialize UI components
        initializeUIComponents();

        // Check permissions
        checkPermissions();
    }

    /**
     * Checks and requests necessary permissions.
     */
    private void checkPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACTIVITY_RECOGNITION);
        }
        if (isMapMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }
        // Add check for FOREGROUND_SERVICE_LOCATION permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.FOREGROUND_SERVICE_LOCATION);
            }
        }
        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        } else {
            // Permissions are granted, proceed with setup
            setupActivity();
        }
    }

    /**
     * Sets up the activity based on the selected mode.
     */
    private void setupActivity() {
        // Initialize location services if in Map mode
        //showDefaultMap();

        // Set up button listeners
        setupButtonListeners();

        // Start the tracking service
        startTrackingService();
    }

    /**
     * Initializes the UI components by finding them via their IDs.
     */
    private void initializeUIComponents() {
        btnPauseResume = findViewById(R.id.btn_stop);
        btnShow = findViewById(R.id.btn_show);
        timerTextView = findViewById(R.id.timer_text_view);
        stepTextView = findViewById(R.id.step_text_view);
        avgPaceTextView = findViewById(R.id.avg_text_view);
        cTextView = findViewById(R.id.calories_text_view);
        backButton = findViewById(R.id.back_button_running_page);
        runImageView = findViewById(R.id.default_image_view);
        waitView = findViewById(R.id.wait);
        waitTextView = findViewById(R.id.waitText);
        muteMusicView = findViewById(R.id.music_control);

        // Initialize MusicPlayer with audio resource
        musicPlayer = new MusicPlayer(this, R.raw.pulsestep_fever);

        // Enable or disable looping based on user input
        musicPlayer.setLooping(shouldLoop);

        // Set up mute button click listener
        muteMusicView.setOnClickListener(v -> {
            if (musicPlayer.isMuted()) {
                muteMusicView.setImageResource(R.drawable.ic_music_launcher);
                musicPlayer.unmute();
            } else {
                muteMusicView.setImageResource(R.drawable.ic_mute_music);
                musicPlayer.mute();
            }
        });
        try {
            gifDrawable = new GifDrawable(getResources(), R.drawable.run);
            runImageView.setImageDrawable(gifDrawable);
            gifDrawable.stop(); // Start the GIF animation
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Hide the map initially
        // Set click listener for the back button
        backButton.setOnClickListener(v -> popUpConfirmDialog());
        btnPauseResume.setClickable(true);
    }


    /**
     * Navigates back to the MainActivity.
     */
    private void navigateToMainActivity() {
        Intent intent = new Intent(NoMapActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }


    /**
     * Sets up the button listeners for pause/resume and show actions.
     */
    @SuppressLint("ClickableViewAccessibility")
    private void setupButtonListeners() {
        progressBar = findViewById(R.id.progressBar);
        btnPauseResume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (musicPlayer.isPlaying()) {
                    Log.d(TAG, "Pausing music...");
                    musicPlayer.pause();
                } else {
                    Log.d(TAG, "Playing music...");
                    musicPlayer.play();
                }
                handleStartStopButtonClick();
            }
        });
        CircularProgressDrawable circularProgressDrawable = new CircularProgressDrawable(this);
        circularProgressDrawable.setColor(ContextCompat.getColor(this, R.color.light_orange));
        progressBar.setProgressDrawable(circularProgressDrawable);
        btnShow.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (isRunning || hasTriggeredSuccess) {
                        return true;
                    }
                    isRunning = true;


                    progressBar.setVisibility(View.VISIBLE);
                    progressStatus = 0;
                    circularProgressDrawable.setProgress(progressStatus);

                    if (!hasTriggeredSuccess) {
                        ObjectAnimator scaleXDown = ObjectAnimator.ofFloat(btnShow, "scaleX", 1f, 1.3f);
                        ObjectAnimator scaleYDown = ObjectAnimator.ofFloat(btnShow, "scaleY", 1f, 1.3f);
                        AnimatorSet animatorSetDown = new AnimatorSet();
                        animatorSetDown.playTogether(scaleXDown, scaleYDown);
                        animatorSetDown.setDuration(2000);
                        animatorSetDown.start();
                    }

                    new Thread(new Runnable() {
                        public void run() {
                            while (progressStatus < 100 && isRunning) {
                                progressStatus += 1;
                                handler.post(new Runnable() {
                                    public void run() {
                                        circularProgressDrawable.setProgress(progressStatus);
                                    }
                                });
                                try {
                                    Thread.sleep(20);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }

                            if (progressStatus >= 100 && isRunning) {
                                isLongPress = true;
                                handler.post(() -> {
                                    showLastTrack();
                                    isRunning = false;
                                    hasTriggeredSuccess = true;
                                });
                            }
                        }
                    }).start();
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (!hasTriggeredSuccess) {
                        ObjectAnimator scaleXUp = ObjectAnimator.ofFloat(btnShow, "scaleX", 1.3f, 1f);
                        ObjectAnimator scaleYUp = ObjectAnimator.ofFloat(btnShow, "scaleY", 1.3f, 1f);
                        AnimatorSet animatorSetUp = new AnimatorSet();
                        animatorSetUp.playTogether(scaleXUp, scaleYUp);
                        animatorSetUp.setDuration(300);
                        animatorSetUp.start();
                    }
                    isRunning = false;
                    if (progressStatus < 100) {
                        progressBar.setVisibility(View.GONE);
                        handler.removeCallbacksAndMessages(null);
                        progressStatus = 0;
                    }
                    break;
            }
            return true;
        });

    }

    /**
     * Handles the start/stop button click event.
     */
    private void handleStartStopButtonClick() {
        if (isTracking) {
            pauseTracking();
        } else {
            resumeTracking();
        }
    }

    /**
     * Load a GIF from local resources, adjust its playback speed based on the average pace,
     * and set it to the provided ImageView.
     *
     * @param avgPace The average pace to determine the speed factor.
     */
    private void setGifWithSpeed(double avgPace) {
        if (gifDrawable != null) {
            float speedFactor = getGifSpeedFactor(avgPace);
            Log.d(TAG, "Setting GIF speed with avgPace: " + avgPace + ", speedFactor: " + speedFactor);
            gifDrawable.setSpeed(speedFactor);
            if (!gifDrawable.isRunning()) {
                gifDrawable.start();
            }
        } else {
            Log.e(TAG, "gifDrawable is null in setGifWithSpeed()");
        }
    }

    /**
     * Get speed factor for GIF animation based on average pace.
     *
     * @param avgPace The average pace in minutes per kilometer.
     * @return Speed factor (e.g., 1.0 for normal, < 1.0 for faster, > 1.0 for slower).
     */
    private float getGifSpeedFactor(double avgPace) {
        if (avgPace <= 0) {
            return 1.0f; // Default speed
        }
        Log.d("factor", String.valueOf(avgPace));
        if (avgPace < 18) { // Fast pace (running)
            return 2.0f; // Increase speed for fast running
        } else if (avgPace < 20 && avgPace > 18) { // Medium pace (jogging)
            return 1.5f; // Normal speed for jogging
        } else { // Slow pace (walking)
            return 1.0f; // Slow down for walking
        }}


    /**
     * Starts the tracking process.
     */
    @SuppressLint({"MissingPermission", "UseCompatLoadingForDrawables"})
    private void resumeTracking() {
        isTracking = true;
        isPaused = false;
        if (isFirstStart) {
            pathPoints.clear();
            totalDistance = 0.0f;
            startTime = SystemClock.elapsedRealtime();
            timerHandler.postDelayed(timerRunnable, 0);
            isFirstStart = false;
        } else {
            // Adjust startTime to account for pause duration
            long pauseDuration = SystemClock.elapsedRealtime() - pauseTime;
            startTime += pauseDuration;
            timerHandler.postDelayed(timerRunnable, 0);
        }
        btnPauseResume.setImageDrawable(getResources().getDrawable(R.drawable.pause));
        btnShow.setVisibility(View.GONE);

        // Start the GIF at default speed
        if (gifDrawable != null) {
            gifDrawable.setSpeed(1.0f); // Default speed
            gifDrawable.start();
        }

        // Send broadcast to service to resume step counting
        Intent resumeIntent = new Intent(StepTrackingService.ACTION_RESUME_STEP_COUNTING);
        LocalBroadcastManager.getInstance(this).sendBroadcast(resumeIntent);
    }


    /**
     * Pauses the tracking process.
     */
    private void pauseTracking() {
        isTracking = false;
        isPaused = true;
        pauseTime = SystemClock.elapsedRealtime();
        timerHandler.removeCallbacks(timerRunnable);
        btnPauseResume.setImageDrawable(getResources().getDrawable(R.drawable.start));
        btnShow.setVisibility(View.VISIBLE);
        // Stop the GIF
        if (gifDrawable != null) {
            gifDrawable.stop();
        }
        // Send broadcast to service to pause step counting
        Intent pauseIntent = new Intent(StepTrackingService.ACTION_PAUSE_STEP_COUNTING);
        LocalBroadcastManager.getInstance(this).sendBroadcast(pauseIntent);
    }

    /**
     * Starts the tracking service.
     */
    private void startTrackingService() {
        Intent serviceIntent = new Intent(this, StepTrackingService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
        isServiceRunning = true;
    }

    /**
     * Stops the tracking service.
     */
    private void stopTrackingService() {
        Intent serviceIntent = new Intent(this, StepTrackingService.class);
        stopService(serviceIntent);
        isServiceRunning = false;
    }

    /**
     * Displays a default map image when location permission is not granted or in No-map mode.
     */
    private void showDefaultMap() {
        // Display default image
        // Switch to run PNG
        Glide.with(this)
                .asBitmap()  // Load the first frame of the GIF as a static image
                .load(R.drawable.run)
                .into(runImageView);
        runImageView.setVisibility(View.VISIBLE);
    }


    private void updatePath(int stepCount) {
        if (!isTracking || isPaused) {
            return;
        }
        isStepCountingActive = true;

        // Reset the timeout handler
        stepCountHandler.removeCallbacks(stepCountTimeoutRunnable);
        stepCountTimeoutRunnable = () -> isStepCountingActive = false;
        stepCountHandler.postDelayed(stepCountTimeoutRunnable, STEP_COUNT_TIMEOUT);
        // Assume an average step length in meters
        float averageStepLength = 0.75f;  // Modify step length based on personal data, in meters
        float distanceInMeters = stepCount * averageStepLength;  // Calculate total walking distance, in meters
        double totalDistanceKm = distanceInMeters / 1000.0;  // Convert to kilometers
        double totalTimeMinutes = elapsedTime / (1000.0 * 60.0);  // Total time, in minutes

        // Ensure distance and time are valid before calculating pace
        if (totalDistanceKm >= realDistance && totalTimeMinutes > 0) {
            avgPace = totalTimeMinutes / totalDistanceKm;  // Calculate average pace, in minutes/kilometer
            double elapsedTimeInMinutes = elapsedTime / 60000.0;  // Convert to minutes
            double caloriesBurned = calculateCalories(userWeight, elapsedTimeInMinutes, avgPace);  // Calculate calories burned
            runOnUiThread(() -> cTextView.setText(String.format("%d", Math.round(caloriesBurned))));
            setGifWithSpeed(avgPace);
            // Check if the calculated pace is within a reasonable range
            if (avgPace >= 1.0 && avgPace <= 30.0) {
                runOnUiThread(() -> avgPaceTextView.setText(String.format("%d'%02d\"", (int) avgPace, (int) ((avgPace * 60) % 60))));
            } else {
                runOnUiThread(() -> avgPaceTextView.setText("--'--\""));
            }
        } else {
            runOnUiThread(() -> avgPaceTextView.setText("--'--\""));
        }
    }

    /**
     * Updates the average pace in No-map mode based on steps and time.
     */
    @SuppressLint("DefaultLocale")
    private void updateAvgPaceNoMapMode() {
        if (isStepCountingActive) {
            // Skip updating since step count updates are active
            return;
        }
        // Assume average step length in meters
        float averageStepLength = 0.75f;
        float distance = currentStepCount * averageStepLength; // in meters
        double distanceKm = distance / 1000.0;
        double totalTimeMinutes = elapsedTime / (1000.0 * 60.0);
        if (distanceKm >= realDistance && totalTimeMinutes > 0) {
            avgPace = totalTimeMinutes / distanceKm;  // Calculate average pace, in minutes/kilometer
            double elapsedTimeInMinutes = elapsedTime / 60000.0;  // Convert to minutes
            double caloriesBurned = calculateCalories(userWeight, elapsedTimeInMinutes, avgPace);  // Calculate calories burned
            runOnUiThread(() -> cTextView.setText(String.format("%d", Math.round(caloriesBurned))));
            setGifWithSpeed(avgPace);
            // Check if the calculated pace is within a reasonable range
            if (avgPace >= 1.0 && avgPace <= 30.0) {
                runOnUiThread(() -> avgPaceTextView.setText(String.format("%d'%02d\"", (int) avgPace, (int) ((avgPace * 60) % 60))));
            } else {
                runOnUiThread(() -> avgPaceTextView.setText("--'--\""));
            }
        } else {
            runOnUiThread(() -> avgPaceTextView.setText("--'--\""));
        }
    }

    /**
     * Calculate MET value based on average pace.
     *
     * @param avgPace The average pace in minutes per kilometer.
     * @return Adjusted MET value.
     */
    private double getDynamicMetValue(double avgPace) {
        // Example: Adjust MET value based on avgPace (higher pace reduces metValue)
        if (avgPace < 6 && avgPace > 0) { // Fast pace (running)
            return 10.0; // Higher MET for running
        } else if (avgPace < 9 && avgPace > 6) { // Medium pace (jogging)
            return 8.0; // Medium MET for jogging
        } else { // Slow pace (walking)
            return 4.0; // Lower MET for walking
        }
    }

    /**
     * Calculate the calories burned based on weight, duration, and dynamic MET value
     *
     * @param weight            User weight in kilograms
     * @param durationInMinutes Duration of activity in minutes
     * @param avgPace           The average pace in minutes per kilometer.
     * @return Calories burned
     */
    private double calculateCalories(double weight, double durationInMinutes, double avgPace) {
        double metValue = getDynamicMetValue(avgPace);
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

    /**
     * Shows the last tracked path on the map with start and end markers.
     */
    private void showLastTrack() {
        // Calculate distance
        float distanceInKm = 0.0f;
        // In No-map mode, calculate distance based on steps
        float averageStepLength = 0.75f;
        float distance = currentStepCount * averageStepLength; // in meters

        distanceInKm = distance / 1000.0f;
        String timeElapsed = timerTextView.getText().toString();
        int stepCount = currentStepCount;

        double totalTimeMinutes = elapsedTime / (1000.0 * 60.0);
        String avg = (distanceInKm <= 0.01) ? "--'--''" : String.valueOf(totalTimeMinutes / distanceInKm);

        // Get the last location's address
        String address = "Unknown Location";
        if (isMapMode && initialLatitude != 0.0 && initialLongitude != 0.0) {
            LatLng initialLatLng = new LatLng(initialLatitude, initialLongitude);
            address = getAddressFromLatLng(initialLatLng);
        }
        // Create Intent to RunSummaryActivity
        Intent intent = new Intent(NoMapActivity.this, RunSummaryActivity.class);
        intent.putExtra("distanceInKm", distanceInKm);
        intent.putExtra("totalDistance", distanceInKm);
        intent.putExtra("avgPace", avg);
        intent.putExtra("time", timeElapsed);
        intent.putExtra("address", address);
        intent.putExtra("stepCount", stepCount);
        intent.putExtra("calories", cTextView.getText().toString());
        intent.putExtra("MODE", "NO_MAP");


        startActivity(intent);
        finish();
    }

    /**
     * Handles permission changes when the activity resumes.
     */
    private void handlePermissionChanges() {
        // Check for activity recognition permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                // Permission revoked, navigate back to MainActivity
                Toast.makeText(this, "Activity recognition permission is required", Toast.LENGTH_SHORT).show();
                navigateToWorkoutPage();
            }
        }
    }

    /**
     * Navigates back to the WorkoutActivity.
     */
    private void navigateToWorkoutPage() {
        Intent returnIntent = new Intent();
        setResult(Activity.RESULT_OK, returnIntent);
        finish();
    }

    /**
     * Handles tracking state and permission changes when the activity resumes.
     */
    @Override
    protected void onResume() {
        super.onResume();

        // Handle permission changes
        handlePermissionChanges();

        // Register BroadcastReceiver
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.example.pulsestepapplication.STEP_UPDATE");
        LocalBroadcastManager.getInstance(this).registerReceiver(trackingReceiver, filter);

        if (isTracking && !isPaused) {
            timerHandler.postDelayed(timerRunnable, 0);
            btnShow.setVisibility(View.GONE);
        } else {
            btnShow.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Unregisters the BroadcastReceiver when the activity is paused.
     */
    @Override
    protected void onPause() {
        super.onPause();
        // Unregister BroadcastReceiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(trackingReceiver);
    }

    /**
     * Removes timer callbacks when the activity is stopped.
     */
    @Override
    protected void onStop() {
        super.onStop();
        // Pause the timer if tracking
        if (isTracking) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }

    /**
     * Handles the result of permission requests.
     *
     * @param requestCode  The request code passed in requestPermissions().
     * @param permissions  The requested permissions.
     * @param grantResults The grant results for the corresponding permissions.
     */
    @SuppressLint("NewApi")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == BACKGROUND_LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Background location permission granted
                startTrackingService();
            } else {
                // Permission denied, exit to workout page
                Toast.makeText(this, "Background location permission is required", Toast.LENGTH_SHORT).show();
                navigateToWorkoutPage();
            }
        } else if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                allGranted &= (result == PackageManager.PERMISSION_GRANTED);
            }
            if (allGranted) {
                // Permissions granted
                setupActivity();
            } else {
                // Permissions denied, exit to workout page
                Toast.makeText(this, "All permissions are required", Toast.LENGTH_SHORT).show();
                navigateToWorkoutPage();
            }
        }
    }

    /**
     * Shows a confirmation dialog to exit the current running activity.
     * - "Yes" will finish the activity and navigate to the WorkoutFragment.
     * - "No" will close the dialog without exiting.
     */
    private void popUpConfirmDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_custom, null);
        builder.setView(dialogView);
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();

        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams layoutParams = window.getAttributes();
            layoutParams.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.8);

            int offsetInDp = 100;
            float scale = getResources().getDisplayMetrics().density;
            layoutParams.y = (int) (offsetInDp * scale + 0.5f);
            layoutParams.dimAmount = 0.9f;
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

            window.setAttributes(layoutParams);
        }

        Button positiveButton = dialogView.findViewById(R.id.positive_button);
        Button negativeButton = dialogView.findViewById(R.id.negative_button);

        positiveButton.setOnClickListener(v -> {
            Intent returnIntent = new Intent();
            setResult(Activity.RESULT_OK, returnIntent);
            finish();
            dialog.dismiss();
        });

        negativeButton.setOnClickListener(v -> dialog.dismiss());
    }

    /**
     * BroadcastReceiver to receive location and step updates from the service.
     */
    private BroadcastReceiver trackingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.example.pulsestepapplication.STEP_UPDATE".equals(intent.getAction())) {
                int stepCount = intent.getIntExtra("stepCount", 0);
                Log.d(TAG, "Received step count update: " + stepCount);

                updatePath(stepCount);
                updateStepCount(stepCount);
                startRealTimeUpdate=false;
            }
        }
    };

    /**
     * Updates the step count on the UI.
     *
     * @param stepCount The current step count.
     */
    private void updateStepCount(int stepCount) {
        runOnUiThread(() -> {
            if (stepCount < 5) {
                stepTextView.setText("--");
            } else {
                stepTextView.setText(String.valueOf(stepCount));
            }
            currentStepCount = stepCount;
        });
    }

    /**
     * Called when the back button is pressed
     */
    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        popUpConfirmDialog();
    }

    /**
     * Stops the service when the activity is destroyed.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        musicPlayer.release(); // Release resources when activity is destroyed
        stopTrackingService();
        if (gifDrawable != null) {
            gifDrawable.recycle();
            gifDrawable = null;
        }
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        timerHandler.removeCallbacks(timerRunnable);
    }
}
