package com.example.pulsestepapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.Button;
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
import com.google.firebase.auth.FirebaseAuth;

public class Settings extends AppCompatActivity {

    private ImageView backButton;
    private Button logOutButton;
    private LinearLayout resetPasswordButton;

    private LinearLayout editPersonalInfoButton;

    private MaterialSwitch locationSwitch;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private boolean isUserInitiatedSwitchChange = false;



    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        // 获取从 ProfileFragment 传递的 userId
        Intent intent = getIntent();
        String userId = intent.getStringExtra("userId");

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

        // Reference to the reset_password redirecting button
        resetPasswordButton = findViewById(R.id.reset_password);
        resetPasswordButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                // Finish the current activity and return to the RunSummaryActivity page
                Intent intent = new Intent(Settings.this, ResetPassword.class);
                intent.putExtra("userId", userId);
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
                                FirebaseAuth.getInstance().signOut();
                                startActivity(new Intent(Settings.this, StartActivity.class));
                                finish(); // Close the main activity
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