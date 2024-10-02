package com.example.pulsestepapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.example.pulsestepapplication.calendar.DateItemClickListener;
import com.example.pulsestepapplication.calendar.HorizontalCalendar;


public class ProfileFragment extends Fragment implements DateItemClickListener {
    private HorizontalCalendar horizontalCalendar;
    private TextView monthTextView;
    ActivityResultLauncher<Intent> imagePickerLauncher;
//    Uri selectedImageUri;

    public ProfileFragment(){

    }

//    @Override
//    public void onCreate(@Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
//                result -> {
//                    if (result.getResultCode() == Activity.RESULT_OK){
//                        Intent data = result.getData();
//                        if (data != null && data.getData() != null){
//                            selectedImageUri = data.getData();
//                        }
//                    }
//                }
//                );
//    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Find the setting button by its ID
        ImageView settingButton = view.findViewById(R.id.setting_button_profile_page);

        // Set OnClickListener for the setting button
        settingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create an Intent to start Setting Activity
                Intent intent = new Intent(getActivity(), Settings.class);
                startActivity(intent);
            }
        });


        // Check if the fragment is added to its activity and is ready
        if (isAdded()) {
            // Initialize HorizontalCalendar when the fragment is fully attached to an activity
            RecyclerView datesRv = view.findViewById(R.id.dates_rv);
            TextView monthTextView = view.findViewById(R.id.month);
            horizontalCalendar = new HorizontalCalendar(this, datesRv, monthTextView, requireActivity());
        } else {
            // Handle the case where the fragment is not attached to an activity
            Toast.makeText(getContext(), "Calendar is not available yet. Please try again later.", Toast.LENGTH_SHORT).show();
            Log.e("ProfileFragment", "Fragment is not attached to an activity. HorizontalCalendar initialization skipped.");
        }

        return view;
    }

    @Override
    public void onDateClick(String date, int position) {
        // Handle date click
        horizontalCalendar.highlightSelectedDate(position);
    }

}