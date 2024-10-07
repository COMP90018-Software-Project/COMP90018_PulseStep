package com.example.pulsestepapplication;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class Settings extends AppCompatActivity {

    private ImageView backButton;
    private Button logOutButton;
    private LinearLayout resetPasswordButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        // Reference to the back button
        backButton = findViewById(R.id.back_button_setting_page);

        // Handle back button click
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // End the Settings activity and return to MainActivity
            }
        });

        // Reference to the reset_password redirecting button
        resetPasswordButton = findViewById(R.id.reset_password);
        resetPasswordButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                // Finish the current activity and return to the RunSummaryActivity page
                Intent intent = new Intent(Settings.this, ResetPassword.class);
                startActivity(intent);
            }
        });


        // Reference to the logout button
        logOutButton = findViewById(R.id.bt_logout_settings);

        // Handle logout button click
        logOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show the Material AlertDialog for logout confirmation
                new MaterialAlertDialogBuilder(Settings.this)
                        .setTitle("Log out")
                        .setMessage("Are you sure you want to log out?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Handle the logout action here
                                performLogout();
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Dismiss the dialog if "Cancel" is clicked
                                dialog.dismiss();
                            }
                        })
                        .show();
            }
        });
    }

    // Function to perform the logout action
    private void performLogout() {
        // Clearing session, navigating to login screen, etc.
    }

}
