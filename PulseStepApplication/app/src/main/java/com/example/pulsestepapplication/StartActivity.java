package com.example.pulsestepapplication;

import android.os.Bundle;
import android.widget.Button;
import android.content.Intent;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class StartActivity extends AppCompatActivity {
    private Button startBtn;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        startBtn = findViewById(R.id.btn_start);

        startBtn.setOnClickListener(view -> {
            Intent intent = new Intent(StartActivity.this, Login.class);
            startActivity(intent);
        });

    }
}
