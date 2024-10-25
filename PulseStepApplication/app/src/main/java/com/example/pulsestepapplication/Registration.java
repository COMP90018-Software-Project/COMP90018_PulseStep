package com.example.pulsestepapplication;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Patterns;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;

public class Registration extends AppCompatActivity {

    private TextInputLayout fullNameInputLayout, emailInputLayout;
    private TextInputEditText fullNameEditText, emailEditText;
    private Button continueButton;
    private ImageButton backButton;
    private TextView signInText;
    private FirebaseAuth mAuth;

    private boolean emailVerificationSent = false; // Track if email is sent
    private final String passVerificationMessage = "Continue";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        mAuth = FirebaseAuth.getInstance();

        fullNameEditText = findViewById(R.id.fullNameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        continueButton = findViewById(R.id.continueButton);
        backButton = findViewById(R.id.backButton);
        signInText = findViewById(R.id.signInText);
        fullNameInputLayout = findViewById(R.id.fullNameInputLayout);
        emailInputLayout = findViewById(R.id.emailInputLayout);

        // Back button logic
        backButton.setOnClickListener(view -> finish());

        // Set "Sign In" text style
        String fullText = "Already have an account? Sign In";
        SpannableString spannableString = new SpannableString(fullText);
        int startIndex = fullText.indexOf("Sign In");
        int endIndex = startIndex + "Sign In".length();
        spannableString.setSpan(new ForegroundColorSpan(getResources().getColor(android.R.color.black)),
                startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        signInText.setText(spannableString);

        // Sign In click event
        signInText.setOnClickListener(view -> {
            Intent intent = new Intent(Registration.this, Login.class);
            startActivity(intent);
        });

        // Continue button click event
        continueButton.setOnClickListener(view -> {
            if (emailVerificationSent) {
                // If email verification has been sent, check if verified
                String fullName = fullNameEditText.getText().toString();
                String email = emailEditText.getText().toString();
                proceedIfEmailVerified(fullName, email);
            } else {
                // Otherwise, validate inputs and send verification email
                validateAndRegisterUser();
                continueButton.setText(passVerificationMessage);
            }
        });
    }

    // Method to validate inputs and register the user
    private void validateAndRegisterUser() {
        String fullName = fullNameEditText.getText().toString();
        String email = emailEditText.getText().toString();

        // Validate full name
        if (fullName.isEmpty()) {
            fullNameInputLayout.setError("Full name cannot be empty");
            return;
        } else if (!fullName.matches("[a-zA-Z ]+")) {
            fullNameInputLayout.setError("Full name can only contain letters and spaces");
            return;
        } else {
            fullNameInputLayout.setError(null);
        }

        // Validate email
        if (email.isEmpty()) {
            emailInputLayout.setError("Enter your email address");
            return;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.setError("Invalid email format");
            return;
        } else {
            emailInputLayout.setError(null);
        }

        // Check if the email is already registered
        mAuth.fetchSignInMethodsForEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        boolean isEmailRegistered = task.getResult().getSignInMethods().size() > 0;
                        if (isEmailRegistered) {
                            Toast.makeText(Registration.this,
                                    "Email already registered. Please sign in.",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            // Register the user and send verification email
                            registerUser(fullName, email);
                        }
                    } else {
                        Toast.makeText(Registration.this,
                                "Error checking email: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Method to register the user and send a verification email
    private void registerUser(String fullName, String email) {
        mAuth.createUserWithEmailAndPassword(email, "defaultPassword123")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            sendVerificationEmail(user);
                        }
                    } else {
                        Toast.makeText(Registration.this,
                                "Registration failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Method to send verification email
    private void sendVerificationEmail(FirebaseUser user) {
        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        emailVerificationSent = true; // Set flag to true
                        Toast.makeText(Registration.this,
                                "Verification email sent. Please verify before continuing.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(Registration.this,
                                "Failed to send verification email.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Method to check if the email is verified before proceeding
    private void proceedIfEmailVerified(String fullName, String email) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.reload().addOnCompleteListener(task -> {
                if (user.isEmailVerified()) {
                    // Navigate to the SetPassword activity
                    Intent intent = new Intent(Registration.this, SetPassword.class);
                    intent.putExtra("FULL_NAME", fullName);
                    intent.putExtra("EMAIL", email);
                    startActivity(intent);
                } else {
                    Toast.makeText(Registration.this,
                            "Please verify your email first. If you haven't received it, check your email address.",
                            Toast.LENGTH_LONG).show();
                }
            });
        } else {
            Toast.makeText(Registration.this, "User not found. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }
}