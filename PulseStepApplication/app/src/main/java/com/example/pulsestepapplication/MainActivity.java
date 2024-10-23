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

import com.example.pulsestepapplication.databinding.ActivityMainBinding;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    // Cache the WorkoutFragment instance
    private Fragment workoutFragment;
    private Fragment rankingFragment;
    private Fragment profileFragment;
    private Fragment activeFragment;  // The currently displayed Fragment
    private String userId;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        userId = getIntent().getStringExtra("USER_ID");
        if (!hasLocationPermissions()) {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, LOCATION_PERMISSION_REQUEST_CODE);
        }
        if (savedInstanceState == null) {

            fetchUserData();
        } else {
            String activeFragmentTag = savedInstanceState.getString("activeFragmentTag");
            activeFragment = getSupportFragmentManager().findFragmentByTag(activeFragmentTag);
            int selectedItemId = savedInstanceState.getInt("selectedItemId");
            binding.bottomNavigationView.setSelectedItemId(selectedItemId);

            workoutFragment = getSupportFragmentManager().findFragmentByTag("workout");
            rankingFragment = getSupportFragmentManager().findFragmentByTag("ranking");
            profileFragment = getSupportFragmentManager().findFragmentByTag("profile");

            if (workoutFragment == null) {
                initWorkoutFragment(hasLocationPermissions(), new Bundle());
            }
            if (rankingFragment == null) {
                initRankingFragment(new Bundle());
            }
            if (profileFragment == null) {
                initProfileFragment(new Bundle());
            }
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

    /**
     * Displays the specified Fragment using show() and hide() to avoid refreshing.
     */
    private void showFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        if (activeFragment != null) {
            transaction.hide(activeFragment);
        }

        if (!isFinishing() && !isDestroyed()) {
            transaction.show(fragment).commit();
        }

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

    private void fetchUserData() {
        if (userId != null) {
            DocumentReference userRef = db.collection("users").document(userId);


            userRef.addSnapshotListener((documentSnapshot, error) -> {
                if (error != null) {
                    Log.e("UserInfo", "Listen failed.", error);
                    return;
                }

                if (documentSnapshot != null && documentSnapshot.exists()) {
                    String fullName = documentSnapshot.getString("fullName");
                    String weight = documentSnapshot.getString("weight");
                    String gender = documentSnapshot.getString("gender");

                    if (fullName != null && weight != null) {
                        Log.e("UserInfo", "Full Name: " + fullName + ", Weight: " + weight);


                        passDataToFragments(userId, fullName, Double.parseDouble(weight), gender);
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

    private void passDataToFragments(String userId, String fullName, Double weight, String gender) {
        // 创建 Bundle 存储数据
        Bundle args = new Bundle();
        args.putString("userId", userId);
        args.putString("fullName", fullName);
        args.putDouble("weight", weight);
        args.putString("gender", gender);
        args.putBoolean("locationGranted", hasLocationPermissions());

        Log.e("PassedData", "userId: " + userId + ", Full Name: " + fullName + ", Weight: " + weight + ", Gender: " + gender);

        if (workoutFragment == null) {
            initWorkoutFragment(hasLocationPermissions(), args);
        } else {
            ((WorkoutFragment) workoutFragment).updateData(fullName, weight, hasLocationPermissions());
        }

        if (rankingFragment == null) {
            initRankingFragment(args);
        }

        if (profileFragment == null) {
            initProfileFragment(args);
        } else {
            ((ProfileFragment) profileFragment).updateData(userId, fullName, gender);
        }

        if (activeFragment == null) {
            activeFragment = workoutFragment;
        }

        showFragment(activeFragment);
    }

    private void initWorkoutFragment(boolean locationGranted, Bundle args) {
        if (getSupportFragmentManager().findFragmentByTag("workout") == null) {
            workoutFragment = new WorkoutFragment();
            workoutFragment.setArguments(args);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.frameLayout, workoutFragment, "workout")
                    .commit();
        }
    }

    private void initRankingFragment(Bundle args) {
        if (getSupportFragmentManager().findFragmentByTag("ranking") == null) {
            rankingFragment = new RankingFragment();
//            rankingFragment.setArguments(args);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.frameLayout, rankingFragment, "ranking")
                    .hide(rankingFragment)
                    .commit();
        }
    }

    private void initProfileFragment(Bundle args) {
        if (getSupportFragmentManager().findFragmentByTag("profile") == null) {
            profileFragment = new ProfileFragment();
            profileFragment.setArguments(args);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.frameLayout, profileFragment, "profile")
                    .hide(profileFragment)
                    .commit();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (activeFragment != null) {
            outState.putString("activeFragmentTag", activeFragment.getTag());
        }
        outState.putInt("selectedItemId", binding.bottomNavigationView.getSelectedItemId());
    }
}
