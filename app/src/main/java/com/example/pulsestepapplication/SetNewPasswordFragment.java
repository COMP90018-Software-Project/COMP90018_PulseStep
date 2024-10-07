package com.example.pulsestepapplication;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class SetNewPasswordFragment extends Fragment {

    public SetNewPasswordFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_set_new_password, container, false);

        Button updateButton = view.findViewById(R.id.bt_update);
        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Load SetNewPasswordFragment when user clicks "Confirm"
                ((ResetPassword) getActivity()).loadFragment(new SuccessFragment());
            }
        });


        return view;
    }
}