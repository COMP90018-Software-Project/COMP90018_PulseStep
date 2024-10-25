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

public class SetPassword extends AppCompatActivity {

    private TextInputEditText newPasswordEditText, confirmPasswordEditText;
    private TextInputLayout newPasswordInputLayout, confirmPasswordInputLayout;
    private TextView userInfoText, signInText;
    private Button continueButton;
    private ImageButton backButton;
    private CheckBox termCheckbox;
    private TextView termLink;
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
        termCheckbox = findViewById(R.id.term_checkbox);
        termLink = findViewById(R.id.termOfUse);

        // 设置TextView部分文字为可点击的链接
        String termLinkText = getString(R.string.term_of_use);
        SpannableString termSpannableString = new SpannableString(termLinkText);

        // 设置 "Terms of Service" 的点击事件
        ClickableSpan termsSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                showTermDialog(SetPassword.this, "Terms of Service & Privacy Policy", String.valueOf(Html.fromHtml(getString(R.string.term_content))));
            }
        };

        // 设置颜色为蓝色
        ForegroundColorSpan blueColorSpan = new ForegroundColorSpan(Color.BLUE);

        // 找到文字中的特定部分位置
        int termsStart = termLinkText.indexOf("Terms of Service & Privacy Policy");
        int termsEnd = termsStart + "Terms of Service & Privacy Policy".length();

        // 为 "Terms of Service" 设置可点击和颜色
        termSpannableString.setSpan(termsSpan, termsStart, termsEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        termSpannableString.setSpan(blueColorSpan, termsStart, termsEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        // 设置TextView的内容为SpannableString，并启用点击事件
        termLink.setText(termSpannableString);
        termLink.setMovementMethod(LinkMovementMethod.getInstance());

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

            // Check if user agree the term
            if (!termCheckbox.isChecked()) {
                Toast.makeText(SetPassword.this, "You must agree to the Terms of Service & Privacy Policy to continue.", Toast.LENGTH_SHORT).show();
                return;
            }

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
}
