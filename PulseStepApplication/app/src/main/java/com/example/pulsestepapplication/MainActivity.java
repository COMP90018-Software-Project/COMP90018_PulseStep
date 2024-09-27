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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        replaceFragment(new WorkoutFragment());
        Log.d("DEBUG", "home page: workout fragment");

        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
                switch(item.getItemId()){

                    case R.id.workout:
                        replaceFragment(new WorkoutFragment());
                        Log.d("DEBUG", "change to workout");
                        break;
                    case R.id.ranking:
                        replaceFragment(new RankingFragment());
                        Log.d("DEBUG", "change to ranking");
                        break;
                    case R.id.profile:
                        replaceFragment(new ProfileFragment());
                        Log.d("DEBUG", "change to profile");
                        break;
                }

                return true;
        });
    }

    private void replaceFragment(Fragment fragment){
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frameLayout, fragment);
        fragmentTransaction.commit();
    }
}