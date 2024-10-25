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

        // Continue button logic
        continueButton.setOnClickListener(view -> updatePassword());

//        // 设置 "Continue" 按钮的点击事件
//        continueButton.setOnClickListener(view -> {
//            String newPassword = newPasswordEditText.getText().toString();
//            String confirmPassword = confirmPasswordEditText.getText().toString();
//
//            // Check if user agree the term
//            if (!termCheckbox.isChecked()) {
//                Toast.makeText(SetPassword.this, "You must agree to the Terms of Service and Privacy Policy to continue.", Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//            // 校验两个密码是否一致
//            if (newPassword.isEmpty()) {
//                newPasswordInputLayout.setError("Password cannot be empty");
//            } else if (!newPassword.equals(confirmPassword)) {
//                confirmPasswordInputLayout.setError("Passwords do not match");
//            } else {
//                newPasswordInputLayout.setError(null); // 清除错误
//                confirmPasswordInputLayout.setError(null); // 清除错误
//
//                // 显示 ProgressDialog
//                progressDialog.show();
//
//                // 密码校验通过，执行 Firebase 注册操作
//                assert email != null;
//                mAuth.createUserWithEmailAndPassword(email, newPassword)
//                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
//                            @Override
//                            public void onComplete(@NonNull Task<AuthResult> task) {
//                                // 隐藏 ProgressDialog
//                                progressDialog.dismiss();
//
//                                if (task.isSuccessful()) {
//                                    FirebaseUser currentUser = mAuth.getCurrentUser();
//                                    String userUID = currentUser.getUid();
//                                    Map<String, Object> userDetails = new HashMap<>();
//                                    userDetails.put("fullName", fullName);
//                                    userDetails.put("email", email);
//                                    userDetails.put("birthday", "01/01/2000");
//                                    userDetails.put("height", "170");
//                                    userDetails.put("weight", "60");
//                                    userDetails.put("gender", "Other");
//                                    userDetails.put("appleHealthEnabled", "false");
//                                    userDetails.put("avatarUrl", "default_avatar.png");
//                                    // Default daily target for user
//                                    userDetails.put("target", 1);
//                                    db.collection("users").document(userUID).set(userDetails);
//                                    Toast.makeText(SetPassword.this, "Account created.", Toast.LENGTH_SHORT).show();
//                                    // 跳转到下一个页面或者主界面
//                                    Intent intent = new Intent(SetPassword.this, PersonalDetails.class);
//                                    intent.putExtra("FULL_NAME", fullName);
//                                    intent.putExtra("EMAIL", email);
//                                    startActivity(intent);
//                                } else {
//                                    // 如果注册失败，显示详细错误信息
//                                    String errorMessage = task.getException() != null ? task.getException().getMessage() : "Authentication failed.";
//                                    Toast.makeText(SetPassword.this, errorMessage, Toast.LENGTH_SHORT).show();
//                                }
//                            }
//                        });
//            }
//        });
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
        String confirmPassword = confirmPasswordEditText.getText().toString();

        if (!termCheckbox.isChecked()) {
            Toast.makeText(this,
                    "You must agree to the Terms of Service and Privacy Policy to continue.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPassword.isEmpty()) {
            newPasswordInputLayout.setError("Password cannot be empty");
            return;
        } else if (!newPassword.equals(confirmPassword)) {
            confirmPasswordInputLayout.setError("Passwords do not match");
            return;
        } else {
            newPasswordInputLayout.setError(null);
            confirmPasswordInputLayout.setError(null);
        }

        progressDialog.show();

        // Get the current user and update the password
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUser.updatePassword(newPassword)
                    .addOnCompleteListener(task -> {
                        progressDialog.dismiss();
                        if (task.isSuccessful()) {
                            saveUserDetails(currentUser);
                        } else {
                            Toast.makeText(SetPassword.this,
                                    "Failed to update password: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            progressDialog.dismiss();
            Toast.makeText(this, "User not found. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveUserDetails(FirebaseUser user) {
        String userUID = user.getUid();
        Map<String, Object> userDetails = new HashMap<>();
        userDetails.put("fullName", fullName);
        userDetails.put("email", email);
        userDetails.put("birthday", "01/01/2000");
        userDetails.put("height", "170");
        userDetails.put("weight", "60");
        userDetails.put("gender", "Other");
        userDetails.put("appleHealthEnabled", "false");
        userDetails.put("avatarUrl", "default_avatar.png");
        userDetails.put("target", 1); // Default daily target

        db.collection("users").document(userUID).set(userDetails)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(SetPassword.this,
                                "Account created successfully.",
                                Toast.LENGTH_SHORT).show();
                        navigateToPersonalDetails();
                    } else {
                        Toast.makeText(SetPassword.this,
                                "Failed to save user details.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToPersonalDetails() {
        Intent intent = new Intent(SetPassword.this, PersonalDetails.class);
        intent.putExtra("FULL_NAME", fullName);
        intent.putExtra("EMAIL", email);
        startActivity(intent);
    }

}
