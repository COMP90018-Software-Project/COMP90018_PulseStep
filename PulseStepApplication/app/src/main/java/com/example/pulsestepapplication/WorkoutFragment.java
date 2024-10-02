package com.example.pulsestepapplication;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.content.Intent;


public class WorkoutFragment extends Fragment {

    public WorkoutFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_workout, container, false);

        // Find the Run button by its ID
        Button runButton = view.findViewById(R.id.run_button);

        // Set OnClickListener for the Run button
        runButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create an Intent to start RunningActivity
                Intent intent = new Intent(getActivity(), RunningActivity.class);
                startActivity(intent);
            }
        });

        // Find the Jump button by its ID
        Button jumpButton = view.findViewById(R.id.jump_button);

        // Set OnClickListener for the Jump button
        jumpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create an Intent to start RunningActivity
                Intent intent = new Intent(getActivity(), JumpActivity.class);
                startActivity(intent);
            }
        });


        return view;
    }
}