package com.example.pulsestepapplication;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class SetPassword extends AppCompatActivity {

    private TextInputEditText newPasswordEditText, confirmPasswordEditText;
    private TextInputLayout newPasswordInputLayout, confirmPasswordInputLayout;
    private TextView userInfoText, signInText;
    private Button continueButton;
    private ImageButton backButton;
    private FirebaseAuth mAuth;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_password);

        // 初始化 Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // 初始化 UI 元素
        newPasswordEditText = findViewById(R.id.newPasswordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        newPasswordInputLayout = findViewById(R.id.newPasswordInputLayout);
        confirmPasswordInputLayout = findViewById(R.id.confirmPasswordInputLayout);
        userInfoText = findViewById(R.id.userInfoText);
        signInText = findViewById(R.id.signInText);
        continueButton = findViewById(R.id.continueButton);
        backButton = findViewById(R.id.backButton);

        // 初始化 ProgressDialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Creating account...");
        progressDialog.setCancelable(false);

        // 获取传递的用户名字和邮箱信息
        String fullName = getIntent().getStringExtra("FULL_NAME");
        String email = getIntent().getStringExtra("EMAIL");

        // 显示用户信息
        userInfoText.setText("Welcome, " + fullName + " (" + email + ")");

        // 设置返回按钮的点击事件
        backButton.setOnClickListener(view -> finish());

        // 设置 "Sign In" 部分的文本样式
        String fullText = "Already have an account? Sign In";
        SpannableString spannableString = new SpannableString(fullText);
        int startIndex = fullText.indexOf("Sign In");
        int endIndex = startIndex + "Sign In".length();

        // 设置加粗和黑色
        spannableString.setSpan(new StyleSpan(Typeface.BOLD), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new ForegroundColorSpan(getResources().getColor(android.R.color.black)), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        signInText.setText(spannableString);

        // 设置 "Sign In" 的点击事件
        signInText.setOnClickListener(view -> {
            Intent intent = new Intent(SetPassword.this, Login.class);
            startActivity(intent);
        });

        // 设置 "Continue" 按钮的点击事件
        continueButton.setOnClickListener(view -> {
            String newPassword = newPasswordEditText.getText().toString();
            String confirmPassword = confirmPasswordEditText.getText().toString();

            // 校验两个密码是否一致
            if (newPassword.isEmpty()) {
                newPasswordInputLayout.setError("Password cannot be empty");
            } else if (!newPassword.equals(confirmPassword)) {
                confirmPasswordInputLayout.setError("Passwords do not match");
            } else {
                newPasswordInputLayout.setError(null); // 清除错误
                confirmPasswordInputLayout.setError(null); // 清除错误

                // 显示 ProgressDialog
                progressDialog.show();

                // 密码校验通过，执行 Firebase 注册操作
                assert email != null;
                mAuth.createUserWithEmailAndPassword(email, newPassword)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                // 隐藏 ProgressDialog
                                progressDialog.dismiss();

                                if (task.isSuccessful()) {
                                    Toast.makeText(SetPassword.this, "Account created.", Toast.LENGTH_SHORT).show();
                                    // 跳转到下一个页面或者主界面
                                    Intent intent = new Intent(SetPassword.this, PersonalDetails.class);
                                    intent.putExtra("FULL_NAME", fullName);
                                    intent.putExtra("EMAIL", email);
                                    startActivity(intent);
                                } else {
                                    // 如果注册失败，显示详细错误信息
                                    String errorMessage = task.getException() != null ? task.getException().getMessage() : "Authentication failed.";
                                    Toast.makeText(SetPassword.this, errorMessage, Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });
    }
}
