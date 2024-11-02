package com.example.pulsestepapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;

public class SetNewPasswordFragment extends Fragment {

    // Firebase Auth instance
    private FirebaseAuth mAuth;
    private String email;  // Email passed from ResetPassword activity

    public SetNewPasswordFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_set_new_password, container, false);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Get the email passed from the ResetPassword activity
        if (getActivity() != null && getArguments() != null) {
            email = getArguments().getString("EMAIL");
        }

        // Find the update button
        Button updateButton = view.findViewById(R.id.bt_update);

        // Set click listener for the update button
        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (email != null && !email.isEmpty()) {
                    // Call Firebase method to send password reset email
                    resetPassword(email);
                } else {
                    // Show error if email is null or empty
                    Toast.makeText(getActivity(), "Invalid email address", Toast.LENGTH_SHORT).show();
                }
            }
        });

        return view;
    }

    // Method to send password reset email using Firebase
    private void resetPassword(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // If email is sent successfully, show success message and load the success fragment
                        Toast.makeText(getActivity(), "Password reset email sent. Check your inbox.", Toast.LENGTH_LONG).show();
                        ((ResetPassword) getActivity()).loadFragment(new SuccessFragment());
                    } else {
                        // If there is an error (e.g., invalid email), show an error message
                        Toast.makeText(getActivity(), "Failed to send reset email. Please try again.", Toast.LENGTH_LONG).show();
                    }
                });
    }
}
