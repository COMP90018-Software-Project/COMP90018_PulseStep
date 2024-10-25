package com.example.pulsestepapplication;

import static com.example.pulsestepapplication.R.layout.activity_jump;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class JumpActivity extends AppCompatActivity {
    private static final String TAG = "JumpRopeActivity";
    private static final double metValue = 9.0; // around 0.14 - 0.2 /min => 9.0 /Hour
    private static final int REQUEST_CODE_PERMISSIONS = 1001;

    private String userName;
    private int userAge;
    private double userWeight;
    // UI Components
    private ImageButton btnPauseResume;
    private TextView timerTextView, jumpTextView, avgJumpTextView;
    private ImageView backButton;
    private ImageButton btnShow;

    // Step Counter
    private JumpCounter jumpCounter;
    private ImageView jumpImageView;

    // Geocoder
    private Geocoder geocoder;
    private double initialLatitude;
    private double initialLongitude;


    // Timer Variables
    private boolean isTracking = false;
    private boolean isPaused = false;
    private boolean isFirstStart = true;
    private float totalJumpCount = 0;
    private int currentJumpCount = 0;

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            checkAndRequestPermissions();
        } else {
            setupActivity();
        }
    }

    private void checkAndRequestPermissions() {
        String[] permissions = {
                Manifest.permission.ACTIVITY_RECOGNITION,
                Manifest.permission.FOREGROUND_SERVICE
        };

        boolean allPermissionsGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }

        if (allPermissionsGranted) {
            setupActivity();
        } else {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                setupActivity();
            } else {
                Log.e(TAG, "Required permissions are not granted.");
                finish();
            }
        }
    }

    private void setupActivity() {
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

        showDefaultBackground();
        setupButtonListeners();
    }

    private void initializeUIComponents() {
        btnPauseResume = findViewById(R.id.jump_btn_stop);
        timerTextView = findViewById(R.id.jump_timer_text_view);
        jumpTextView = findViewById(R.id.jump_count_text_view);
        avgJumpTextView = findViewById(R.id.jump_avg_text_view);
        cTextView = findViewById(R.id.jump_calories_text_view);
        backButton = findViewById(R.id.back_button_jump_page);
        btnShow = findViewById(R.id.jump_btn_show);
        jumpImageView = findViewById(R.id.jump_default_image_view);

        backButton.setOnClickListener(v -> popUpConfirmDialog());
    }

    private void setupButtonListeners() {
        btnPauseResume.setOnClickListener(v -> handlePauseResumeButtonClick());
        btnShow.setOnClickListener(v -> showJumpResult());
    }

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

    @SuppressLint({"MissingPermission", "UseCompatLoadingForDrawables"})
    private void startTracking() {
        isTracking = true;
        isPaused = false;
        startTime = SystemClock.elapsedRealtime();
        totalJumpCount = 0;
        timerHandler.postDelayed(timerRunnable, 0);
        btnPauseResume.setImageDrawable(getResources().getDrawable(R.drawable.pause));
        btnShow.setVisibility(View.GONE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            jumpCounter.startJumpTracking();
        }

        // Start foreground service to keep tracking even in background
        Intent serviceIntent = new Intent(this, JumpTrackingService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

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

        // Stop the foreground service when paused
        Intent serviceIntent = new Intent(this, JumpTrackingService.class);
        stopService(serviceIntent);
    }

    private void popUpConfirmDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(JumpActivity.this);
        builder.setTitle("Confirm Exit");
        builder.setMessage("Are you sure you want to exit the jump activity?");
        builder.setCancelable(false);
        builder.setPositiveButton("Yes", (dialog, which) -> {
            Intent intent = new Intent(JumpActivity.this, JumpSummaryActivity.class);
            startActivity(intent);
            finish();
        });
        builder.setNegativeButton("No", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void initJumpCounter() {
        jumpCounter = new JumpCounter(this);
        jumpCounter.setJumpCounterListener(jumpCount -> {
            runOnUiThread(() -> {
                currentJumpCount = Math.max(0, jumpCount - 1);
                jumpTextView.setText(String.valueOf(currentJumpCount));
            });
        });
    }

    private void showDefaultBackground() {
        jumpImageView.setImageResource(R.drawable.bg_jump);
        jumpImageView.setVisibility(View.VISIBLE);
        jumpTextView.setVisibility(View.VISIBLE);
        timerTextView.setVisibility(View.VISIBLE);
    }

    private void showJumpResult() {
        String timeElapsed = timerTextView.getText().toString();
        String avg = avgJumpTextView.getText().toString() + " jumps/min";

        String address = "Unknown Location";
        if (initialLatitude != 0.0 && initialLongitude != 0.0) {
            LatLng initialLatLng = new LatLng(initialLatitude, initialLongitude);
            address = getAddressFromLatLng(initialLatLng);
        }

        Intent intent = new Intent(JumpActivity.this, JumpSummaryActivity.class);
        intent.putExtra("jumpCount", currentJumpCount);
        intent.putExtra("avgSpeed", avg);
        intent.putExtra("time", timeElapsed);
        intent.putExtra("address", address);
        intent.putExtra("calories", cTextView.getText().toString());

        startActivity(intent);
        finish();
    }

    @SuppressLint("DefaultLocale")
    private void updateAvgJumpCount() {
        double totalTimeMinutes = elapsedTime / (1000.0 * 60.0);
        if (totalTimeMinutes > 0) {
            double avgJump = currentJumpCount / totalTimeMinutes;
            double elapsedTimeInMinutes = elapsedTime / 60000.0;

            double caloriesBurned = calculateCalories(userWeight, elapsedTimeInMinutes, metValue);
            cTextView.setText(String.format("%d", Math.round(caloriesBurned)));
            runOnUiThread(() -> avgJumpTextView.setText(String.format("%.2f", avgJump)));
        } else {
            runOnUiThread(() -> avgJumpTextView.setText("0.00"));
        }
    }

    private double calculateCalories(double weight, double durationInMinutes, double metValue) {
        double durationInHours = durationInMinutes / 60.0;
        return metValue * weight * durationInHours;
    }

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
