package com.example.pulsestepapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

public class Settings extends AppCompatActivity {

    private ImageView backButton;
    private Button logOutButton;
    private TextView deactivateButton;
    private LinearLayout resetPasswordButton;

    private LinearLayout editPersonalInfoButton;

    private LinearLayout userGuideLinesButton;

    private MaterialSwitch locationSwitch;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private boolean isUserInitiatedSwitchChange = false;
    private MaterialSwitch notificationSwitch;
    private SharedPreferences sharedPref;
    private String userId;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        // 获取从 ProfileFragment 传递的 userId
        Intent intent = getIntent();
        userId = intent.getStringExtra("userId");

        if (userId != null) {
            Log.d("SettingsActivity", "Received userId: " + userId);
            // 使用 userId 做进一步操作
        } else {
            Log.e("SettingsActivity", "No userId received.");
        }

        // Reference to the back button
        backButton = findViewById(R.id.back_button_setting_page);

        // Handle back button click
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // End the Settings activity and return to MainActivity
            }
        });
        notificationSwitch = findViewById(R.id.notification_switch);

        sharedPref = getSharedPreferences("my_prefs", MODE_PRIVATE);
        boolean isNotificationEnabled = sharedPref.getBoolean("notification_switch", true);
        notificationSwitch.setChecked(isNotificationEnabled);
        // Set listener
        notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean("notification_switch", isChecked);
            long currentTimestamp = System.currentTimeMillis();
            if (isChecked) {
                // Notification enabledonStop
                editor.putLong("notification_on_time", currentTimestamp);
                // Delete messages during notification off period
                deleteMessagesDuringNotificationOffPeriod(currentTimestamp);
            } else {

                editor.putLong("notification_off_time", currentTimestamp);
            }

            editor.apply();
        });
        // Reference to the reset_password redirecting button
        resetPasswordButton = findViewById(R.id.reset_password);
        resetPasswordButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                FirebaseAuth mAuth;
                // Initialize Firebase Auth
                mAuth = FirebaseAuth.getInstance();
                // Check if user is already logged in
                FirebaseUser currentUser = mAuth.getCurrentUser();
                // Finish the current activity and return to the RunSummaryActivity page
                Intent intent = new Intent(Settings.this, ResetPassword.class);
                intent.putExtra("EMAIL", currentUser.getEmail());
                startActivity(intent);
            }
        });

        // Reference to the personal info redirecting button
        editPersonalInfoButton = findViewById(R.id.pesonal_info);
        editPersonalInfoButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                // Finish the current activity and return to the RunSummaryActivity page
                Intent intent = new Intent(Settings.this, EditPersonalInfo.class);
                intent.putExtra("userId", userId);
                startActivity(intent);
            }
        });

        userGuideLinesButton = findViewById(R.id.user_guide_lines);
        userGuideLinesButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                // Finish the current activity and return to the RunSummaryActivity page
                Intent intent = new Intent(Settings.this, UserGuideLinesActivity.class);
                startActivity(intent);
            }
        });


        // Reference to the logout button
        logOutButton = findViewById(R.id.bt_logout_settings);

        // Handle logout button click
        logOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Inflate the custom logout dialog layout
                LayoutInflater inflater = LayoutInflater.from(Settings.this);
                View dialogView = inflater.inflate(R.layout.dialog_logout, null);

                // Find buttons in the custom dialog layout
                Button positiveButton = dialogView.findViewById(R.id.positive_button);
                Button negativeButton = dialogView.findViewById(R.id.negative_button);

                // Create the AlertDialog with the custom view
                AlertDialog dialog = new AlertDialog.Builder(Settings.this)
                        .setView(dialogView)
                        .setCancelable(false)  // Prevent dismissing by clicking outside
                        .create();

                dialog.show();

                // Handle Confirm button click
                positiveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Perform logout actions
                        performLogout();
                        FirebaseAuth.getInstance().signOut();

                        // Clear activity history and navigate to Login screen
                        Intent intent = new Intent(Settings.this, Login.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();  // Close current activity

                        // Dismiss the dialog
                        dialog.dismiss();
                    }
                });

                // Handle Cancel button click
                negativeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Dismiss the dialog when Cancel is clicked
                        dialog.dismiss();
                    }
                });

                // Optional: Customize dialog window properties if needed
                Window window = dialog.getWindow();
                if (window != null) {
                    WindowManager.LayoutParams layoutParams = window.getAttributes();
                    layoutParams.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.8);  // Set dialog width
                    layoutParams.dimAmount = 0.9f;  // Background dimming effect
                    window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                    window.setAttributes(layoutParams);
                }
            }
        });


        // Deactivate logic
        deactivateButton = findViewById(R.id.bt_deactivate);
        // Handle logout button click
        deactivateButton.setOnClickListener(v -> {
            // Inflate the custom deactivate dialog layout
            LayoutInflater inflater = LayoutInflater.from(Settings.this);
            View dialogView = inflater.inflate(R.layout.dialog_deactivate, null);

            // Find buttons in the custom dialog layout
            Button positiveButton = dialogView.findViewById(R.id.positive_button);
            Button negativeButton = dialogView.findViewById(R.id.negative_button);

            // Create and show the AlertDialog with the custom view
            AlertDialog dialog = new AlertDialog.Builder(Settings.this)
                    .setView(dialogView)
                    .setCancelable(false)  // Prevent dismissing by clicking outside
                    .create();

            dialog.show();

            // Handle Confirm button click
            positiveButton.setOnClickListener(v1 -> {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                if (user != null) {
                    // Show password input dialog for re-authentication
                    showPasswordInputDialog(user);
                }

                // Dismiss the dialog
                dialog.dismiss();
            });

            // Handle Cancel button click
            negativeButton.setOnClickListener(v12 -> dialog.dismiss());

            // Optional: Customize dialog window properties if needed
            Window window = dialog.getWindow();
            if (window != null) {
                WindowManager.LayoutParams layoutParams = window.getAttributes();
                layoutParams.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.8);  // Set dialog width
                layoutParams.dimAmount = 0.9f;  // Background dimming effect
                window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                window.setAttributes(layoutParams);
            }
        });




        locationSwitch = findViewById(R.id.location_switch);
        updateLocationSwitchState();

        locationSwitch.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                isUserInitiatedSwitchChange = true;
            }
            return false;
        });

        locationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isUserInitiatedSwitchChange) {
                isUserInitiatedSwitchChange = false;
                if (isChecked) {
                    requestLocationPermissionWithDialog();
                } else {
                    showPermissionRevocationDialog();
                }
            }
        });
    }
    private void deleteMessagesDuringNotificationOffPeriod(long notificationOnTime) {
        // Get the last notification off time
        long notificationOffTime = sharedPref.getLong("notification_off_time", 0);

        if (notificationOffTime == 0) {
            // No previous off time, nothing to delete
            return;
        }

        if (userId == null) {
            Log.e("Settings", "userId is null. Cannot delete messages.");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Create a query to find messages during the off period
        Query query = db.collection("message")
                .whereEqualTo("updateUserId", userId)
                .whereGreaterThanOrEqualTo("timestamp", notificationOffTime)
                .whereLessThan("timestamp", notificationOnTime);

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                WriteBatch batch = db.batch();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    batch.delete(document.getReference());
                }

                // Commit the batch
                batch.commit()
                        .addOnSuccessListener(aVoid -> {
                            Log.d("Settings", "Messages during notification off period deleted successfully.");
                        })
                        .addOnFailureListener(e -> {
                            Log.e("Settings", "Failed to delete messages: ", e);
                        });
            } else {
                Log.e("Settings", "Error getting messages to delete: ", task.getException());
            }
        });
    }
    // ReEnter password to confirm deactivate
    private void showPasswordInputDialog(FirebaseUser user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();

        // Inflate the custom password input dialog layout
        View dialogView = inflater.inflate(R.layout.dialog_password_input, null);
        builder.setView(dialogView);
        builder.setCancelable(false);  // Disable dismissing the dialog by clicking outside

        // Create and show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();

        // Customize the dialog window properties
        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams layoutParams = window.getAttributes();
            layoutParams.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.8);

            int offsetInDp = 100;
            float scale = getResources().getDisplayMetrics().density;
            layoutParams.y = (int) (offsetInDp * scale + 0.5f);  // Apply Y-offset

            layoutParams.dimAmount = 0.9f;  // Dim the background
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setAttributes(layoutParams);
        }

        // Find and initialize UI elements in the custom dialog view
        TextInputEditText passwordEditText = dialogView.findViewById(R.id.passwordEditText);
        Button positiveButton = dialogView.findViewById(R.id.positive_button);
        Button negativeButton = dialogView.findViewById(R.id.negative_button);

        // Handle Confirm button click
        positiveButton.setOnClickListener(v -> {
            String password = passwordEditText.getText().toString().trim();
            if (!password.isEmpty()) {
                // Perform re-authentication and deletion logic
                reauthenticateAndDelete(user, password);
                dialog.dismiss();  // Dismiss the dialog
            } else {
                // Show a toast if the password field is empty
                Toast.makeText(this, "Password cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        // Handle Cancel button click
        negativeButton.setOnClickListener(v -> dialog.dismiss());  // Dismiss the dialog
    }

    // Recheck user authentication
    private void reauthenticateAndDelete(FirebaseUser user, String password) {
        // Get the user's email and create credentials with the provided password
        String email = user.getEmail();
        if (email == null) {
            Toast.makeText(this, "Email not found. Cannot authenticate.", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(email, password);

        // Reauthenticate the user
        user.reauthenticate(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("Reauthentication", "User reauthenticated.");
                        // If reauthentication is successful, proceed to delete the user
                        deleteUserAuth(user);
                        deleteUser(user);
                    } else {
                        Log.e("Reauthentication", "Failed: " + task.getException().getMessage());
                        Toast.makeText(this, "Reauthentication failed. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Delete user from auth
    private void deleteUserAuth(FirebaseUser user) {
        // delete user auth
        user.delete().addOnCompleteListener(deleteTask -> {
            if (deleteTask.isSuccessful()) {
                Log.d("DeleteUser", "User account deleted.");
                Toast.makeText(this, "Account deleted, hope to see you again!", Toast.LENGTH_SHORT).show();
                // navigate to login page
                Intent intent = new Intent(this, Login.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });
    }

    // Delete user firestore
    private void deleteUser(FirebaseUser user) {
        String userId = user.getUid();  // Get the user's UID

        // Delete the user from Firestore's 'users' collection
        FirebaseFirestore.getInstance().collection("users").document(userId)
                .delete()
                .addOnCompleteListener(deleteTask -> {
                    if (deleteTask.isSuccessful()) {
                        Log.d("Firestore", "User document deleted from Firestore.");
                        // Now delete the Firebase Authentication user
                        deleteUserAuth(user);
                    } else {
                        Log.e("Firestore", "Failed to delete user document: " + deleteTask.getException().getMessage());
                        Toast.makeText(this, "Failed to delete user data.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Function to perform the logout action
    private void performLogout() {
        // Clearing session, navigating to login screen, etc.
    }

    /**
     * Updates the location switch state based on the current permission status.
     */
    private void updateLocationSwitchState() {
        boolean isLocationPermissionGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (locationSwitch != null) {
            locationSwitch.setOnCheckedChangeListener(null);
            locationSwitch.setChecked(isLocationPermissionGranted);
            locationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isUserInitiatedSwitchChange) {
                    isUserInitiatedSwitchChange = false;
                    if (isChecked) {
                        requestLocationPermissionWithDialog();
                    } else {
                        showPermissionRevocationDialog();
                    }
                }
            });
        }
    }

    /**
     * Requests location permission using a custom dialog.
     */
    private void requestLocationPermissionWithDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.custom_dialog, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialog);
        builder.setView(dialogView);
        builder.setCancelable(false);

        AlertDialog dialog = builder.create();

        Button btnAllow = dialogView.findViewById(R.id.btn_positive);
        Button btnCancel = dialogView.findViewById(R.id.btn_negative);
        TextView title = dialogView.findViewById(R.id.dialog_title);
        TextView message = dialogView.findViewById(R.id.dialog_message);

        title.setText("Location Permission Needed");
        message.setText("This app requires location access to provide location-based features.");

        btnAllow.setText("Allow");
        btnCancel.setText("Cancel");

        btnAllow.setOnClickListener(v -> {
            ActivityCompat.requestPermissions(Settings.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> {
            updateLocationSwitchState();
            dialog.dismiss();
        });

        dialog.show();

        // Adjust dialog window attributes
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            params.gravity = Gravity.BOTTOM;
            window.setAttributes(params);
            window.getAttributes().windowAnimations = R.style.DialogAnimation;
        }
    }

    /**
     * Guides the user to revoke location permission manually via a custom dialog.
     */
    private void showPermissionRevocationDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.custom_dialog, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialog);
        builder.setView(dialogView);
        builder.setCancelable(false);

        AlertDialog dialog = builder.create();

        Button btnOpenSettings = dialogView.findViewById(R.id.btn_positive);
        Button btnCancel = dialogView.findViewById(R.id.btn_negative);
        TextView title = dialogView.findViewById(R.id.dialog_title);
        TextView message = dialogView.findViewById(R.id.dialog_message);

        title.setText("Revoke Location Permission");
        message.setText("To disable location access, please revoke the permission in app settings.");

        btnOpenSettings.setText("Open Settings");
        btnCancel.setText("Cancel");

        btnOpenSettings.setOnClickListener(v -> {
            openAppSettings();
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
            updateLocationSwitchState();
        });

        dialog.show();

        // Adjust dialog window attributes
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            params.gravity = Gravity.BOTTOM;
            window.setAttributes(params);
            window.getAttributes().windowAnimations = R.style.DialogAnimation;
        }
    }

    /**
     * Displays a dialog when location permission has been permanently denied.
     */
    private void showPermissionDeniedForeverDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.custom_dialog, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialog);
        builder.setView(dialogView);
        builder.setCancelable(false);

        AlertDialog dialog = builder.create();

        Button btnOpenSettings = dialogView.findViewById(R.id.btn_positive);
        Button btnCancel = dialogView.findViewById(R.id.btn_negative);
        TextView title = dialogView.findViewById(R.id.dialog_title);
        TextView message = dialogView.findViewById(R.id.dialog_message);

        title.setText("Permission Denied Permanently");
        message.setText("Location permission has been denied permanently. Please enable it in app settings.");

        btnOpenSettings.setText("Open Settings");
        btnCancel.setText("Cancel");

        btnOpenSettings.setOnClickListener(v -> {
            openAppSettings();
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
            updateLocationSwitchState();
        });

        dialog.show();

        // Adjust dialog window attributes
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            params.gravity = Gravity.BOTTOM;
            window.setAttributes(params);
            window.getAttributes().windowAnimations = R.style.DialogAnimation;
        }
    }

    /**
     * Opens the app's settings page for the user to adjust permissions.
     */
    private void openAppSettings() {
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Update the switch state when returning from settings
        updateLocationSwitchState();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            // Check if permission was granted
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                updateLocationSwitchState();
            } else {
                boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                        this, Manifest.permission.ACCESS_FINE_LOCATION);
                if (!showRationale) {
                    // User selected "Don't ask again"
                    showPermissionDeniedForeverDialog();
                } else {
                    // Permission denied but not permanently
                    Toast.makeText(this,
                            "Location permission denied. Some features may not work properly.",
                            Toast.LENGTH_LONG).show();
                    updateLocationSwitchState();
                }
            }
        }
    }
}