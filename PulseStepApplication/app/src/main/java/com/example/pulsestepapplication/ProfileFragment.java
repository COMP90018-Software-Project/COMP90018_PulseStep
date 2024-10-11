package com.example.pulsestepapplication;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
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

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.pulsestepapplication.calendar.DateItemClickListener;
import com.example.pulsestepapplication.calendar.HorizontalCalendar;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;


import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class ProfileFragment extends Fragment implements DateItemClickListener {
    private HorizontalCalendar horizontalCalendar;
    private TextView monthTextView;
    ImageView profileImage;
    StorageReference storageReference;

    ActivityResultLauncher<Intent> imagePickLauncher;
    Uri selectedImageUri;

    private String userId;
    private String userName;

    public ProfileFragment() {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        imagePickLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if(result.getResultCode() == Activity.RESULT_OK){
                        Intent data = result.getData();
                        if(data!=null && data.getData()!=null){
                            selectedImageUri = data.getData();
                            setProfilePic(getContext(),selectedImageUri,profileImage);
                            uploadImage();
                        }
                    }
                }
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Get the arguments passed from MainActivity
        Bundle args = getArguments();
        if (args != null) {
            userId = args.getString("userId");
            userName = args.getString("fullName");
            Log.e("ProfileFragment", "userId: " + userId + "Full Name: " + userName);
        }

        profileImage = view.findViewById(R.id.profile_image);
        FirebaseStorage.getInstance().getReference()
                .child("users")
                .child(userId)
                .child("images/profile_image")
                .getDownloadUrl()
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        Uri uri  = task.getResult();
                        setProfilePic(getContext(),uri,profileImage);
                    }
                });

        // Set username in profile page
        TextView nameTextView = view.findViewById(R.id.name);
        nameTextView.setText(userName);

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

        imagePicker.setOnClickListener((v)->{
            ImagePicker.with(this).cropSquare().compress(512).maxResultSize(512,512)
                    .createIntent(new Function1<Intent, Unit>() {
                        @Override
                        public Unit invoke(Intent intent) {
                            imagePickLauncher.launch(intent);
                            return null;
                        }
                    });
        });


        return view;
    }

    @Override
    public void onDateClick(String date, int position) {
        // Handle date click
        horizontalCalendar.highlightSelectedDate(position);
    }


    // Upload the selected image to Firebase Storage
    private void uploadImage() {
        if (selectedImageUri != null) {
            if (userId != null && !userId.isEmpty()) {
                // 初始化 Firebase Storage 的引用
                storageReference = FirebaseStorage.getInstance().getReference()
                        .child("users").child(userId).child("images/profile_image");

                storageReference.putFile(selectedImageUri)
                        .addOnSuccessListener(taskSnapshot -> {
                            // 上传成功的处理逻辑
//                            Toast.makeText(getContext(), "Image Uploaded", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            // 上传失败的处理逻辑
                            Toast.makeText(getContext(), "Upload Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            } else {
                Log.e("ProfileFragment", "Name not found in arguments");
                Toast.makeText(getContext(), "Failed to upload: Name is null or empty", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e("ProfileFragment", "filePath is null");
        }
    }

    public static void setProfilePic(Context context, Uri imageUri, ImageView imageView){
        Glide.with(context).load(imageUri).apply(RequestOptions.circleCropTransform()).into(imageView);
    }

}