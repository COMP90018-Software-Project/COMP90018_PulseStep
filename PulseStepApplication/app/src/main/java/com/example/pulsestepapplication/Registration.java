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

public class Registration extends AppCompatActivity {

    private TextInputLayout fullNameInputLayout, emailInputLayout;
    private TextInputEditText fullNameEditText, emailEditText;
    private Button continueButton;
    private ImageButton backButton;
    private TextView signInText;
    private FirebaseAuth mAuth;

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

        // 设置返回按钮的点击事件
        backButton.setOnClickListener(view -> finish());

        // 设置 "Sign In" 部分的文本样式
        String fullText = "Already have an account? Sign In";
        SpannableString spannableString = new SpannableString(fullText);
        int startIndex = fullText.indexOf("Sign In");
        int endIndex = startIndex + "Sign In".length();

        spannableString.setSpan(new ForegroundColorSpan(getResources().getColor(android.R.color.black)), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        signInText.setText(spannableString);

        // Sign In click event
        signInText.setOnClickListener(view -> {
            Intent intent = new Intent(Registration.this, Login.class);
            startActivity(intent);
        });

        // Continue button click event
        continueButton.setOnClickListener(view -> {
            String fullName = fullNameEditText.getText().toString();
            String email = emailEditText.getText().toString();

            // 检查全名是否为空或者不符合格式
            if (fullName.isEmpty()) {
                fullNameInputLayout.setError("Full name cannot be empty");
                return;
            } else if (!fullName.matches("[a-zA-Z ]+")) { // 只允许字母和空格
                fullNameInputLayout.setError("Full name can only contain letters and spaces");
                return;
            } else {
                fullNameInputLayout.setError(null); // 清除错误
            }

            // 检查邮箱是否为空或格式不正确
            if (email.isEmpty()) {
                emailInputLayout.setError("Enter your email address");
                return;
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailInputLayout.setError("Invalid email format");
                return;
            } else {
                emailInputLayout.setError(null); // 清除错误
            }

            // 检查邮箱是否已经在 Firebase 中注册
            mAuth.fetchSignInMethodsForEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            boolean isEmailRegistered = task.getResult().getSignInMethods().size() > 0;
                            // 跳转到设置密码页面
                            Intent intent = new Intent(Registration.this, SetPassword.class);
                            intent.putExtra("FULL_NAME", fullName);
                            intent.putExtra("EMAIL", email);
                            startActivity(intent);
                        } else {
                            // 如果发生错误，显示错误信息
                            Toast.makeText(Registration.this, "Error checking email: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }
}