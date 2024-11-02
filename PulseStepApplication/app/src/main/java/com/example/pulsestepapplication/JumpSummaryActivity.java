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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class JumpSummaryActivity extends AppCompatActivity {

    // UI Components
    private TextView avgPaceTextView;
    private TextView timeTextView;
    private TextView addressTextView;
    private TextView totalJumpCountTextView;
    private TextView caloriesTextView;

    // Tracking Data
    private String time; // formatted as "MM:SS"
    private String address; // optional
    private int jumpCount;
    private String avgPace;
    private Button finishButton;
    private String calories;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jump_summary);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize UI components
        initializeUIComponents();

        // Retrieve data from Intent
        retrieveIntentData();

        // Display data
        displayData();

        // Get user UID
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String userUID = currentUser.getUid();

        // Get passed startDateTime and finishDateTime
        String startDateTime = getIntent().getStringExtra("startDateTime");
        String finishDateTime = getIntent().getStringExtra("finishDateTime");
        Log.d("RunSummary", "startDateTime: " + startDateTime);
        Log.d("RunSummary", "finishDateTime: " + finishDateTime);

        // Handle Finish button click
        finishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Map<String, Object> userJumpRopeDetails = new HashMap<>();

                userJumpRopeDetails.put("userId", userUID);
                userJumpRopeDetails.put("startDateTime", startDateTime);
                userJumpRopeDetails.put("finishDateTime", finishDateTime);
                userJumpRopeDetails.put("activeTime", time);
                userJumpRopeDetails.put("avgJumpCount", avgPace);
                userJumpRopeDetails.put("calories", calories);
                userJumpRopeDetails.put("jumpCount ", jumpCount);
                userJumpRopeDetails.put("location", address);

                // Save data to Firestore
                db.collection("jump").add(userJumpRopeDetails)
                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {
                                Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                                updateUserActiveTime(userUID, time);
                                updateUserJumpInfo(userUID, time, jumpCount, Double.parseDouble(calories));
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG, "Error adding document", e);
                            }
                        });

                // Finish the activity and return to the previous screen
                Intent intent = new Intent(JumpSummaryActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra("fragment", "WorkoutFragment");
                intent.putExtra("USER_ID", userUID);
                startActivity(intent);
                finish();
            }
        });

    }

    /**
     * Initializes the UI components by finding them via their IDs.
     */
    private void initializeUIComponents() {
        timeTextView = findViewById(R.id.jump_summary_time);
        addressTextView = findViewById(R.id.jump_summary_address);
        avgPaceTextView = findViewById(R.id.jump_summary_avg_count);
        totalJumpCountTextView = findViewById(R.id.jump_count);
        caloriesTextView = findViewById(R.id.jump_summary_calories);
        finishButton = findViewById(R.id.bt_finish_jump);
    }

    /**
     * Retrieves data passed from the tracking activity via Intent.
     */
    private void retrieveIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            time = intent.getStringExtra("time");
            jumpCount = intent.getIntExtra("jumpCount", 0);
            address = intent.getStringExtra("address");
            avgPace = intent.getStringExtra("avgSpeed");
            try {
                Double.parseDouble(avgPace);
            } catch (NumberFormatException e) {
                avgPace = "0";
            }

            calories = intent.getStringExtra("calories");
            try {
                Double.parseDouble(calories);
            } catch (NumberFormatException e) {
                calories = "0";
            }
        }
    }


    /**
     * Displays the retrieved data on the UI components.
     */
    private void displayData() {
        Log.d("DEBUG", "Average Jump Speed: " + avgPace);
        // distanceTextView.setText(String.format("%.2f", distance));
        timeTextView.setText(time != null ? time : "00:00");
        totalJumpCountTextView.setText(String.valueOf(jumpCount));
        addressTextView.setText(address != null ? address : "N/A");
        avgPaceTextView.setText(avgPace != null ? avgPace : "N/A");
        caloriesTextView.setText(calories != null ? calories : "N/A");

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

    private void updateUserJumpInfo(String userId, String activeTime, int jumpCount, double calories) {
        // Convert active time from string MM:SS to seconds.
        int timeInSeconds = convertTimeToSeconds(activeTime);

        // Retrieve current date
        String today = getCurrentDate();

        // Update dailyActive and monthlyActive in users
        DocumentReference userRef = db.collection("users").document(userId);
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // Get current dailyRunningInfo data
                Map<String, Map<String, Object>> dailyJumpInfo = (Map<String, Map<String, Object>>) documentSnapshot.get("dailyJumpInfo");

                // Initialize if not exist
                if (dailyJumpInfo == null) {
                    dailyJumpInfo = new HashMap<>();
                }

                // Calculate new active time
                long updatedDailyActiveTime = dailyJumpInfo.containsKey(today) && dailyJumpInfo.get(today).get("activeTime") instanceof Long
                        ? (Long) dailyJumpInfo.get(today).get("activeTime") + timeInSeconds
                        : timeInSeconds;

                // Calculate new distance, handle the conversion from Double to Float
                int updatedDailyJumpCount = dailyJumpInfo.containsKey(today) && dailyJumpInfo.get(today).get("jumpCount") instanceof Integer
                        ? (Integer) dailyJumpInfo.get(today).get("jumpCount") + jumpCount
                        : jumpCount;

                // Calculate new calories, handle the conversion from Double to Float if needed
                double updatedDailyCalories = dailyJumpInfo.containsKey(today) && dailyJumpInfo.get(today).get("calories") instanceof Double
                        ? (Double) dailyJumpInfo.get(today).get("calories") + calories
                        : calories;

                // Create or update daily activity entry with time, distance, and calories
                Map<String, Object> dailyData = new HashMap<>();
                dailyData.put("activeTime", updatedDailyActiveTime);
                dailyData.put("jumpCount", updatedDailyJumpCount);
                dailyData.put("calories", updatedDailyCalories);

                // Update Firestore data
                Map<String, Object> updates = new HashMap<>();
                updates.put("dailyJumpInfo." + today, dailyData);

                userRef.update(updates).addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User daily jump info updated successfully.");
                }).addOnFailureListener(e -> {
                    Log.w(TAG, "Error updating user daily jump info", e);
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
