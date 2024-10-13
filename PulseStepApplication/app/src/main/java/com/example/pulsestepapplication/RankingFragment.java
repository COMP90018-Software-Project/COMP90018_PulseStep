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
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.w3c.dom.Text;

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
    private TextView topNameTextView;
    private TextView topActiveTimeTextView;
    private TextView rankUserNameView;
    private TextView rankUserImageView;
    private TextView rankUserRankNoView;
    private TextView rankUserActiveTimeView;
    private TextView rankLikeNumView;
    private RecyclerView rankRecyclerView;
    private String targetHours;
    private String userDailyActiveTime;
    private String userMonthlyActiveTime;
    private String userTotalActiveTime;
    private String currentDate;
    private String currentMonth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private String userId;
    private String userName;
    private ProgressBar progressBar;
    private boolean isMonthlyRank;
    private int dailyLikeInt;
    private int monthlyLikeInt;


    public RankingFragment() {
        // Required empty public constructor
    }

//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setUpRankModels();
//    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Initialize FirebaseFirestore
        db = FirebaseFirestore.getInstance();

        // 获取当前用户的 UID
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        userId = currentUser.getUid();

        // Get current date
        currentDate = getCurrentDate();
        currentMonth = getCurrentMonth();

        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_ranking, container, false);

//        initRankingListView(view);
        rankRecyclerView = view.findViewById(R.id.ranking_List);
        rankRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

//        RankListAdapter rankListAdapter = new RankListAdapter(requireContext(), rankModels);
//        rankRecyclerView.setAdapter(rankListAdapter);

        // View of daily target and active time TextView
        dailyTargetTextView = view.findViewById(R.id.daily_target_hours);
        dailyActiveTimeTextView = view.findViewById(R.id.progress_detail);

        // View of the row above rank list (you daily or monthly active time)
        topNameTextView = view.findViewById(R.id.you_name);
        topActiveTimeTextView = view.findViewById(R.id.you_time);

        // View of progress bar
        progressBar = view.findViewById(R.id.progressBar);

        ImageView editTarget = view.findViewById(R.id.bt_set_target);
        // setTarget Dialog
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
                            isMonthlyRank = false;
//                            updateRecyclerViewData("daily");
                            // Fetch target and progress data from firestore
                            fetchTargetData();
                            break;
                        case R.id.bt_monthly:
                            titleTextView.setText("Monthly Sports Rankings");
                            isMonthlyRank = true;
//                            updateRecyclerViewData("monthly");
                            // Fetch target and progress data from firestore
                            fetchTargetData();
                            break;
                    }
                }
            }
        });

        // Fetch target and progress data from firestore
        fetchTargetData();

        setUpDailyRankModels();
