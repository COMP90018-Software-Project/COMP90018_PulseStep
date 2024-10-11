package com.example.pulsestepapplication;

import static android.app.Activity.RESULT_OK;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.pulsestepapplication.calendar.DateItemClickListener;
import com.example.pulsestepapplication.calendar.HorizontalCalendar;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;

public class ProfileFragment extends Fragment implements DateItemClickListener {
    private HorizontalCalendar horizontalCalendar;
    private TextView monthTextView;
    private Uri filePath;
    ImageView profileImage;
    StorageReference storageReference;
    ActivityResultLauncher<Intent> imagePickerLauncher;

    private final int PICK_IMAGE_REQUEST = 71;

    public ProfileFragment(){

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize the ActivityResultLauncher for image picking
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        // Handle the image picking result
                        Intent data = result.getData();
                        filePath = data.getData();
                        if (filePath != null) {
                            try {
                                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), filePath);
                                profileImage.setImageBitmap(bitmap);
                                uploadImage();  // Upload the selected image
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
        );
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


        // Image picker button
        ImageView imagePicker = view.findViewById(R.id.btn_image_picker);
        profileImage = view.findViewById(R.id.profile_image);

        loadImage(); // Load the existing image from storage

        // Set OnClickListener for the image picker button
        imagePicker.setOnClickListener(v -> chooseImage());

        return view;
    }

    @Override
    public void onDateClick(String date, int position) {
        // Handle date click
        horizontalCalendar.highlightSelectedDate(position);
    }

    private void loadImage() {
        Bundle bundle = getArguments();
        if (bundle != null) {
            final String retrievedName = bundle.getString("Name");

            if (retrievedName != null) {
                // Reference to an image file in Cloud Storage
                StorageReference storageReference = FirebaseStorage.getInstance().getReference()
                        .child(retrievedName).child("images/profile_image");

                // Load the image using Glide
                Glide.with(this)
                        .load(storageReference)
                        .into(profileImage);
            } else {
                Log.e("ProfileFragment", "No name found in bundle");
            }
        } else {
            Log.e("ProfileFragment", "Arguments bundle is null");
        }
    }


    // Upload the selected image to Firebase Storage
    private void uploadImage() {
        if (filePath != null) {
            // 从 arguments 中获取名字
            Bundle bundle = getArguments();
            if (bundle != null) {
                final String retrievedName = bundle.getString("Name");

                if (retrievedName != null) {
                    // 初始化 Firebase Storage 的引用
                    storageReference = FirebaseStorage.getInstance().getReference()
                            .child(retrievedName).child("images/profile_image");

                    storageReference.putFile(filePath)
                            .addOnSuccessListener(taskSnapshot -> {
                                // 上传成功的处理逻辑
                                Toast.makeText(getContext(), "Image Uploaded", Toast.LENGTH_SHORT).show();
                                // 重新加载图片
                                loadImage();
                            })
                            .addOnFailureListener(e -> {
                                // 上传失败的处理逻辑
                                Toast.makeText(getContext(), "Upload Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                } else {
                    Log.e("ProfileFragment", "Name not found in arguments");
                    Toast.makeText(getContext(), "Failed to upload: Name is null", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.e("ProfileFragment", "Arguments bundle is null");
                Toast.makeText(getContext(), "Failed to upload: Arguments are null", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e("ProfileFragment", "File path is null");
            Toast.makeText(getContext(), "Failed to upload: File path is null", Toast.LENGTH_SHORT).show();
        }
    }



    // Method to trigger the image picker
    private void chooseImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        imagePickerLauncher.launch(Intent.createChooser(intent, "Select Picture"));
    }


}