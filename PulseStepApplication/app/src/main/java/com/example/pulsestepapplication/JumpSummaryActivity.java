package com.example.pulsestepapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

public class JumpSummaryActivity extends AppCompatActivity {

    // UI Components
    // private GoogleMap googleMap;
    // private SupportMapFragment mapFragment;
    private TextView avgPaceTextView;
    private TextView timeTextView;
    private TextView addressTextView;
    private TextView totalJumpCountTextView;
    // private ImageView defaultBackground;
    private TextView caloriesTextView;

    // Tracking Data
    // private float distance; // in kilometers
    private String time; // formatted as "MM:SS"
    private String address; // optional
    private int jumpCount;
    // private ArrayList<LatLng> trajectory;
    // private CardView mapCard;
    private String avgPace;
    private Button finishButton;
    private String calories;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jump_summary);

        // Initialize UI components
        initializeUIComponents();

        // Retrieve data from Intent
        retrieveIntentData();

        // Display data
        displayData();

//        // Initialize and set up the jump speed gif
//        setUpGif(savedInstanceState);

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



    /**
     * Initializes the UI components by finding them via their IDs.
     */
    private void initializeUIComponents() {
        timeTextView = findViewById(R.id.jump_summary_time);
        addressTextView = findViewById(R.id.jump_summary_address);
        avgPaceTextView = findViewById(R.id.jump_summary_avg_count);
        totalJumpCountTextView = findViewById(R.id.jump_count);
        // defaultBackground = findViewById(R.id.default_background);
        caloriesTextView = findViewById(R.id.jump_summary_calories);
        // mapCard = findViewById(R.id.map_container);
        finishButton = findViewById(R.id.bt_finish_jump);
    }

    /**
     * Retrieves data passed from the tracking activity via Intent.
     */
    private void retrieveIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            time = intent.getStringExtra("time");
            jumpCount = intent.getIntExtra("jumpCount", 0);
            address = intent.getStringExtra("address");
            avgPace = intent.getStringExtra("avgSpeed");
            calories = intent.getStringExtra("calories");
        }
    }


    /**
     * Displays the retrieved data on the UI components.
     */
    private void displayData() {
        Log.d("DEBUG", "Average Jump Speed: " + avgPace);
        // distanceTextView.setText(String.format("%.2f", distance));
        timeTextView.setText(time != null ? time : "00:00");
        totalJumpCountTextView.setText(String.valueOf(jumpCount));
        addressTextView.setText(address != null ? address : "N/A");
        avgPaceTextView.setText(avgPace != null ? avgPace : "N/A");
        caloriesTextView.setText(calories != null ? calories : "N/A");

    }







}