//        fetchDailyTopUsers();

        return view;
    }

    /**
     * Set up the target picker dialog
     */
    private void setUpTargetPickerDialog(){
        final Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_set_target); // 使用自定义布局 dialog_time_picker.xml

        // Get NumberPicker and button
        final NumberPicker targetPicker = dialog.findViewById(R.id.targetPicker);
        Button btnSet = dialog.findViewById(R.id.btn_set);
        Button btnCancel = dialog.findViewById(R.id.btn_cancel);

        // Set picked num in NumberPicker
        targetPicker.setMinValue(1);
        targetPicker.setMaxValue(12);

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

        // close dialog when click cancel
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    /**
     * Displays the updated target.
     */
    private void updateTargetDisplay(String targetHours) {
        if (targetHours.equals("--")) {
            dailyTargetTextView.setText("-- hours");
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
     * Displays the updated the username and active time on the row above rank list.
     */
    private void updateTopRowDisplayDisplay(String userName, String totalActiveTime) {
        if (totalActiveTime != null) {
            topActiveTimeTextView.setText(totalActiveTime + " " + (totalActiveTime.equals("1") ? "hour" : "hours"));
        } else {
            topActiveTimeTextView.setText("0 hours");
        }
        topNameTextView.setText(userName);
    }


    /**
     * Fetch the target from firestore.
     */
    private void fetchTargetData() {
        if (userId != null) {
            DocumentReference userRef = db.collection("users").document(userId);
//            userRef.get().addOnSuccessListener(documentSnapshot -> {
            // using addSnapshotListener to listen the data change in database
            userRef.addSnapshotListener((documentSnapshot, error) -> {
                if (error != null) {
                    Log.w("RankingFragment", "Listen failed.", error);
                    return;
                }
                if (documentSnapshot.exists()) {
                    Object target = documentSnapshot.get("target");
                    Object fullName = documentSnapshot.get("fullName");

                    Map<String, Long> userDailyActiveMap = (Map<String, Long>) documentSnapshot.get("dailyActive");
                    Map<String, Long> userMonthlyActiveMap = (Map<String, Long>) documentSnapshot.get("monthlyActive");

                    // init active time in hours to 0
                    double userDailyActiveInHours = 0;
                    double userMonthlyActiveInHours = 0;

                    // get today active time and convert to hours
                    if (userDailyActiveMap != null && userDailyActiveMap.containsKey(currentDate)
                            && userMonthlyActiveMap != null && userMonthlyActiveMap.containsKey(currentMonth)) {
                        long userDailyActiveInSeconds = userDailyActiveMap.get(currentDate);
                        userDailyActiveInHours = convertSecondsToHours(userDailyActiveInSeconds);
                        long useMonthlyActiveInSeconds = userMonthlyActiveMap.get(currentMonth);
                        userMonthlyActiveInHours = convertSecondsToHours(useMonthlyActiveInSeconds);

                    }
//                    dailyActiveTime = Double.toString(dailyActiveInHours);
                    // convert to specific format
                    userDailyActiveTime = convertTimeFormat(userDailyActiveInHours);
//                    if (userDailyActiveInHours == Math.floor(userDailyActiveInHours)) {
//                        // if int
//                        dailyActiveTime = String.format(Locale.getDefault(), "%.0f", userDailyActiveInHours);
//                    } else {
//                        // if decimal
//                        dailyActiveTime = String.format(Locale.getDefault(), "%.1f", userDailyActiveInHours);
//                    }
                    // Update daily active time UI
                    updateDailyActiveDisplay(userDailyActiveTime);

                    // Update target hours UI
                    if (target != null) {
                        targetHours = target.toString();
                    } else {
                        targetHours = "--";  // Default value
                    }
                    updateTargetDisplay(targetHours);

                    // update daily progress - ProgressBar
                    if (!targetHours.equals("--")) {
                        double targetInHours = Double.parseDouble(targetHours);  // Convert target hours to double
                        int progress = (int) ((userDailyActiveInHours / targetInHours) * 100);  // Calculate progress percentage
                        progressBar.setProgress(progress);  // Update progress bar
                    }

                    // update the row above rank list
//                    // init dailyActiveTime to 0
//                    double monthlyActiveInHours = 0;
//                    // get today active time and convert to hours
//                    if (monthlyActiveMap != null && monthlyActiveMap.containsKey(currentMonth)) {
//                        long monthlyActiveInSeconds = monthlyActiveMap.get(currentMonth);
//                        monthlyActiveInHours = convertSecondsToHours(monthlyActiveInSeconds);
//                    }
//                    dailyActiveTime = Double.toString(dailyActiveInHours);
                    // convert to specific format
                    userMonthlyActiveTime = convertTimeFormat(userMonthlyActiveInHours);
//                    if (userMonthlyActiveInHours == Math.floor(userMonthlyActiveInHours)) {
//                        // if int
//                        userMonthlyActiveTime = String.format(Locale.getDefault(), "%.0f", userMonthlyActiveInHours);
//                    } else {
//                        // if decimal
//                        userMonthlyActiveTime = String.format(Locale.getDefault(), "%.1f", userMonthlyActiveInHours);
//                    }

                    if (fullName != null) {
                        userName = fullName.toString();
                    } else {
                        userName = "--";  // Default value
                    }

                    if (isMonthlyRank){
                        userTotalActiveTime = userMonthlyActiveTime;
                    } else {
                        userTotalActiveTime = userDailyActiveTime;
                    }
                    updateTopRowDisplayDisplay(userName, userTotalActiveTime);
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


    /**
     * Convert seconds to hours
     */
    private double convertSecondsToHours(long seconds) {
        return seconds / 3600.0;
    }

    private String convertTimeFormat(double time){
        String convertedTime;
        if (time == Math.floor(time)) {
            // if int
            convertedTime = String.format(Locale.getDefault(), "%.0f", time);
        } else {
            // if decimal
            convertedTime = String.format(Locale.getDefault(), "%.1f", time);
        }
        return convertedTime;
    }
    /**
     * Method used to get current date in the format: YYYY-MM-DD
     */
    private String getCurrentDate() {
        Date date = Calendar.getInstance().getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return dateFormat.format(date);
    }

    /**
     * Method used to get current date in the format: YYYY-MM-DD
     */
    private String getCurrentMonth() {
        Date date = Calendar.getInstance().getTime();
        SimpleDateFormat monthFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        return monthFormat.format(date);
    }

//    private void  initRankingListView(View view) {
//        rankUserNameView = view.findViewById(R.id.rank_user_name);
//        rankUserImageView = view.findViewById(R.id.rank_user_image);
//        rankUserActiveTimeView = view.findViewById(R.id.rank_workout_time);
//        rankUserRankNoView = view.findViewById(R.id.rank_num);
//        rankLikeNumView = view.findViewById(R.id.num_like);
//        rankRecyclerView = view.findViewById(R.id.ranking_List);
//    }

    private void setUpDailyRankModels(){
        Log.d("CurrentDate", "Current Date: " + currentDate);
        rankModels.clear();
        db.collection("users")
                .whereGreaterThan("dailyActive." + currentDate,299)
                .orderBy("dailyActive." + currentDate, Query.Direction.DESCENDING)
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                            if (error != null) {
                                Log.e("FetchTopUsers", "Error fetching daily active users", error);
                                return;
                            }

                            if (queryDocumentSnapshots != null) {
                                rankModels.clear();  // 每次获取新数据时清空旧数据
                                int rankNo = 1;
                                // 遍历查询结果
                                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                    // 获取用户的 dailyActive 值
                                    Map<String, Long> dailyActiveMap = (Map<String, Long>) document.get("dailyActive");
                                    if (dailyActiveMap != null && dailyActiveMap.containsKey(currentDate)) {
                                        long dailyActiveInSeconds = dailyActiveMap.get(currentDate);
                                        double dailyActiveInHours = convertSecondsToHours(dailyActiveInSeconds);
                                        String dailyActiveTime = convertTimeFormat(dailyActiveInHours);
                                        dailyActiveTime = dailyActiveTime + " " + (dailyActiveTime.equals("1") ? "hour" : "hours");
                                        // 获取用户的其他信息，例如姓名
                                        String userName = document.getString("fullName");

                                        Map<String, Long> dailyLikeMap = (Map<String, Long>) document.get("dailyLike");
                                        long dailyLikeNum = (dailyLikeMap != null && dailyLikeMap.containsKey(currentDate))
                                                ? dailyLikeMap.get(currentDate) : 0;

                                        // 将用户数据添加到 rankModels 列表
                                        rankModels.add(new RankModel(String.valueOf(rankNo), userName,
                                                dailyActiveTime, R.drawable.sample_profile_img, String.valueOf(dailyLikeNum)));

                                        Log.d("RankModels", "Added user: " + userName + ", active time: " + dailyActiveTime);

                                        rankNo++;
                                    }
                                }

                                // 设置适配器，如果尚未设置
                                if (rankRecyclerView.getAdapter() == null) {
                                    RankListAdapter rankListAdapter = new RankListAdapter(requireContext(), rankModels);
                                    rankRecyclerView.setAdapter(rankListAdapter);
                                } else {
                                    // 更新数据并通知适配器
                                    rankRecyclerView.getAdapter().notifyDataSetChanged();
                                }
                            }

                });

    }

    //    private void setUpRankModels(){
//        String[] rankNo = getResources().getStringArray(R.array.sample_ranking_no);
//        String[] rankUserNames = getResources().getStringArray(R.array.sample_ranking_user_names);
//        String[] rankWorkoutTimes = getResources().getStringArray(R.array.sample_ranking_times);
//
//        for (int i = 0; i<rankNo.length; i++){
//            rankModels.add(new RankModel(rankNo[i],
//                    rankUserNames[i],
//                    rankWorkoutTimes[i],
//                    rankUserImages[i]));
//        }
//
//    }

}