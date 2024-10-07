package com.example.pulsestepapplication;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class PersonalDetails extends AppCompatActivity {

    private TextView birthdayTextView;
    private EditText heightEditText, weightEditText;
    private RadioButton maleRadioButton, femaleRadioButton, otherRadioButton;
    private Button finishButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ProgressDialog progressDialog; // ProgressDialog to show saving state

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_details);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize UI elements
        birthdayTextView = findViewById(R.id.birthdayTextView);
        heightEditText = findViewById(R.id.heightEditText);
        weightEditText = findViewById(R.id.weightEditText);
        maleRadioButton = findViewById(R.id.maleRadioButton);
        femaleRadioButton = findViewById(R.id.femaleRadioButton);
        otherRadioButton = findViewById(R.id.otherRadioButton);
        finishButton = findViewById(R.id.finishButton);

        // Initialize ProgressDialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Saving details...");
        progressDialog.setCancelable(false);

        // Set up DatePickerDialog for birthday selection
        birthdayTextView.setOnClickListener(v -> {
            // Get the current date
            final Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            // Create a DatePickerDialog with year and month scroll enabled
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    PersonalDetails.this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        // Set the selected date to the TextView
                        String selectedDate = (selectedMonth + 1) + "/" + selectedDay + "/" + selectedYear;
                        birthdayTextView.setText(selectedDate);
                    },
                    year, month, day);
            datePickerDialog.show();
        });

        // Get user UID
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish(); // Exit if user is not logged in
            return;
        }

        String userUID = currentUser.getUid();

        // Get passed fullName and email
        String fullName = getIntent().getStringExtra("FULL_NAME");
        String email = getIntent().getStringExtra("EMAIL");

        // Set Finish button click listener
        finishButton.setOnClickListener(view -> {
            // Get user input data
            String birthday = birthdayTextView.getText().toString();
            String heightStr = heightEditText.getText().toString();
            String weightStr = weightEditText.getText().toString();
            boolean isMale = maleRadioButton.isChecked();
            boolean isFemale = femaleRadioButton.isChecked();
            boolean isOther = otherRadioButton.isChecked();

            boolean hasError = false;

            // Validate birthday
            if (birthday.isEmpty()) {
                birthdayTextView.setError("Please select your birthday.");
                hasError = true;
            } else {
                birthdayTextView.setError(null); // Clear error
            }

            // Validate height
            if (heightStr.isEmpty()) {
                heightEditText.setError("Please enter your height.");
                hasError = true;
            } else {
                try {
                    float height = Float.parseFloat(heightStr);
                    if (height <= 0) {
                        heightEditText.setError("Height must be a positive number.");
                        hasError = true;
                    } else {
                        heightEditText.setError(null); // Clear error
                    }
                } catch (NumberFormatException e) {
                    heightEditText.setError("Please enter a valid number for height.");
                    hasError = true;
                }
            }

            // Validate weight
            if (weightStr.isEmpty()) {
                weightEditText.setError("Please enter your weight.");
                hasError = true;
            } else {
                try {
                    float weight = Float.parseFloat(weightStr);
                    if (weight <= 0) {
                        weightEditText.setError("Weight must be a positive number.");
                        hasError = true;
                    } else {
                        weightEditText.setError(null); // Clear error
                    }
                } catch (NumberFormatException e) {
                    weightEditText.setError("Please enter a valid number for weight.");
                    hasError = true;
                }
            }

            // Validate gender
            if (!isMale && !isFemale && !isOther) {
                Toast.makeText(this, "Please select your gender.", Toast.LENGTH_SHORT).show();
                hasError = true;
            }

            // If there's any error, return early
            if (hasError) {
                return;
            }

            // Determine gender
            String gender;
            if (isMale) {
                gender = "Male";
            } else if (isFemale) {
                gender = "Female";
            } else {
                gender = "Other";
            }

            boolean appleHealthEnabled = false; // Change as necessary

            // Set default avatar URL based on gender
            String avatarUrl;
            if (isMale) {
                avatarUrl = "male_default_avatar.png";
            } else if (isFemale) {
                avatarUrl = "female_default_avatar.png";
            } else {
                avatarUrl = "default_avatar.png"; // Default avatar for "Other"
            }

            // Show ProgressDialog before starting the save operation
            progressDialog.show();

            // Create user data map
            Map<String, Object> userDetails = new HashMap<>();
            userDetails.put("fullName", fullName);
            userDetails.put("email", email);
            userDetails.put("birthday", birthday);
            userDetails.put("height", heightStr);
            userDetails.put("weight", weightStr);
            userDetails.put("gender", gender);
            userDetails.put("appleHealthEnabled", appleHealthEnabled);
            userDetails.put("avatarUrl", avatarUrl);

            // Save data to Firestore
            db.collection("users").document(userUID)
                    .set(userDetails)
                    .addOnCompleteListener(task -> {
                        // Hide ProgressDialog after operation completes
                        progressDialog.dismiss();

                        if (task.isSuccessful()) {
                            Toast.makeText(PersonalDetails.this, "Details saved successfully!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(PersonalDetails.this, Login.class);
                            startActivity(intent);
                        } else {
                            Toast.makeText(PersonalDetails.this, "Failed to save details.", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }
}
