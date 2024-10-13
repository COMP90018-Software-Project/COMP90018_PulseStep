package com.example.pulsestepapplication;

import android.app.Dialog;
import android.os.Bundle;

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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Map;
import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;


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
    private TextView dailyActiveTimeTextView;
    private String targetHours;
    private String dailyActiveTime;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private String userId;
    private ProgressBar progressBar;

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

        // View of daily target and active time TextView
        dailyTargetTextView = view.findViewById(R.id.daily_target_hours);
        dailyActiveTimeTextView = view.findViewById(R.id.progress_detail);

        // View of progress bar
        progressBar = view.findViewById(R.id.progressBar);

        // Initialize FirebaseFirestore
        db = FirebaseFirestore.getInstance();

        // 获取当前用户的 UID
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        userId = currentUser.getUid();

        // 从 Firebase 获取当前用户的 target 并更新 UI
        fetchTargetData();
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
     * Displays the updated daily active time.
     */
    private void updateDailyActiveDisplay(String dailyActiveTime) {
        if (dailyActiveTime != null) {
            dailyActiveTimeTextView.setText(dailyActiveTime + "/");
        } else {
            dailyActiveTimeTextView.setText("0/");
        }
    }

    /**
     * Fetch the target from firestore.
     */
    private void fetchTargetData() {
        if (userId != null) {
            DocumentReference userRef = db.collection("users").document(userId);
//            userRef.get().addOnSuccessListener(documentSnapshot -> {
            // 使用 addSnapshotListener 来监听数据库的变动
            userRef.addSnapshotListener((documentSnapshot, error) -> {
                if (error != null) {
                    Log.w("RankingFragment", "Listen failed.", error);
                    return;
                }
                if (documentSnapshot.exists()) {
                    // Update daily active time UI
                    Map<String, Long> dailyActiveMap = (Map<String, Long>) documentSnapshot.get("dailyActive");
                    // Get current date
                    String today = getCurrentDate();
                    // init dailyActiveTime to 0
                    double dailyActiveInHours = 0;
                    // get today active time and convert to hours
                    if (dailyActiveMap != null && dailyActiveMap.containsKey(today)) {
                        long dailyActiveInSeconds = dailyActiveMap.get(today);
                        dailyActiveInHours = convertSecondsToHours(dailyActiveInSeconds);
                    }
//                    dailyActiveTime = Double.toString(dailyActiveInHours);
                    // convert to specific format
                    if (dailyActiveInHours == Math.floor(dailyActiveInHours)) {
                        // if int
                        dailyActiveTime = String.format(Locale.getDefault(), "%.0f", dailyActiveInHours);
                    } else {
                        // if decimal
                        dailyActiveTime = String.format(Locale.getDefault(), "%.1f", dailyActiveInHours);
                    }
                    updateDailyActiveDisplay(dailyActiveTime);

                    // Update target hours UI
                    Object target = documentSnapshot.get("target");
                    if (target != null) {
                        targetHours = target.toString();
                    } else {
                        targetHours = "--";  // Default value
                    }
                    updateTargetDisplay(targetHours);

                    // update daily progress - ProgressBar
                    if (!targetHours.equals("--")) {
                        double targetInHours = Double.parseDouble(targetHours);  // Convert target hours to double
                        int progress = (int) ((dailyActiveInHours / targetInHours) * 100);  // Calculate progress percentage
                        progressBar.setProgress(progress);  // Update progress bar
                    }
                }
            });
//                    .addOnFailureListener(e -> {
//                targetHours = "--";  // Set default value in case of failure
//                dailyActiveTime = "0";
//                updateTargetDisplay(targetHours);
//                updateDailyActiveDisplay(dailyActiveTime);
//            });
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

    // 将秒转换为小时，并保留小数点后一位
    private double convertSecondsToHours(long seconds) {
        return seconds / 3600.0; // 将秒转换为小时
    }

    // 获取当前日期的函数，返回格式 "yyyy-MM-dd"
    private String getCurrentDate() {
        Date date = Calendar.getInstance().getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return dateFormat.format(date); // YYYY-MM-DD 格式
    }
}