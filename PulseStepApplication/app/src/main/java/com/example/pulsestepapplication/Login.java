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
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class Login extends AppCompatActivity {

    private TextInputLayout emailInputLayout;
    private TextInputEditText emailEditText;
    private Button continueButton;
    private TextView createAccountText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailInputLayout = findViewById(R.id.emailInputLayout);
        emailEditText = findViewById(R.id.emailEditText);
        continueButton = findViewById(R.id.continueButton);
        createAccountText = findViewById(R.id.createAccountText);

        // 设置 "Create Account" 部分的文本样式
        String fullText = "Don't have an account? Create Account";
        SpannableString spannableString = new SpannableString(fullText);
        int startIndex = fullText.indexOf(" Create Account");
        int endIndex = startIndex + " Create Account".length();

        // 设置加粗和黑色
        spannableString.setSpan(new ForegroundColorSpan(getResources().getColor(android.R.color.black)), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        createAccountText.setText(spannableString);

        continueButton.setOnClickListener(view -> {
            String email = emailEditText.getText().toString();

            if (email.isEmpty()) {
                emailInputLayout.setError("Enter your email address");
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailInputLayout.setError("Invalid email format");
            } else {
                emailInputLayout.setError(null);
                Intent intent = new Intent(Login.this, Password.class);
                intent.putExtra("EMAIL", email);
                startActivity(intent);
            }
        });

        // 设置 "Create Account" 的点击事件
        createAccountText.setOnClickListener(view -> {
            Intent intent = new Intent(Login.this, Registration.class);
            startActivity(intent);
        });
    }
}