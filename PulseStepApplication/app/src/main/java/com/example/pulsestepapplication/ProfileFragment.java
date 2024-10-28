package com.example.pulsestepapplication;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import java.util.Collections;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.pulsestepapplication.calendar.CalendarAdapter;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class ProfileFragment extends Fragment {
    private ImageView profileImage;
    private TextView nameTextView;
    private StorageReference storageReference;

    private ActivityResultLauncher<Intent> imagePickLauncher;
    private Uri selectedImageUri;

    private String userId;
    private String userName;
    private String gender;
    private int selectedPosition;
    private String selectedDate;
    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private LinearSnapHelper snapHelper;

    private ListenerRegistration profileListener;  // Listener registration for Firestore updates

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // 获取从 MainActivity 传来的参数
        Bundle args = getArguments();
        if (args != null) {
            userId = args.getString("userId");
            userName = args.getString("fullName");
            gender = args.getString("gender");
            Log.e("ProfileFragment", "userId: " + userId + ", Full Name: " + userName + ", Gender: " + gender);
        }

        // 默认加载当天的数据
        selectedDate = getCurrentDate();
        fetchUserDailyInfo(selectedDate);

        profileImage = view.findViewById(R.id.profile_image);
        FirebaseStorage.getInstance().getReference()
                .child("users")
                .child(userId)
                .child("images/profile_image")
                .getDownloadUrl()
                .addOnSuccessListener(uri -> {
                    // 成功获取到图片 URL，设置用户自定义头像
                    setProfilePic(getContext(), uri, profileImage);
                })
                .addOnFailureListener(exception -> {
                    // 文件不存在，处理 StorageException，并根据性别设置默认头像
                    if (exception instanceof StorageException) {
                        StorageException storageException = (StorageException) exception;
                        if (storageException.getErrorCode() == StorageException.ERROR_OBJECT_NOT_FOUND) {
                            // 根据性别设置默认头像
                            if (gender != null) {
                                if (gender.equalsIgnoreCase("male")) {
                                    // 设置男性默认头像
                                    profileImage.setImageResource(R.drawable.male_default_avatar);
                                } else if (gender.equalsIgnoreCase("female")) {
                                    // 设置女性默认头像
                                    profileImage.setImageResource(R.drawable.female_default_avatar);
                                }else if (gender.equalsIgnoreCase("other")) {
                                    // 如果性别为other，设置通用默认头像
                                    profileImage.setImageResource(R.drawable.default_avatar);
                                }
                            }
                        } else {
                            Log.e("ProfileFragment", "Error fetching profile image: " + exception.getMessage());
                        }
                    }
                });


        nameTextView = view.findViewById(R.id.name);
        nameTextView.setText(userName);

        // Initialize Firestore listener for user profile updates
        initializeProfileListener();

        ImageView settingButton = view.findViewById(R.id.setting_button_profile_page);
        settingButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), Settings.class);
            // 通过 Intent 传递 userId 到 Settings Activity
            intent.putExtra("userId", userId);
            startActivity(intent);
        });

        // 设置 RecyclerView 和 CalendarAdapter
        if (isAdded()) {
            recyclerView = view.findViewById(R.id.recyclerView);
            layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
            recyclerView.setLayoutManager(layoutManager);

            List<String> dates = getDatesList();
            CalendarAdapter adapter = new CalendarAdapter(dates, selectedPosition, recyclerView, layoutManager, date -> {
                selectedDate = date;
                Log.d("ProfileFragment", "Date clicked: " + date);
                fetchUserDailyInfo(selectedDate);  // 点击日期时获取该日期的数据
            });
            recyclerView.setAdapter(adapter);

        } else {
            Toast.makeText(getContext(), "Calendar is not available yet. Please try again later.", Toast.LENGTH_SHORT).show();
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



    // 根据选择的日期实时监听 Firestore 数据的更改
    private void fetchUserDailyInfo(String date) {
        DocumentReference userRef = FirebaseFirestore.getInstance().collection("users").document(userId);

        // 添加实时监听器，当数据库数据发生变化时会自动调用
        userRef.addSnapshotListener((documentSnapshot, error) -> {
            if (error != null) {
                Log.w("FETCH DATA", "Listen failed.", error);
                return;
            }

            if (documentSnapshot != null && documentSnapshot.exists()) {
                Map<String, Map<String, Object>> dailyJumpInfo = (Map<String, Map<String, Object>>) documentSnapshot.get("dailyJumpInfo");
                Map<String, Map<String, Object>> dailyRunningInfo = (Map<String, Map<String, Object>>) documentSnapshot.get("dailyRunningInfo");

                // 处理跳绳数据
                if (dailyJumpInfo != null && dailyJumpInfo.containsKey(date)) {
                    Map<String, Object> todayJumpData = dailyJumpInfo.get(date);

                    long activeJumpTime = todayJumpData.get("activeTime") != null ? (Long) todayJumpData.get("activeTime") : 0;
                    int jumpCount = todayJumpData.get("jumpCount") != null ? ((Long) todayJumpData.get("jumpCount")).intValue() : 0;
                    double jumpCalories = todayJumpData.get("calories") != null ? ((Number) todayJumpData.get("calories")).doubleValue() : 0.0;

                    updateJumpUI(activeJumpTime, jumpCount, jumpCalories);
                } else {
                    updateJumpUINoRecord();
                }

                // 处理跑步数据
                if (dailyRunningInfo != null && dailyRunningInfo.containsKey(date)) {
                    Map<String, Object> todayRunningData = dailyRunningInfo.get(date);

                    long activeRunningTime = todayRunningData.get("activeTime") != null ? (Long) todayRunningData.get("activeTime") : 0;
                    float runningDistance = todayRunningData.get("distance") != null ? ((Number) todayRunningData.get("distance")).floatValue() : 0.0f;
                    double runningCalories = todayRunningData.get("calories") != null ? ((Number) todayRunningData.get("calories")).doubleValue() : 0.0;

                    updateRunningUI(activeRunningTime, runningDistance, runningCalories);
                } else {
                    updateRunningUINoRecord();
                }
            } else {
                Log.w("FETCH DATA", "Document does not exist.");
            }
        });
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

    public List<String> getDatesList() {
        List<String> dates = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();  // Current date

        SimpleDateFormat dateFormat = new SimpleDateFormat("d", Locale.getDefault());

        // Add previous 15 days
        List<String> tempDates = new ArrayList<>();  // Temporary list to store dates
        for (int i = 15; i > 0; i--) {
            calendar.add(Calendar.DAY_OF_MONTH, -1);
            tempDates.add(dateFormat.format(calendar.getTime()));
        }

        // Reverse the temporary list so the oldest date comes first
        Collections.reverse(tempDates);
        dates.addAll(tempDates);  // Add the reversed dates to the main 'dates' list


        // Find today's date
        String todayDate = "Today";
        dates.add(todayDate);
        selectedPosition = dates.indexOf(todayDate);

        // Add next 7 days
        calendar = Calendar.getInstance();  // Reset to the current date
        for (int i = 1; i <= 7; i++) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            dates.add(dateFormat.format(calendar.getTime()));
        }

        return dates;
    }

    private void updateJumpUI(long activeTime, int jumpCount, double calories) {
        TextView workoutDurationTextView = getView().findViewById(R.id.workout_duration_jump);
        TextView workoutCountTextView = getView().findViewById(R.id.workout_count_jump);
        TextView workoutCaloriesTextView = getView().findViewById(R.id.workout_calories_jump);

        // 设置时长 (将秒数转换为分钟)
        workoutDurationTextView.setText(String.format(Locale.getDefault(), "%.1f min", (double) activeTime / 60));

        // 设置跳跃次数
        workoutCountTextView.setText(String.format(Locale.getDefault(), "%d times", jumpCount));

        // 设置卡路里
        workoutCaloriesTextView.setText(String.format(Locale.getDefault(), "%.1f kcal", calories / 1000));
    }

    // Helper function to update the UI with retrieved data
    private void updateRunningUI(long activeTime, float distance, double calories) {
        TextView workoutDurationTextView = getView().findViewById(R.id.workout_duration_run);
        TextView workoutCountTextView = getView().findViewById(R.id.workout_count_run);
        TextView workoutCaloriesTextView = getView().findViewById(R.id.workout_calories_run);

        // 设置时长 (将秒数转换为分钟)
        workoutDurationTextView.setText(String.format(Locale.getDefault(), "%.1f min", (double) activeTime / 60));

        // 设置跳跃次数
        workoutCountTextView.setText(String.format(Locale.getDefault(), "%.1f km", distance / 1000));

        // 设置卡路里
        workoutCaloriesTextView.setText(String.format(Locale.getDefault(), "%.1f kcal", calories / 1000));
    }

    private void updateRunningUINoRecord() {
        TextView runDurationTextView = getView().findViewById(R.id.workout_duration_run);
        TextView runCountTextView = getView().findViewById(R.id.workout_count_run);
        TextView runCaloriesTextView = getView().findViewById(R.id.workout_calories_run);

        runDurationTextView.setText("-- min");
        runCountTextView.setText("-- km");
        runCaloriesTextView.setText("-- kcal");
    }

    private void updateJumpUINoRecord() {
        TextView jumpDurationTextView = getView().findViewById(R.id.workout_duration_jump);
        TextView jumpCountTextView = getView().findViewById(R.id.workout_count_jump);
        TextView jumpCaloriesTextView = getView().findViewById(R.id.workout_calories_jump);

        jumpDurationTextView.setText("-- min");
        jumpCountTextView.setText("-- times");
        jumpCaloriesTextView.setText("-- kcal");
    }



    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    // This method updates the fragment's data
    public void updateData(String UserId, String fullName, String gender) {
        this.userId = UserId;
        this.userName = fullName;
        this.gender = gender;

        TextView nameTextView = getView().findViewById(R.id.name);
        nameTextView.setText(userName);


        profileImage = getView().findViewById(R.id.profile_image);
        FirebaseStorage.getInstance().getReference()
                .child("users")
                .child(userId)
                .child("images/profile_image")
                .getDownloadUrl()
                .addOnSuccessListener(uri -> {
                    // 成功获取到图片 URL，设置用户自定义头像
                    setProfilePic(getContext(), uri, profileImage);
                })
                .addOnFailureListener(exception -> {
                    // 文件不存在，处理 StorageException，并根据性别设置默认头像
                    if (exception instanceof StorageException) {
                        StorageException storageException = (StorageException) exception;
                        if (storageException.getErrorCode() == StorageException.ERROR_OBJECT_NOT_FOUND) {
                            // 根据性别设置默认头像
                            if (gender != null) {
                                if (gender.equalsIgnoreCase("male")) {
                                    // 设置男性默认头像
                                    profileImage.setImageResource(R.drawable.male_default_avatar);
                                } else if (gender.equalsIgnoreCase("female")) {
                                    // 设置女性默认头像
                                    profileImage.setImageResource(R.drawable.female_default_avatar);
                                }else if (gender.equalsIgnoreCase("other")) {
                                    // 如果性别为other，设置通用默认头像
                                    profileImage.setImageResource(R.drawable.default_avatar);
                                }
                            }
                        } else {
                            Log.e("ProfileFragment", "Error fetching profile image: " + exception.getMessage());
                        }
                    }
                });
    }


    private void initializeProfileListener() {
        if (userId == null) {
            Log.e("ProfileFragment", "User ID is null. Cannot set up listener.");
            return;
        }

        // Set up real-time listener for the user's profile document
        DocumentReference userRef = FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId);

        profileListener = userRef.addSnapshotListener((documentSnapshot, e) -> {
            if (e != null) {
                Log.e("ProfileFragment", "Failed to listen for profile updates", e);
                return;
            }

            if (documentSnapshot != null && documentSnapshot.exists()) {
                // Update full name and gender if available
                userName = documentSnapshot.getString("fullName");
                gender = documentSnapshot.getString("gender");

                // Update the UI with the new data
                updateUIWithProfileData();
            } else {
                Log.w("ProfileFragment", "Profile document does not exist.");
            }
        });
    }

    // Method to update the UI based on the latest profile data
    private void updateUIWithProfileData() {
        if (userName != null) {
            nameTextView.setText(userName);
        }

        if (gender != null) {
            setDefaultProfileImageBasedOnGender();
        }
    }

    // Set default profile image based on gender
    private void setDefaultProfileImageBasedOnGender() {
        FirebaseStorage.getInstance().getReference()
                .child("users")
                .child(userId)
                .child("images/profile_image")
                .getDownloadUrl()
                .addOnSuccessListener(uri -> {
                    // 成功获取到图片 URL，设置用户自定义头像
                    setProfilePic(getContext(), uri, profileImage);
                })
                .addOnFailureListener(exception -> {
                    // 文件不存在，处理 StorageException，并根据性别设置默认头像
                    if (exception instanceof StorageException) {
                        StorageException storageException = (StorageException) exception;
                        if (storageException.getErrorCode() == StorageException.ERROR_OBJECT_NOT_FOUND) {
                            // 根据性别设置默认头像
                            if (gender != null) {
                                if (gender.equalsIgnoreCase("male")) {
                                    // 设置男性默认头像
                                    profileImage.setImageResource(R.drawable.male_default_avatar);
                                } else if (gender.equalsIgnoreCase("female")) {
                                    // 设置女性默认头像
                                    profileImage.setImageResource(R.drawable.female_default_avatar);
                                }else if (gender.equalsIgnoreCase("other")) {
                                    // 如果性别为other，设置通用默认头像
                                    profileImage.setImageResource(R.drawable.default_avatar);
                                }
                            }
                        } else {
                            Log.e("ProfileFragment", "Error fetching profile image: " + exception.getMessage());
                        }
                    }
                });
    }


}