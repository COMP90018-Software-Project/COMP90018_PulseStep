package com.example.pulsestepapplication;

import android.app.Dialog;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

public class RankingFragment extends Fragment {

    ArrayList<RankModel> rankModels = new ArrayList<>();
    int[] rankUserImages = {R.drawable.sample_profile_img,
            R.drawable.sample_profile_img,
            R.drawable.sample_profile_img,
            R.drawable.sample_profile_img,
            R.drawable.sample_profile_img,
            R.drawable.sample_profile_img,
            R.drawable.sample_profile_img,
            R.drawable.sample_profile_img,
            R.drawable.sample_profile_img,
            R.drawable.sample_profile_img};

    private TextView dailyTargetTextView;
    private String targetHours;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private String userId;


    public RankingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setUpRankModels();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        setUpRankModels();

        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_ranking, container, false);

        RecyclerView rankRecyclerView = view.findViewById(R.id.ranking_List);
        rankRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        RankListAdapter rankListAdapter = new RankListAdapter(requireContext(), rankModels);
        rankRecyclerView.setAdapter(rankListAdapter);

        // 初始化显示目标时长的 TextView
        dailyTargetTextView = view.findViewById(R.id.daily_target_hours);

        // Initialize FirebaseFirestore
        db = FirebaseFirestore.getInstance();

        // 获取当前用户的 UID
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        userId = currentUser.getUid();

        // 从 Firebase 获取当前用户的 target 并更新 UI
        fetchTargetHours();
//        fetchDailyActiveHours();

        ImageView editTarget = view.findViewById(R.id.bt_set_target);
        // 给 dailyTargetTextView 设置点击事件，触发 Dialog
        editTarget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setUpTargetPickerDialog();
            }
        });

        // switch ranking list (monthly/daily)
        MaterialButtonToggleGroup toggleButton = view.findViewById(R.id.bt_switch_rank);
        TextView titleTextView = view.findViewById(R.id.rank_title);
        toggleButton.addOnButtonCheckedListener(new MaterialButtonToggleGroup.OnButtonCheckedListener() {
            @Override
            public void onButtonChecked(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
                // Respond to button selection
                if (isChecked) {
                    switch (checkedId) {
                        case R.id.bt_daily:
                            titleTextView.setText("Daily Sports Rankings");
//                            updateRecyclerViewData("daily");
                            break;
                        case R.id.bt_monthly:
                            titleTextView.setText("Monthly Sports Rankings");
//                            updateRecyclerViewData("monthly");
                            break;
                    }
                }
            }
        });
        return view;
    }

    private void setUpRankModels(){
        String[] rankNo = getResources().getStringArray(R.array.sample_ranking_no);
        String[] rankUserNames = getResources().getStringArray(R.array.sample_ranking_user_names);
        String[] rankWorkoutTimes = getResources().getStringArray(R.array.sample_ranking_times);

        for (int i = 0; i<rankNo.length; i++){
            rankModels.add(new RankModel(rankNo[i],
                    rankUserNames[i],
                    rankWorkoutTimes[i],
                    rankUserImages[i]));
        }

    }

    /**
     * Set up the target picker dialog
     */
    private void setUpTargetPickerDialog(){
        final Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_set_target); // 使用自定义布局 dialog_time_picker.xml

        // 获取 NumberPicker 和按钮
        final NumberPicker targetPicker = dialog.findViewById(R.id.targetPicker);
        Button btnSet = dialog.findViewById(R.id.btn_set);
        Button btnCancel = dialog.findViewById(R.id.btn_cancel);

        // 设置 NumberPicker 的当前值
        targetPicker.setMinValue(1);
        targetPicker.setMaxValue(12); // 设置 1 到 12 小时

        // Handle possible null or invalid targetHours
        int targetValue = 1;  // Default value
        if (targetHours != null) {
            try {
                targetValue = Integer.parseInt(targetHours);
            } catch (NumberFormatException e) {
                targetValue = 1;  // Default value if parsing fails
            }
        }
        targetPicker.setValue(targetValue);

        // Set new target
        btnSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 获取选择的目标时间并更新
                targetHours = String.valueOf(targetPicker.getValue());
                updateTargetHours(targetHours);
                updateTargetDisplay(targetHours);
                dialog.dismiss();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();  // 取消，关闭对话框
            }
        });

        dialog.show();  // 显示对话框
    }

    /**
     * Displays the updated target.
     */
    private void updateTargetDisplay(String targetHours) {
        if (targetHours.equals("--")) {
            dailyTargetTextView.setText("-- hours");  // 如果没有目标，显示 "-- hours"
        } else {
            dailyTargetTextView.setText(targetHours + " " + (targetHours.equals("1") ? "hour" : "hours"));
        }
    }

    /**
     * Fetch the target from firestore.
     */
    private void fetchTargetHours() {
        if (userId != null) {
            DocumentReference userRef = db.collection("users").document(userId);
            userRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Object target = documentSnapshot.get("target");
                    if (target != null) {
                        targetHours = target.toString();
                    } else {
                        targetHours = "--";  // Default value
                    }
                    updateTargetDisplay(targetHours);
                }
            }).addOnFailureListener(e -> {
                targetHours = "--";  // Set default value in case of failure
                updateTargetDisplay(targetHours);
            });
        }
    }

    /**
     * Update the new target to firestore.
     */
    private void updateTargetHours(String newTarget) {
        if (userId != null) {
            // Add a new document with a generated ID
            db.collection("users").document(userId)
                    .update("target", targetHours)
                    .addOnSuccessListener(aVoid -> {
                        // Success update
                        Toast.makeText(getContext(), "New Target Has Been Set!", Toast.LENGTH_SHORT).show();

                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to update target!", Toast.LENGTH_SHORT).show();
                    });
        }
    }

}