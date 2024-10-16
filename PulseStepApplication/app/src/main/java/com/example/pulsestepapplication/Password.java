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


        // 初始化 Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        // 获取 Firestore 实例
        db = FirebaseFirestore.getInstance();


        passwordEditText = findViewById(R.id.passwordEditText);
        signInButton = findViewById(R.id.signInButton);
        backButton = findViewById(R.id.backButton);
        createAccountText = findViewById(R.id.createAccountText);
        forgotPasswordText = findViewById(R.id.forgotPasswordText);


        // 初始化 ProgressDialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Signing in...");
        progressDialog.setCancelable(false);


        // 获取从上一个页面传递的电子邮件
        email = getIntent().getStringExtra("EMAIL");


        // 设置 "Create Account" 部分的文本样式
        String fullText = "Don't have an account? Create Account";
        SpannableString spannableString = new SpannableString(fullText);
        int startIndex = fullText.indexOf("Create Account");
        int endIndex = startIndex + "Create Account".length();


        // 设置加粗和黑色
        spannableString.setSpan(new ForegroundColorSpan(getResources().getColor(android.R.color.black)), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        createAccountText.setText(spannableString);


        // 设置 "Forgot password?" 的文本样式
        String forgotText = "Forgot password?";
        SpannableString forgotSpannable = new SpannableString(forgotText);
        forgotSpannable.setSpan(new ForegroundColorSpan(getResources().getColor(android.R.color.black)), 0, forgotText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        forgotPasswordText.setText(forgotSpannable);


        // 设置返回按钮的点击事件
        backButton.setOnClickListener(view -> finish());


        // 设置 "Sign In" 按钮的点击事件
        signInButton.setOnClickListener(view -> {
            String password = passwordEditText.getText().toString();
            if (password.isEmpty()) {
                passwordEditText.setError("Enter your password");
            } else {
                // 显示加载框
                progressDialog.show();
                signInUser(email, password); // 调用登录方法
            }
        });


        // 设置 "Create Account" 的点击事件
        createAccountText.setOnClickListener(view -> {
            Intent intent = new Intent(Password.this, Registration.class);
            startActivity(intent);
        });


        // 设置 "Forgot password?" 的点击事件
        forgotPasswordText.setOnClickListener(view -> {
            Intent intent = new Intent(Password.this, ResetPassword.class);
            startActivity(intent);
        });
    }


    // 登录方法
    private void signInUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    // 隐藏加载框
                    progressDialog.dismiss();


                    if (task.isSuccessful()) {
                        // 登录成功
                        Toast.makeText(Password.this, "Login successful", Toast.LENGTH_SHORT).show();
                        // 获取当前用户的 UID
                        String userId = mAuth.getCurrentUser().getUid();
                        // 从 Firestore 中获取用户信息
                        db.collection("users").document(userId).get()
                                .addOnSuccessListener(documentSnapshot -> {
                                    if (documentSnapshot.exists()) {
                                        // 获取用户的全名
                                        String fullName = documentSnapshot.getString("fullName");
                                        // 显示登录成功信息和全名
                                        Toast.makeText(Password.this, "Login successful. Welcome " + fullName, Toast.LENGTH_SHORT).show();
                                        // 跳转到主页面或其他页面
                                        Intent intent = new Intent(Password.this, MainActivity.class);
                                        intent.putExtra("FULL_NAME", fullName); // 传递全名到下一个页面
                                        intent.putExtra("USER_ID", userId); // 传递 UID 到下一个页面
                                        startActivity(intent);
                                        finish();
                                    }
                                });
                    } else {
                        // 登录失败
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
