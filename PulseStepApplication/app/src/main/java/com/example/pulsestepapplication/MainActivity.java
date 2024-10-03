package com.example.pulsestepapplication;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
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

    // Cache the WorkoutFragment instance
    private Fragment workoutFragment;
    private Fragment activeFragment; // The currently displayed Fragment

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize and display WorkoutFragment
        workoutFragment = new WorkoutFragment();
        activeFragment = workoutFragment; // Set the initial fragment to workoutFragment

        // Use add() to add the initial Fragment and display it
        getSupportFragmentManager().beginTransaction()
                .add(R.id.frameLayout, workoutFragment, "workout")
                .commit();

        Log.d("DEBUG", "home page: workout fragment");

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
}
