package com.example.pulsestepapplication;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Check if user is already logged in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // If user is logged in, go to the main activity
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
        } else {
            // If user is not logged in, go to the login activity
            startActivity(new Intent(SplashActivity.this, Login.class));
        }

        finish(); // Close the splash activity
    }
}
