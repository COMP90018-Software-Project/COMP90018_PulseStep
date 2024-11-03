package com.example.pulsestepapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class RankRowDetail extends AppCompatActivity {
        private ImageView profileImage;

        private String userId;
        private String selectedDate;

        private final FirebaseFirestore db = FirebaseFirestore.getInstance();

        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_rank_row_detail);

            userId = getIntent().getStringExtra("userId");

            fetchUserData();

            ImageView backArrow = findViewById(R.id.back_button_setting_page);
            backArrow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Finish the current activity and return to the RankingFragment page
                    Intent intent = new Intent(RankRowDetail.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                }
            });

            // Set the current date as the selected date
            selectedDate = getCurrentDate();

            // Fetch daily info based on the current date
            fetchUserDailyInfo(selectedDate);
        }

    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    // Fetch the profile image from Firebase Storage
    private void fetchProfileImage(String gender) {
        FirebaseStorage.getInstance().getReference()
                .child("users")
                .child(userId)
                .child("images/profile_image")
                .getDownloadUrl()
                .addOnSuccessListener(uri -> setProfilePic(uri, profileImage))
                .addOnFailureListener(exception -> {
                    if (exception instanceof StorageException &&
                            ((StorageException) exception).getErrorCode() == StorageException.ERROR_OBJECT_NOT_FOUND) {
                        // Set default avatar based on gender
                        setDefaultAvatar(gender);
                    } else {
                        Log.e("ProfileActivity", "Error fetching profile image: " + exception.getMessage());
                    }
                });
    }

    private void setProfilePic(Uri imageUri, ImageView imageView) {
        if (!isDestroyed() && !isFinishing()) {
            Glide.with(this).load(imageUri).apply(RequestOptions.circleCropTransform()).into(imageView);
        } else {
            Log.e("RankRowDetail", "Activity is destroyed. Cannot load image.");
        }
    }

    // Set default avatar based on gender
    private void setDefaultAvatar(String gender) {
        if (gender != null) {
            Log.e("IM IN","HHHHHHHHHHHHHH");
            if (gender.equalsIgnoreCase("male")) {
                profileImage.setImageResource(R.drawable.male_default_avatar);
            } else if (gender.equalsIgnoreCase("female")) {
                profileImage.setImageResource(R.drawable.female_default_avatar);
            } else {
                profileImage.setImageResource(R.drawable.default_avatar);
            }
        }
    }

    private void fetchUserData() {
        if (userId != null) {
            DocumentReference userRef = db.collection("users").document(userId);

            // Adding a snapshot listener to get real-time updates
            userRef.addSnapshotListener((documentSnapshot, error) -> {
                if (error != null) {
                    Log.e("UserInfo", "Listen failed.", error);
                    return;
                }

                if (documentSnapshot != null && documentSnapshot.exists()) {
                    // Get user information
                    String userName = documentSnapshot.getString("fullName");
                    String gender = documentSnapshot.getString("gender");
                    // Ensure the fields are not null
                    if (userName != null && gender != null) {
                        Log.e("UserInfo", "Full Name: " + userName + ", Weight: " + gender);

                        // Pass the data to the Fragment
                        updateName(userName);
                        updateAvatar(gender);
                    } else {
                        Log.e("UserInfo", "Some fields are missing.");
                    }
                } else {
                    Log.e("UserInfo", "Document does not exist.");
                }
            });
        } else {
            Log.e("UserInfo", "User is not logged in or userId is null.");
        }
    }

    private void updateName(String userName) {
        TextView nameTextView = findViewById(R.id.name);
        nameTextView.setText(userName);
    }

    private void updateAvatar(String gender){
        profileImage = findViewById(R.id.profile_image);

        // Fetch profile image from Firebase Storage
        fetchProfileImage(gender);
    }

    private void fetchUserDailyInfo(String date) {
        DocumentReference userRef = FirebaseFirestore.getInstance().collection("users").document(userId);

        // Add snapshot listener for real-time updates
        userRef.addSnapshotListener((documentSnapshot, error) -> {
            if (error != null) {
                Log.e("RankRowDetail", "Listen failed.", error);
                return;
            }

            if (documentSnapshot != null && documentSnapshot.exists()) {
                Map<String, Map<String, Object>> dailyJumpInfo = (Map<String, Map<String, Object>>) documentSnapshot.get("dailyJumpInfo");
                Map<String, Map<String, Object>> dailyRunningInfo = (Map<String, Map<String, Object>>) documentSnapshot.get("dailyRunningInfo");

                // Process jump info
                if (dailyJumpInfo != null && dailyJumpInfo.containsKey(date)) {
                    Map<String, Object> todayJumpData = dailyJumpInfo.get(date);
                    long activeJumpTime = todayJumpData.get("activeTime") != null ? (Long) todayJumpData.get("activeTime") : 0;
                    int jumpCount = todayJumpData.get("jumpCount") != null ? ((Long) todayJumpData.get("jumpCount")).intValue() : 0;
                    double jumpCalories = todayJumpData.get("calories") != null ? ((Number) todayJumpData.get("calories")).doubleValue() : 0.0;
                    updateJumpUI(activeJumpTime, jumpCount, jumpCalories);
                } else {
                    updateJumpUINoRecord();
                }

                // Process running info
                if (dailyRunningInfo != null && dailyRunningInfo.containsKey(date)) {
                    Map<String, Object> todayRunningData = dailyRunningInfo.get(date);
                    long activeRunningTime = todayRunningData.get("activeTime") != null ? (Long) todayRunningData.get("activeTime") : 0;
                    float runningDistance = todayRunningData.get("distance") != null ? ((Number) todayRunningData.get("distance")).floatValue() : 0.0f;
                    double runningCalories = todayRunningData.get("calories") != null ? ((Number) todayRunningData.get("calories")).doubleValue() : 0.0;
                    updateRunningUI(activeRunningTime, runningDistance, runningCalories);
                } else {
                    updateRunningUINoRecord();
                }
            } else {
                Log.w("RankRowDetail", "Document does not exist.");
            }
        });
    }


    // Helper to update jump-related UI
    private void updateJumpUI(long activeTime, int jumpCount, double calories) {
        TextView workoutDurationTextView = findViewById(R.id.workout_duration_jump);
        TextView workoutCountTextView = findViewById(R.id.workout_count_jump);
        TextView workoutCaloriesTextView = findViewById(R.id.workout_calories_jump);

        workoutDurationTextView.setText(String.format(Locale.getDefault(), "%.1f min", (double) activeTime / 60));
        workoutCountTextView.setText(String.format(Locale.getDefault(), "%d times", jumpCount));
        workoutCaloriesTextView.setText(String.format(Locale.getDefault(), "%.1f kcal", calories / 1000));
    }

    // Helper to update running-related UI
    private void updateRunningUI(long activeTime, float distance, double calories) {
        TextView workoutDurationTextView = findViewById(R.id.workout_duration_run);
        TextView workoutCountTextView = findViewById(R.id.workout_count_run);
        TextView workoutCaloriesTextView = findViewById(R.id.workout_calories_run);

        workoutDurationTextView.setText(String.format(Locale.getDefault(), "%.1f min", (double) activeTime / 60));
        workoutCountTextView.setText(String.format(Locale.getDefault(), "%.1f km", distance / 1000));
        workoutCaloriesTextView.setText(String.format(Locale.getDefault(), "%.1f kcal", calories / 1000));
    }

    private void updateRunningUINoRecord() {
        TextView runDurationTextView = findViewById(R.id.workout_duration_run);
        TextView runCountTextView = findViewById(R.id.workout_count_run);
        TextView runCaloriesTextView = findViewById(R.id.workout_calories_run);

        runDurationTextView.setText("-- min");
        runCountTextView.setText("-- km");
        runCaloriesTextView.setText("-- kcal");
    }

    private void updateJumpUINoRecord() {
        TextView jumpDurationTextView = findViewById(R.id.workout_duration_jump);
        TextView jumpCountTextView = findViewById(R.id.workout_count_jump);
        TextView jumpCaloriesTextView = findViewById(R.id.workout_calories_jump);

        jumpDurationTextView.setText("-- min");
        jumpCountTextView.setText("-- times");
        jumpCaloriesTextView.setText("-- kcal");
    }
}
