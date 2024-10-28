package com.example.pulsestepapplication;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;


public class SuccessFragment extends Fragment {
    private Button continueButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_success, container, false);

        continueButton = view.findViewById(R.id.bt_continue);
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle continue action (e.g., navigate to login or dashboard)
                getActivity().finish(); // Close the activity after success
            }
        });

        return view;
    }

}