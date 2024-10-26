package com.example.pulsestepapplication;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditPersonalInfo extends AppCompatActivity {
    private TextView birthdayTextView;
    private EditText fullNameEditText, heightEditText, weightEditText;
    private RadioButton maleRadioButton, femaleRadioButton, otherRadioButton;
    private ImageView backButton;
    private Button finishButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ProgressDialog progressDialog; // ProgressDialog to show saving state
    private String userId;

    private final String popUpMessage = "Are you sure you want to leave this page? Any unsaved changes will be lost.";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_details);

        db = FirebaseFirestore.getInstance();

        // 获取从 ProfileFragment 传递的 userId
        Intent intent = getIntent();
        userId = intent.getStringExtra("userId");

        // Initialize UI elements
        birthdayTextView = findViewById(R.id.birthdayTextView);
        fullNameEditText = findViewById(R.id.fullNameEditText);
        heightEditText = findViewById(R.id.heightEditText);
        weightEditText = findViewById(R.id.weightEditText);
        maleRadioButton = findViewById(R.id.maleRadioButton);
        femaleRadioButton = findViewById(R.id.femaleRadioButton);
        otherRadioButton = findViewById(R.id.otherRadioButton);
        finishButton = findViewById(R.id.finishButton);
        backButton = findViewById(R.id.back_button);

        backButton.setOnClickListener(v -> popUpConfirmDialog());

        // Initialize ProgressDialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Saving details...");
        progressDialog.setCancelable(false);

        loadUserData();


        // Set Finish button click listener
        finishButton.setOnClickListener(view -> {
            // Get user input data
            String fullName = fullNameEditText.getText().toString();
            String birthday = birthdayTextView.getText().toString();
            String heightStr = heightEditText.getText().toString();
            String weightStr = weightEditText.getText().toString();
            boolean isMale = maleRadioButton.isChecked();
            boolean isFemale = femaleRadioButton.isChecked();
            boolean isOther = otherRadioButton.isChecked();

            boolean hasError = false;

            // Validate full name
            if (fullName.isEmpty()) {
                fullNameEditText.setError("Full name cannot be empty");
                return;
            } else if (!fullName.matches("[a-zA-Z ]+")) {
                fullNameEditText.setError("Full name can only contain letters and spaces");
                return;
            } else if (fullName.length() > 15) {  // Adjust the maximum length as desired
                fullNameEditText.setError("Full name cannot exceed 30 characters");
                return;
            } else {
                fullNameEditText.setError(null);
            }

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
                    } else if (height < 50 || height > 350) {  // Set height range as needed
                        heightEditText.setError("Height must be between 50 cm and 350 cm.");
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
                    } else if (weight < 20 || weight > 400) {  // Set weight range as needed
                        weightEditText.setError("Weight must be between 20 kg and 400 kg.");
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


            // Show ProgressDialog before starting the save operation
            progressDialog.show();

            // Create user data map
            Map<String, Object> userDetails = new HashMap<>();
            userDetails.put("fullName", fullName);
            userDetails.put("birthday", birthday);
            userDetails.put("height", heightStr);
            userDetails.put("weight", weightStr);
            userDetails.put("gender", gender);

            // Update data in Firestore using SetOptions.merge() to merge fields with existing data
            db.collection("users").document(userId)
                    .set(userDetails, SetOptions.merge())  // Merge new data with existing fields
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(EditPersonalInfo.this, "Details updated successfully", Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(EditPersonalInfo.this, "Error updating details", Toast.LENGTH_SHORT).show();
                        Log.e("EditPersonalInfo", "Error updating details: ", e);
                        progressDialog.dismiss();
                    });
        });
    }

    private void loadUserData() {
        // 从 Firestore 获取用户数据
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // 获取用户的详细信息
                        String fullName = documentSnapshot.getString("fullName");
                        String birthday = documentSnapshot.getString("birthday");
                        String height = documentSnapshot.getString("height");
                        String weight = documentSnapshot.getString("weight");
                        String gender = documentSnapshot.getString("gender");

                        // 设置 UI 元素的值
                        fullNameEditText.setText(fullName);
                        birthdayTextView.setText(birthday);
                        heightEditText.setText(height);
                        weightEditText.setText(weight);

                        // 将 birthday 日期解析为年、月、日
                        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
                        Calendar calendar = Calendar.getInstance();
                        try {
                            Date date = dateFormat.parse(birthday); // 解析从数据库获取的日期
                            if (date != null) {
                                calendar.setTime(date); // 设置日期到 Calendar
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        // 获取 year, month, day
                        int year = calendar.get(Calendar.YEAR);
                        int month = calendar.get(Calendar.MONTH); // 注意，Calendar.MONTH 是 0-based
                        int day = calendar.get(Calendar.DAY_OF_MONTH);

                        // Set up DatePickerDialog with the selected birthday from the database
                        birthdayTextView.setOnClickListener(v -> {
                            DatePickerDialog datePickerDialog = new DatePickerDialog(
                                    EditPersonalInfo.this,
                                    (view, selectedYear, selectedMonth, selectedDay) -> {
                                        // 将用户选择的日期设置为 TextView
                                        String selectedDate = (selectedMonth + 1) + "/" + selectedDay + "/" + selectedYear;
                                        birthdayTextView.setText(selectedDate);
                                    },
                                    year, month, day); // 传递解析出来的年、月、日作为初始日期
                            datePickerDialog.show();
                        });

                        // 设置性别单选按钮
                        if (gender != null) {
                            if (gender.equalsIgnoreCase("Male")) {
                                maleRadioButton.setChecked(true);
                            } else if (gender.equalsIgnoreCase("Female")) {
                                femaleRadioButton.setChecked(true);
                            } else {
                                otherRadioButton.setChecked(true);
                            }
                        }

                    } else {
                        Toast.makeText(EditPersonalInfo.this, "User data not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EditPersonalInfo.this, "Error loading data", Toast.LENGTH_SHORT).show();
                    Log.e("EditPersonalInfo", "Error loading details: ", e);
                });
    }

    private void popUpConfirmDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_custom, null);
        builder.setView(dialogView);
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();

        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams layoutParams = window.getAttributes();
            layoutParams.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.8);

            int offsetInDp = 100;
            float scale = getResources().getDisplayMetrics().density;
            layoutParams.y = (int) (offsetInDp * scale + 0.5f);
            layoutParams.dimAmount = 0.9f;
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

            window.setAttributes(layoutParams);
        }

        // Find the TextView in the dialog and set the dynamic message
        TextView dialogMessage = dialogView.findViewById(R.id.dialog_message);
        dialogMessage.setText(popUpMessage);  // Set the custom message

        Button positiveButton = dialogView.findViewById(R.id.positive_button);
        Button negativeButton = dialogView.findViewById(R.id.negative_button);

        positiveButton.setOnClickListener(v -> {
            Intent intent = new Intent();
            setResult(RESULT_OK, intent);
            finish();
            dialog.dismiss();
        });

        negativeButton.setOnClickListener(v -> dialog.dismiss());
    }

}
