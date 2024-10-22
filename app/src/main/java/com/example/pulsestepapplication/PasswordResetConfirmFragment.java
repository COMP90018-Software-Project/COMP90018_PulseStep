package com.example.pulsestepapplication;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;


public class PasswordResetConfirmFragment extends Fragment {
    public PasswordResetConfirmFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_password_reset_confirm, container, false);

        Button confirmButton = view.findViewById(R.id.bt_confirm);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Load SetNewPasswordFragment when user clicks "Confirm"
                ((ResetPassword) getActivity()).loadFragment(new SetNewPasswordFragment());
            }
        });

        return view;

    }
}