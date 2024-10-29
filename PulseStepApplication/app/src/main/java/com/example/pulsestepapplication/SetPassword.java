package com.example.pulsestepapplication;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.graphics.Typeface;
import android.graphics.Color;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import android.os.Handler;
import android.os.Looper;

public class SetPassword extends AppCompatActivity {

    private TextInputEditText newPasswordEditText, confirmPasswordEditText;
    private TextInputLayout newPasswordInputLayout, confirmPasswordInputLayout;
    private TextView userInfoText, signInText;
    private Button continueButton;
    private ImageButton backButton;
    private CheckBox termCheckbox;
    private TextView termLink;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ProgressDialog progressDialog;
    private String fullName, email;
    private boolean emailVerificationSent = false; // Track if email is sent
    private final String passVerificationMessage = "Continue";
    private Handler handler = new Handler(Looper.getMainLooper()); // 用于定时检查
    private final int CHECK_INTERVAL = 1000; // 每隔5秒检查一次

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_password);

        // 初始化 Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 初始化 UI 元素
        newPasswordEditText = findViewById(R.id.newPasswordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        newPasswordInputLayout = findViewById(R.id.newPasswordInputLayout);
        confirmPasswordInputLayout = findViewById(R.id.confirmPasswordInputLayout);
        userInfoText = findViewById(R.id.userInfoText);
        signInText = findViewById(R.id.signInText);
        continueButton = findViewById(R.id.continueButton);
        backButton = findViewById(R.id.backButton);
        termCheckbox = findViewById(R.id.term_checkbox);
        termLink = findViewById(R.id.termOfUse);

        // Get user details passed from the Registration activity
        fullName = getIntent().getStringExtra("FULL_NAME");
        email = getIntent().getStringExtra("EMAIL");

        // Initialize ProgressDialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Creating account...");
        progressDialog.setCancelable(false);

        // Display user information
        userInfoText.setText("Welcome, " + fullName + " (" + email + ")");

        // Set up clickable terms link
        setTermsClickable();

        // Back button logic
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



        continueButton.setOnClickListener(view -> {
            String newPassword = newPasswordEditText.getText().toString();
            String confirmPassword = confirmPasswordEditText.getText().toString();

            if (!termCheckbox.isChecked()) {
                // 如果用户未勾选复选框，弹出Toast提示
                Toast.makeText(this, "You must agree to the Terms of Service and Privacy Policy to continue.",
                        Toast.LENGTH_SHORT).show();
                return; // 直接返回，不继续执行
            }

            if (emailVerificationSent) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    user.reload().addOnCompleteListener(task -> {
                        if (user.isEmailVerified()) {
                            navigateToPersonalDetails(); // 如果已验证，则直接跳转到下一个页面
                        } else {
                            Toast.makeText(this, "Please verify your email before continuing.",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            } else if (newPassword.isEmpty() || !newPassword.equals(confirmPassword)){
                if (newPassword.isEmpty()) {
                    newPasswordInputLayout.setError("Password cannot be empty");
                } else if (!newPassword.equals(confirmPassword)) {
                    confirmPasswordInputLayout.setError("Passwords do not match");
                } else {
                    newPasswordInputLayout.setError(null);
                    confirmPasswordInputLayout.setError(null);
                }

                emailVerificationSent = false;
            } else{
                createUserAndSendVerification();
                emailVerificationSent = true;
                continueButton.setText(passVerificationMessage);
            }
        });

    }

    private void createUserAndSendVerification() {
        progressDialog.show();

        mAuth.createUserWithEmailAndPassword(email, "temporaryPassword123")
                .addOnCompleteListener(task -> {
                    progressDialog.dismiss();
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            sendVerificationEmail(user);
                            emailVerificationSent = true;
                            continueButton.setText(passVerificationMessage);
                            startVerificationCheck(); // 启动验证状态的定时检查
                        }
                    } else {
                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() :
                                "Failed to create user.";
                        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendVerificationEmail(FirebaseUser user) {
        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this,
                                "Verification email sent. Please verify before continuing.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() :
                                "Failed to send verification email.";
                        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void startVerificationCheck() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    user.reload().addOnCompleteListener(task -> {
                        if (user.isEmailVerified()) {
                            // 验证成功后，显示 Toast 提示
                            Toast.makeText(SetPassword.this,
                                    "Email verified! Updating your password...",
                                    Toast.LENGTH_SHORT).show();

                            updatePassword(); // 验证成功后自动更新密码
                        } else {
                            // 如果未验证，则继续检查
                            handler.postDelayed(this, CHECK_INTERVAL);
                        }
                    });
                }
            }
        }, CHECK_INTERVAL);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 如果用户未完成验证且退出流程，删除用户
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && !user.isEmailVerified()) {
            user.delete().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    db.collection("users").document(user.getUid()).delete();
                }
            });
        }
    }


    private void setTermsClickable() {
        String termLinkText = getString(R.string.term_of_use);
        SpannableString termSpannableString = new SpannableString(termLinkText);

        ClickableSpan termsSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                showTermDialog(SetPassword.this, "Terms of Service & Privacy Policy",
                        String.valueOf(Html.fromHtml(getString(R.string.term_content))));
            }
        };

        ForegroundColorSpan blueColorSpan = new ForegroundColorSpan(Color.BLUE);

        int termsStart = termLinkText.indexOf("Terms of Service & Privacy Policy");
        int termsEnd = termsStart + "Terms of Service & Privacy Policy".length();

        termSpannableString.setSpan(termsSpan, termsStart, termsEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        termSpannableString.setSpan(blueColorSpan, termsStart, termsEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        termLink.setText(termSpannableString);
        termLink.setMovementMethod(LinkMovementMethod.getInstance());
    }

    // 显示滚动对话框的方法
    private void showTermDialog(Context context, String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);

        // 使用布局填充器创建可滚动对话框
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_scrollable, null);
        TextView dialogText = dialogView.findViewById(R.id.dialog_text);
        dialogText.setText(message);

        builder.setView(dialogView);
        builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void updatePassword() {
        String newPassword = newPasswordEditText.getText().toString();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUser.updatePassword(newPassword)
                    .addOnCompleteListener(task -> {
                        progressDialog.dismiss();
                        if (task.isSuccessful()) {
                            saveUserDetails(currentUser);
                        } else {
                            Toast.makeText(this, "Failed to update password: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            progressDialog.dismiss();
            Toast.makeText(this, "User not found. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveUserDetails(FirebaseUser user) {
        Map<String, Object> userDetails = new HashMap<>();
        userDetails.put("fullName", fullName);
        userDetails.put("email", email);
        userDetails.put("birthday", "01/01/2000");
        userDetails.put("height", "170");
        userDetails.put("weight", "60");
        userDetails.put("gender", "Other");
        userDetails.put("appleHealthEnabled", "false");
        userDetails.put("avatarUrl", "default_avatar.png");
        userDetails.put("target", 1);

        db.collection("users").document(user.getUid()).set(userDetails)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                    } else {
                        Toast.makeText(this, "Failed to save user details.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToPersonalDetails() {
        Intent intent = new Intent(this, PersonalDetails.class);
        intent.putExtra("FULL_NAME", fullName);
        intent.putExtra("EMAIL", email);
        startActivity(intent);
    }
}