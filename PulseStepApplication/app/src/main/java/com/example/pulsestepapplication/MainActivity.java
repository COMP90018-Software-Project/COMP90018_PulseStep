package com.example.pulsestepapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.pulsestepapplication.databinding.ActivityMainBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    // Cache the WorkoutFragment instance
    private Fragment workoutFragment;
    private Fragment rankingFragment;
    private Fragment profileFragment;
    private Fragment activeFragment; // The currently displayed Fragment

    private String name;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    private String userId = currentUser != null ? currentUser.getUid() : null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (!hasLocationPermissions()) {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // If location permissions are already granted, initialize the 3 Fragment
            fetchUserData();
        }

        // Initialize bottom navigation view
        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.workout:
                    showFragment(workoutFragment);
                    Log.d("DEBUG", "change to workout");
                    break;
                case R.id.ranking:
                    showFragment(rankingFragment);
                    Log.d("DEBUG", "change to ranking");
                    break;
                case R.id.profile:
                    showFragment(profileFragment);
                    Log.d("DEBUG", "change to profile");
                    break;
            }
            return true;
        });
    }

//    private void initWorkoutFragment(boolean locationGranted) {
//        // Create WorkoutFragment instance
//        workoutFragment = new WorkoutFragment();
//        activeFragment = workoutFragment; // Set the initial fragment to workoutFragment
//
//        // Pass arguments, including location permission status
//        Bundle args = new Bundle();
//        args.putString("name", name);
//        Log.e("WorkoutFragment", "userName = "+ name);
////        args.putInt("age", 66);
////        args.putDouble("weight", 66.6);
//        args.putBoolean("locationGranted", locationGranted); // Pass location permission status
//        workoutFragment.setArguments(args);
//
//        // Add the WorkoutFragment to the container
//        getSupportFragmentManager().beginTransaction()
//                .add(R.id.frameLayout, workoutFragment, "workout")
//                .commit();
//
//        Log.d("DEBUG", "home page: workout fragment");
//    }
//
//    private void initProfileFragment() {
//        // Create a new RankingFragment instance
//        profileFragment = new ProfileFragment();
//
//        // Add the RankingFragment to the fragment container
//        getSupportFragmentManager().beginTransaction()
//                .add(R.id.frameLayout, profileFragment, "profile")
//                .hide(profileFragment)  // Initially hide it, since workoutFragment is shown first
//                .commit();
//
//        Log.d("DEBUG", "ProfileFragment initialized and cached");
//    }
//
//    private void initRankingFragment() {
//        // Create a new RankingFragment instance
//        rankingFragment = new RankingFragment();
//
//        // Add the RankingFragment to the fragment container
//        getSupportFragmentManager().beginTransaction()
//                .add(R.id.frameLayout, rankingFragment, "ranking")
//                .hide(rankingFragment)  // Initially hide it, since workoutFragment is shown first
//                .commit();
//
//        Log.d("DEBUG", "RankingFragment initialized and cached");
//    }

    /**
     * Displays the specified Fragment using show() and hide() to avoid refreshing.
     */
    private void showFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // Check if the fragment is already added
        if (!fragment.isAdded()) {
            // If the fragment is not added, add it and hide the current fragment
            transaction.hide(activeFragment).add(R.id.frameLayout, fragment);
        } else {
            // If the fragment is already added, directly show it and hide the current fragment
            transaction.hide(activeFragment).show(fragment);
        }

        // Commit the transaction and update the currently active fragment
        transaction.commit();
        activeFragment = fragment;
    }

    /**
     * Checks if location permissions are granted.
     *
     * @return True if granted, false otherwise.
     */
    private boolean hasLocationPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
//                                           @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//
//        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
//            boolean locationGranted = false;
//            if (grantResults.length > 0) {
//                for (int result : grantResults) {
//                    if (result == PackageManager.PERMISSION_GRANTED) {
//                        locationGranted = true;
//                        break;
//                    }
//                }
//            }
//
//            // Initialize WorkoutFragment based on permission result
//            initWorkoutFragment(locationGranted);
//        }
//    }


    private void fetchUserData() {
        if (userId != null) {
            DocumentReference userRef = db.collection("users").document(userId);

            userRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    // 获取用户信息
                    String fullName = documentSnapshot.getString("fullName");
                    String weight = documentSnapshot.getString("weight");

                    // 确保字段存在
                    if (fullName != null && weight != null) {
                        Log.e("UserInfo", "Full Name: " + fullName + ", Weight: " + weight);

                        // 把数据传递给 Fragment
                        passDataToFragments(fullName, Double.parseDouble(weight));
                    } else {
                        Log.e("UserInfo", "Some fields are missing.");
                    }
                } else {
                    Log.e("UserInfo", "Document does not exist.");
                }
            }).addOnFailureListener(e -> {
                Log.e("UserInfo", "Error fetching document", e);
            });
        } else {
            Log.e("UserInfo", "User is not logged in or userId is null.");
        }
    }


    private void passDataToFragments(String fullName, Double weight) {
        // 创建 Bundle 存储数据
        Bundle args = new Bundle();
        args.putString("fullName", fullName);
        args.putDouble("weight", weight);
        Log.e("PassedData", "Full Name: " + fullName + ", Weight: " + weight);

        // 初始化各个 Fragment 并传递数据
        initWorkoutFragment(true, args);
        initRankingFragment(args);
        initProfileFragment(args);
    }


    private void initWorkoutFragment(boolean locationGranted, Bundle args) {
        workoutFragment = new WorkoutFragment();
        activeFragment = workoutFragment;

        // 添加 locationGranted 信息到 Bundle
        args.putBoolean("locationGranted", locationGranted);
        workoutFragment.setArguments(args);

        getSupportFragmentManager().beginTransaction()
                .add(R.id.frameLayout, workoutFragment, "workout")
                .commit();

        Log.d("DEBUG", "home page: workout fragment");
    }

    private void initProfileFragment(Bundle args) {
        profileFragment = new ProfileFragment();
        profileFragment.setArguments(args);

        getSupportFragmentManager().beginTransaction()
                .add(R.id.frameLayout, profileFragment, "profile")
                .hide(profileFragment)  // Initially hide it, since workoutFragment is shown first
                .commit();

        Log.d("DEBUG", "ProfileFragment initialized and cached");
    }

    private void initRankingFragment(Bundle args) {
        rankingFragment = new RankingFragment();
        rankingFragment.setArguments(args);

        getSupportFragmentManager().beginTransaction()
                .add(R.id.frameLayout, rankingFragment, "ranking")
                .hide(rankingFragment)  // Initially hide it, since workoutFragment is shown first
                .commit();

        Log.d("DEBUG", "RankingFragment initialized and cached");
    }


}
