package com.example.pulsestepapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class ResetPassword extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        // Find the back button by its ID
        ImageView backButton = findViewById(R.id.back_button_change_password_page);

        // Set click listener for the back button
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Finish the current activity and return to the Settings page
                Intent intent = new Intent(ResetPassword.this, Settings.class);
                startActivity(intent);
            }
        });

        // Load the first fragment (PasswordResetFragment)
        if (savedInstanceState == null) {
            loadFragment(new PasswordResetConfirmFragment());
        }
    }


    // Method to load a fragment into the container
    public void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null); // Allows the user to go back
        transaction.commit();
    }
}