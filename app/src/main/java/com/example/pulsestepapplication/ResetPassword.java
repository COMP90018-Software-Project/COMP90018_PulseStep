package com.example.pulsestepapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ResetPassword extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);
        // 初始化 Firebase Auth
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 获取从上一个页面传递的电子邮件
        String email = getIntent().getStringExtra("EMAIL");

        // Find the back button by its ID
        ImageView backButton = findViewById(R.id.back_button_change_password_page);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Load the first fragment (PasswordResetFragment)
        if (savedInstanceState == null) {
            PasswordResetConfirmFragment confirmFragment = new PasswordResetConfirmFragment();
            loadFragment(confirmFragment);
        }

        // After confirming, load the SetNewPasswordFragment with email
        SetNewPasswordFragment setNewPasswordFragment = new SetNewPasswordFragment();
        Bundle bundle = new Bundle();
        bundle.putString("EMAIL", email);  // Pass email to the fragment
        setNewPasswordFragment.setArguments(bundle);

        // Load SetNewPasswordFragment with the email passed as argument
        loadFragment(setNewPasswordFragment);
    }

    // Method to load a fragment into the container
    public void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null); // Allows the user to go back
        transaction.commit();
    }
}
