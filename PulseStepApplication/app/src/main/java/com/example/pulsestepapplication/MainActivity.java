package com.example.pulsestepapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.pulsestepapplication.databinding.ActivityMainBinding;

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
            // If location permissions are already granted, initialize the WorkoutFragment
            initWorkoutFragment(true);
        }

        // Initialize RankingFragment and ProfileFragment
        initRankingFragment();
        initProfileFragment();

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

    private void initWorkoutFragment(boolean locationGranted) {
        // Create WorkoutFragment instance
        workoutFragment = new WorkoutFragment();
        activeFragment = workoutFragment; // Set the initial fragment to workoutFragment

        // Pass arguments, including location permission status
        Bundle args = new Bundle();
        args.putString("name", "Jackie");
        args.putInt("age", 66);
        args.putDouble("weight", 66.6);
        args.putBoolean("locationGranted", locationGranted); // Pass location permission status
        workoutFragment.setArguments(args);

        // Add the WorkoutFragment to the container
        getSupportFragmentManager().beginTransaction()
                .add(R.id.frameLayout, workoutFragment, "workout")
                .commit();

        Log.d("DEBUG", "home page: workout fragment");
    }

    private void initProfileFragment() {
        // Create a new RankingFragment instance
        profileFragment = new ProfileFragment();

        Bundle args = new Bundle();
        args.putString("Name", "exampleUserName");  // 传递 Name 值
        profileFragment.setArguments(args);

        // Add the RankingFragment to the fragment container
        getSupportFragmentManager().beginTransaction()
                .add(R.id.frameLayout, profileFragment, "profile")
                .hide(profileFragment)  // Initially hide it, since workoutFragment is shown first
                .commit();

        Log.d("DEBUG", "ProfileFragment initialized and cached");
    }

    private void initRankingFragment() {
        // Create a new RankingFragment instance
        rankingFragment = new RankingFragment();

//        // Optionally, you can pass arguments to the RankingFragment
//        // If you don't need to pass any data, you can skip this part
//        Bundle args = new Bundle();
//        args.putString("category", "sports");  // Example of passing arguments
//        rankingFragment.setArguments(args);

        // Add the RankingFragment to the fragment container
        getSupportFragmentManager().beginTransaction()
                .add(R.id.frameLayout, rankingFragment, "ranking")
                .hide(rankingFragment)  // Initially hide it, since workoutFragment is shown first
                .commit();

        Log.d("DEBUG", "RankingFragment initialized and cached");
    }

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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            boolean locationGranted = false;
            if (grantResults.length > 0) {
                for (int result : grantResults) {
                    if (result == PackageManager.PERMISSION_GRANTED) {
                        locationGranted = true;
                        break;
                    }
                }
            }

            // Initialize WorkoutFragment based on permission result
            initWorkoutFragment(locationGranted);
        }
    }
}
