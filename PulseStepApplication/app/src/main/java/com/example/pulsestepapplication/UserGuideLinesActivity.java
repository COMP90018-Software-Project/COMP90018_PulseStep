package com.example.pulsestepapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class UserGuideLinesActivity extends AppCompatActivity {
    private ImageView backButton;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_guide_lines);
        backButton = findViewById(R.id.back_button_setting_page);

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // End the Settings activity and return to MainActivity
            }
        });
    }
}
