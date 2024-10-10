package com.example.pulsestepapplication;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.MemoryCacheSettings;
import com.google.firebase.firestore.PersistentCacheSettings;
import com.google.firebase.firestore.Source;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
            R.drawable.sample_profile_img};

    private TextView dailyTargetTextView;
    private int selectedTargetHours;
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

        db = FirebaseFirestore.getInstance();

        // 获取当前用户的 UID
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        userId = currentUser.getUid();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_ranking, container, false);

        RecyclerView rankRecyclerView = view.findViewById(R.id.ranking_List);
        rankRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));


        RankListAdapter rankListAdapter = new RankListAdapter(requireContext(), rankModels);
        rankRecyclerView.setAdapter(rankListAdapter);

        // Set Target Dialog
        // 从 Firebase 获取当前用户的 target 并更新 UI
//        getCurrentTargetFromFirestore();
//        updateTargetDisplay();
        // 初始化显示目标时长的 TextView
        dailyTargetTextView = view.findViewById(R.id.daily_target_hours);
        getCurrentTargetFromFirestore();

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
        targetPicker.setValue(selectedTargetHours);

        // Set new target
        btnSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 获取选择的目标时间并更新
                selectedTargetHours = targetPicker.getValue();
                updateTargetDisplay();

                // Add a new document with a generated ID
                db.collection("users").document(userId)
                        .update("target", selectedTargetHours)
                        .addOnSuccessListener(aVoid -> {
                            // Success update
                            Toast.makeText(getContext(), "Target updated successfully!", Toast.LENGTH_SHORT).show();

                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Failed to update target!", Toast.LENGTH_SHORT).show();
                        });

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

    // 更新目标时长显示
    private void updateTargetDisplay() {

        String targetText = selectedTargetHours + " "+ (selectedTargetHours == 1 ? "hour" : "hours");
        dailyTargetTextView.setText(targetText);
    }

    // 从 Firestore 获取当前用户的目标值
    private void getCurrentTargetFromFirestore() {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // 获取 target 字段的值
                        Long target = documentSnapshot.getLong("target");
                        if (target != null) {
                            selectedTargetHours = target.intValue();  // 将 Firebase 获取到的目标时间设置为当前值
                            updateTargetDisplay();  // 更新目标显示
                        } else {
                            // 如果 target 不存在，设置默认值为 1
                            selectedTargetHours = 1;
                            updateTargetDisplay();  // 设置默认值
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to load target!", Toast.LENGTH_SHORT).show();
                });
    }

}