package com.example.pulsestepapplication;

import static com.example.pulsestepapplication.R.layout.activity_jump;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
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
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.bumptech.glide.Glide;


public class JumpActivity extends AppCompatActivity {
    private static final String TAG = "JumpRopeActivity";
    // private static final int realCount = 0;
    private static final double metValue = 8.0;


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

    private String formattedStartTime;
    private String formattedFinishTime;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    //Button
    private boolean isLongPress = false;
    private Handler handler = new Handler();
    private ProgressBar progressBar;
    private int progressStatus = 0;
    private boolean isRunning = false;
    private boolean hasTriggeredSuccess = false;

    private final String popUpMessage = "Are you sure you want to leave the jump rope session? Your progress will not be saved.";

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
    @SuppressLint("ClickableViewAccessibility")
    private void setupButtonListeners() {
        progressBar = findViewById(R.id.progressBar);
        btnPauseResume.setOnClickListener(v -> handlePauseResumeButtonClick());
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
                                    showJumpResult();
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
     * Handles the pause/resume button click event.
     */
    private void handlePauseResumeButtonClick() {

        if (isFirstStart) {
            // dynamic adjust image size
            // updateImageViewForMedia(true);
            // Switch to jump gif
            Glide.with(this)
                    .asGif()  // Ensure Glide knows to handle this as a GIF
                    .load("https://i.giphy.com/media/v1.Y2lkPTc5MGI3NjExdWIxMm1" +
                            "xcDl4dGVldGkybnlkN3F0ZG15OWdkZWJ1d2FlMzZyamdqbyZlcD12MV9pbnR" +
                            "lcm5hbF9naWZfYnlfaWQmY3Q9cw/Exf7ID0UMEYp3Gx0Zf/giphy.gif")  // Use direct GIF link
                    .diskCacheStrategy(DiskCacheStrategy.ALL)  // Cache for better performance
                    .into(jumpImageView);  // Load into your ImageView

            long currentTime = System.currentTimeMillis();
            formattedStartTime = dateFormat.format(new Date(currentTime));
            startTracking();
            isFirstStart = false;
        } else if (isPaused) {
            // updateImageViewForMedia(true);
            // Switch to jump gif
            Glide.with(this)
                    .asGif()  // Ensure Glide knows to handle this as a GIF
                    .load("https://i.giphy.com/media/v1.Y2lkPTc5MGI3NjExdWIxMm1" +
                            "xcDl4dGVldGkybnlkN3F0ZG15OWdkZWJ1d2FlMzZyamdqbyZlcD12MV9pbnR" +
                            "lcm5hbF9naWZfYnlfaWQmY3Q9cw/Exf7ID0UMEYp3Gx0Zf/giphy.gif")  // Use direct GIF link
                    .diskCacheStrategy(DiskCacheStrategy.ALL)  // Cache for better performance
                    .into(jumpImageView);  // Load into your ImageView
            resumeTracking();
        } else {
            // updateImageViewForMedia(false);
            // Switch to jump PNG
            Glide.with(this)
                    .asBitmap()  // Load the first frame of the GIF as a static image
                    .load("https://i.giphy.com/media/v1.Y2lkPTc5MGI3NjExdWIxMm1" +
                            "xcDl4dGVldGkybnlkN3F0ZG15OWdkZWJ1d2FlMzZyamdqbyZlcD12MV9pbnR" +
                            "lcm5hbF9naWZfYnlfaWQmY3Q9cw/Exf7ID0UMEYp3Gx0Zf/giphy.gif")  // 加载 PNG
                    .into(jumpImageView);
            pauseTracking();
        }

    }

    private void updateImageViewForMedia(boolean isGif) {
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) jumpImageView.getLayoutParams();

        if (isGif) {
            // 如果是 GIF，设置高度为 600dp，且边距为 0
            params.height = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 500, jumpImageView.getResources().getDisplayMetrics());
            params.setMargins(0, -150, 0, 0);  // 移除所有边距

            // 使用 Glide 加载 GIF
            Glide.with(this)
                    .asGif()
                    .load(R.drawable.jump_gif)
                    .into(jumpImageView);
        } else {
            // 如果是 PNG，设置高度为 270dp，且恢复原始边距
            params.height = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 270, jumpImageView.getResources().getDisplayMetrics());
            params.setMargins(0, 40, 0, 0);  // 恢复原始边距

            // 使用 Glide 加载 PNG
            Glide.with(this)
                    .load(R.drawable.bg_jump)
                    .into(jumpImageView);
        }

        jumpImageView.setLayoutParams(params);  // 应用更改
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

        // Find the TextView in the dialog and set the dynamic message
        TextView dialogMessage = dialogView.findViewById(R.id.dialog_message);
        dialogMessage.setText(popUpMessage);  // Set the custom message

        Button positiveButton = dialogView.findViewById(R.id.positive_button);
        Button negativeButton = dialogView.findViewById(R.id.negative_button);

        positiveButton.setOnClickListener(v -> {
            Intent intent = new Intent();
            setResult(RESULT_OK, intent);  // Set the result to pass back to MainActivity
            finish();
            dialog.dismiss();
        });

        negativeButton.setOnClickListener(v -> dialog.dismiss());
    }



    /**
     * Initializes the StepCounter and sets up its listener.
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void initJumpCounter() {
        jumpCounter = new JumpCounter(this);
        jumpCounter.setJumpCounterListener(jumpCount -> {
            runOnUiThread(() -> {
                currentJumpCount = Math.max(0, jumpCount / 2 - 1);
                jumpTextView.setText(String.valueOf(currentJumpCount));
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
     * Called when the back button is pressed
     */
    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        popUpConfirmDialog();
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
        Glide.with(this)
                .asBitmap()  // Load the first frame of the GIF as a static image
                .load("https://i.giphy.com/media/v1.Y2lkPTc5MGI3NjExdWIxMm1" +
                        "xcDl4dGVldGkybnlkN3F0ZG15OWdkZWJ1d2FlMzZyamdqbyZlcD12MV9pbnR" +
                        "lcm5hbF9naWZfYnlfaWQmY3Q9cw/Exf7ID0UMEYp3Gx0Zf/giphy.gif")  // 加载 PNG
                .into(jumpImageView);
            // Ensure step count and timer views are visible
        jumpTextView.setVisibility(View.VISIBLE);
        timerTextView.setVisibility(View.VISIBLE);
    }



    /**
     * Shows the speed gif end markers.
     */
    private void showJumpResult() {
        long currentTime = System.currentTimeMillis();
        if(formattedStartTime == null){
            formattedStartTime = dateFormat.format(new Date(currentTime));
        }

        formattedFinishTime = dateFormat.format(new Date(currentTime));

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
        intent.putExtra("startDateTime", formattedStartTime);
        intent.putExtra("finishDateTime", formattedFinishTime);

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
