package com.example.pulsestepapplication;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Check Google Play Services availability
        if (checkGooglePlayServices()) {
            proceedAfterPlayServices();
        }
        // If not available, checkGooglePlayServices() will handle the prompt
    }

    /**
     * Check if Google Play Services are available.
     *
     * @return true if available, false otherwise.
     */
    private boolean checkGooglePlayServices() {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int status = googleApiAvailability.isGooglePlayServicesAvailable(this);
        if (status != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(status)) {
                // Show dialog to resolve the issue
                Objects.requireNonNull(googleApiAvailability.getErrorDialog(this, status, PLAY_SERVICES_RESOLUTION_REQUEST)).show();
            } else {
                Log.e(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        Log.d(TAG, "Google Play Services is available.");
        return true;
    }

    /**
     * Proceed with app logic after confirming Play Services.
     */
    private void proceedAfterPlayServices() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // User is logged in, navigate to MainActivity
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            intent.putExtra("USER_ID", currentUser.getUid());
            startActivity(intent);
        } else {
            // User not logged in, navigate to StartActivity
            Intent intent = new Intent(SplashActivity.this, StartActivity.class);
            startActivity(intent);
        }
        finish(); // Close SplashActivity
    }

    /**
     * Handle the result from Play Services resolution dialog.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PLAY_SERVICES_RESOLUTION_REQUEST) {
            if (resultCode == RESULT_OK) {
                // User resolved the issue, proceed
                Log.d(TAG, "User resolved Google Play Services issue.");
                proceedAfterPlayServices();
            } else {
                // User did not resolve, show alert and exit
                Log.e(TAG, "Google Play Services not available. Exiting.");
                new AlertDialog.Builder(this)
                        .setTitle("Google Play Services Required")
                        .setMessage("This app requires Google Play Services to function properly. Please install or update Google Play Services and try again.")
                        .setPositiveButton("Retry", (dialog, which) -> {
                            checkGooglePlayServices();
                        })
                        .setNegativeButton("Exit", (dialog, which) -> {
                            dialog.dismiss();
                            finish();
                        })
                        .setCancelable(false)
                        .show();
            }
        }
    }
}
