package com.example.pulsestepapplication;

import android.content.Intent;
import android.os.Bundle;

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

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ProfileFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ProfileFragment extends Fragment implements DateItemClickListener {
    private HorizontalCalendar horizontalCalendar;
    private TextView monthTextView;


    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public ProfileFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ProfileFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ProfileFragment newInstance(String param1, String param2) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

    }

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