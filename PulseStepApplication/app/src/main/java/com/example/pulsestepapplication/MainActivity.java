package com.example.pulsestepapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.example.pulsestepapplication.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    // Cache the WorkoutFragment instance
    private Fragment workoutFragment;
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

        // Initialize bottom navigation view
        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.workout:
                    showFragment(workoutFragment);
                    Log.d("DEBUG", "change to workout");
                    break;
                case R.id.ranking:
                    showFragment(new RankingFragment());
                    Log.d("DEBUG", "change to ranking");
                    break;
                case R.id.profile:
                    showFragment(new ProfileFragment());
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

    /**
     * Displays the specified Fragment using show() and hide() to avoid refreshing.
     */
    private void showFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // If the fragment to show is the cached workoutFragment
        if (fragment == workoutFragment) {
            // Only show and hide the other Fragment
            transaction.hide(activeFragment).show(workoutFragment);
        } else {
            // If it is not the workoutFragment
            if (!fragment.isAdded()) {
                // If the fragment is not added, add it and hide the current Fragment
                transaction.hide(activeFragment).add(R.id.frameLayout, fragment);
            } else {
                // If the fragment is already added, directly show it and hide the current Fragment
                transaction.hide(activeFragment).show(fragment);
            }
        }

        transaction.commit();
        activeFragment = fragment; // Update the currently displayed Fragment
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
