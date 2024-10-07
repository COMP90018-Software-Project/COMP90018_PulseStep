package com.example.pulsestepapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class JumpSummaryActivity extends AppCompatActivity {

    private Button finishButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jump_summary);

        // Reference to the Finish button
        finishButton = findViewById(R.id.bt_finish_jump);

        // Handle Finish button click
        finishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Finish the activity and return to the previous screen
                Intent intent = new Intent(JumpSummaryActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

    }
}
