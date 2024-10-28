package com.example.pulsestepapplication;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.cardview.widget.CardView;
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
import com.google.firebase.firestore.ListenerRegistration;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Map;
import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;


public class RankingFragment extends Fragment {

    ArrayList<RankModel> rankModels = new ArrayList<>();

    private TextView dailyTargetTextView;
    private TextView dailyActiveTimeTextView;
    private TextView topNameTextView;
    private TextView topActiveTimeTextView;
    private RecyclerView rankRecyclerView;
    private ImageView editTarget;
    private ProgressBar progressBar;

    private String targetHours;
    private String userDailyActiveTime;
    private String userMonthlyActiveTime;
    private String userTotalActiveTime;
    private String currentDate;
    private String currentMonth;
    private String userId;
    private String userName;
    private boolean isMonthlyRank = false;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private ListenerRegistration registration;

    public RankingFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Initialize FirebaseFirestore
        db = FirebaseFirestore.getInstance();

        // Get current UID
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        userId = currentUser.getUid();

        // Get current date
        currentDate = getCurrentDate();
        currentMonth = getCurrentMonth();

        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_ranking, container, false);
        initView(view);

        // setTarget Dialog
        editTarget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setUpTargetPickerDialog();
            }
        });

        // Fetch target and progress data from firestore
        fetchTargetData();

        // Set up init rank list (daily)
        setUpRankModels();

        // switch ranking list (monthly/daily)
        MaterialButtonToggleGroup toggleButton = view.findViewById(R.id.bt_switch_rank);
        toggleButton.check(R.id.bt_daily);
        TextView titleTextView = view.findViewById(R.id.rank_title);
        toggleButton.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            // Respond to button selection
            if (isChecked) {
                if (!isAdded()) {
                    Log.e("RankingFragment", "Fragment not attached, skipping update.");
                    return;
                }
                switch (checkedId) {
                    case R.id.bt_daily:
                        titleTextView.setText("Daily Sports Rankings");
                        isMonthlyRank = false;
                        break;
                    case R.id.bt_monthly:
                        titleTextView.setText("Monthly Sports Rankings");
                        isMonthlyRank = true;
                        break;
                }
                Log.e("isMonthlyRank","isMonthlyRank = "+ isMonthlyRank);
                // Fetch target and progress data from firestore
                fetchTargetData();
                // Set up daily rank list
                setUpRankModels();
            }
        });
        return view;
    }

    /**
     * Init view of ranking page
     */
    private void  initView(View view) {
        // View of daily target and active time TextView
        dailyTargetTextView = view.findViewById(R.id.daily_target_hours);
        dailyActiveTimeTextView = view.findViewById(R.id.progress_detail);
        // View of progress bar
        progressBar = view.findViewById(R.id.progressBar);
        editTarget = view.findViewById(R.id.bt_set_target);

        // View of the row above rank list (you daily or monthly active time)
        topNameTextView = view.findViewById(R.id.you_name);
        topActiveTimeTextView = view.findViewById(R.id.you_time);

        // View of rank list
        rankRecyclerView = view.findViewById(R.id.ranking_List);
        rankRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    }


    /**
     * Set up the target picker dialog
     */
    private void setUpTargetPickerDialog(){
        final Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_set_target);

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
                // Get the new target and update
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

                    // convert to specific format
                    userDailyActiveTime = convertTimeFormat(userDailyActiveInHours);

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

                    // convert to specific format
                    userMonthlyActiveTime = convertTimeFormat(userMonthlyActiveInHours);

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
     * Method used to get current month in the format: YYYY-MM
     */
    private String getCurrentMonth() {
        Date date = Calendar.getInstance().getTime();
        SimpleDateFormat monthFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        return monthFormat.format(date);
    }

    /**
     * Set up rank list model for daily or monthly
     */
    private void setUpRankModels() {
        Log.d("CurrentDate", "Current Date: " + currentDate);
        rankModels.clear();

        Log.e("isMonthlyRank!!!!!!!!!!!!!","isMonthlyRank = "+ isMonthlyRank);
        String rankTypeField = isMonthlyRank ? "monthlyActive." + currentMonth : "dailyActive." + currentDate;

        // Ensure no repeat listener been registered
        if (registration != null) {
            registration.remove();
        }

        registration = db.collection("users")
                .whereGreaterThan(rankTypeField, 299)  // active time greater than 5min can join the rank competition
                .orderBy(rankTypeField, Query.Direction.DESCENDING)
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (error != null) {
                        Log.e("FetchTopUsers", "Error fetching users", error);
                        return;
                    }

                    if (queryDocumentSnapshots != null) {
                        // Check if fragment is attached
                        if (!isAdded()) {
                            Log.e("RankingFragment", "Fragment not attached, skipping update.");
                            return;
                        }
                        rankModels.clear();  // reset the rank list data when retrieving new data
                        int rankNo = 1;

                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            // Get row user name
                            String rowUserId = document.getId();
                            String userName = document.getString("fullName");
                            String gender = document.getString("gender");

                            // Get row user active time (daily or monthly)
                            Map<String, Long> activeMap = (Map<String, Long>) document.get(isMonthlyRank ? "monthlyActive" : "dailyActive");
                            if (activeMap != null && activeMap.containsKey(isMonthlyRank ? currentMonth : currentDate)) {
                                long activeInSeconds = activeMap.get(isMonthlyRank ? currentMonth : currentDate);

                                double activeInHours = convertSecondsToHours(activeInSeconds);
                                String activeTime = convertTimeFormat(activeInHours);
                                activeTime = activeTime + " " + (activeTime.equals("1") ? "hour" : "hours");

                                // Get the like num and the array list of liked user
                                Map<String, ArrayList<String>> likeMap = (Map<String, ArrayList<String>>) document.get(isMonthlyRank ? "monthlyLike" : "dailyLike");

                                long likeNum = (likeMap != null && likeMap.containsKey(isMonthlyRank ? currentMonth : currentDate)) ? likeMap.get(isMonthlyRank ? currentMonth : currentDate).size() : 0;

                                ArrayList<String> likedUsers = (likeMap != null && likeMap.containsKey(isMonthlyRank ? currentMonth : currentDate))
                                        ? likeMap.get(isMonthlyRank ? currentMonth : currentDate) : new ArrayList<>();

                                // Add the rank row detail to rank model
                                rankModels.add(new RankModel(String.valueOf(rankNo), userName, activeTime, String.valueOf(likeNum), likedUsers, rowUserId, gender));

                                Log.d("RankModels", "Added user: " + userName + ", active time: " + activeTime+"rankNo" + rankNo);

                                rankNo++;
                            }
                        }
                        // Set up adapter
                        Log.d("AdapterIsMonthly", "isMonthly = "+isMonthlyRank);
                        RankListAdapter rankListAdapter = new RankListAdapter(requireContext(), rankModels, isMonthlyRank);
                        rankRecyclerView.setAdapter(rankListAdapter);

                    }
                });
    }

    @Override
    public void onStart() {
        super.onStart();
        setUpRankModels();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }
}