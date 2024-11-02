package com.example.pulsestepapplication;


import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;


import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.firestore.FirebaseFirestore;


public class Password extends AppCompatActivity {


    private TextInputEditText passwordEditText;
    private Button signInButton;
    private ImageButton backButton;
    private TextView createAccountText;
    private TextView forgotPasswordText;
    private FirebaseAuth mAuth; // Firebase Authentication 实例
    private String email; // 保存从上一个页面传递的电子邮件
    private ProgressDialog progressDialog; // ProgressDialog 用于显示加载框
    private FirebaseFirestore db;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password);


        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        // Get a Firestore instance
        db = FirebaseFirestore.getInstance();


        passwordEditText = findViewById(R.id.passwordEditText);
        signInButton = findViewById(R.id.signInButton);
        backButton = findViewById(R.id.backButton);
        createAccountText = findViewById(R.id.createAccountText);
        forgotPasswordText = findViewById(R.id.forgotPasswordText);


        // Initialize ProgressDialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Signing in...");
        progressDialog.setCancelable(false);


        // Get the email passed from the previous page
        email = getIntent().getStringExtra("EMAIL");


        // Set the text style of the "Create Account" section
        String fullText = "Don't have an account? Create Account";
        SpannableString spannableString = new SpannableString(fullText);
        int startIndex = fullText.indexOf("Create Account");
        int endIndex = startIndex + "Create Account".length();


        // Set bold and black
        spannableString.setSpan(new ForegroundColorSpan(getResources().getColor(android.R.color.black)), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        createAccountText.setText(spannableString);


        // Set the text style of "Forgot password?"
        String forgotText = "Forgot password?";
        SpannableString forgotSpannable = new SpannableString(forgotText);
        forgotSpannable.setSpan(new ForegroundColorSpan(getResources().getColor(android.R.color.black)), 0, forgotText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        forgotPasswordText.setText(forgotSpannable);


        // Set the click event of the back button
        backButton.setOnClickListener(view -> finish());


        // Set up the click event for the "Sign In" button
        signInButton.setOnClickListener(view -> {
            String password = passwordEditText.getText().toString();
            if (password.isEmpty()) {
                passwordEditText.setError("Enter your password");
            } else {
                // Show loading box
                progressDialog.show();
                signInUser(email, password); // Calling the login method
            }
        });


        // Set up a click event for "Create Account"
        createAccountText.setOnClickListener(view -> {
            Intent intent = new Intent(Password.this, Registration.class);
            startActivity(intent);
        });


        // Set up a click event for "Forgot password?"
        forgotPasswordText.setOnClickListener(view -> {
            Intent intent = new Intent(Password.this, ResetPassword.class);
            intent.putExtra("EMAIL", email); // Pass email to next page
            startActivity(intent);
        });
    }


    //Login Method
    private void signInUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    // Hide loading box
                    progressDialog.dismiss();


                    if (task.isSuccessful()) {
                        // Login successful
                        Toast.makeText(Password.this, "Login successful", Toast.LENGTH_SHORT).show();
                        // Get the UID of the current user
                        String userId = mAuth.getCurrentUser().getUid();
                        // Get user information from Firestore
                        db.collection("users").document(userId).get()
                                .addOnSuccessListener(documentSnapshot -> {
                                    if (documentSnapshot.exists()) {
                                        // Get the user's full name
                                        String fullName = documentSnapshot.getString("fullName");
                                        // Display login success information and full name
                                        Toast.makeText(Password.this, "Login successful. Welcome " + fullName, Toast.LENGTH_SHORT).show();
                                        // Jump to the main page or other pages
                                        Intent intent = new Intent(Password.this, MainActivity.class);
                                        intent.putExtra("FULL_NAME", fullName); // Passing full name to next page
                                        intent.putExtra("USER_ID", userId); // Passing the UID to the next page
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        finish();
                                    }
                                });
                    } else {
                        // Login Failed
                        if (task.getException() instanceof FirebaseAuthInvalidUserException) {
                            passwordEditText.setError("No account found with this email.");
                        } else if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                            passwordEditText.setError("Incorrect password.");
                        } else {
                            Toast.makeText(Password.this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
