package com.example.pulsestepapplication;

import android.content.Context;
import android.content.Intent;
import android.icu.text.SimpleDateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class RankListAdapter extends RecyclerView.Adapter<RankListAdapter.MyViewHolder> {

    Context context;
    public static ArrayList<RankModel> rankModels;
    private boolean isMonthlyRank;
    private ImageView profileImage;

    public RankListAdapter(Context context, ArrayList<RankModel> rankModels, boolean isMonthlyRank){
        this.context = context;
        this.rankModels = rankModels;
        this.isMonthlyRank = isMonthlyRank;
    }

    @NonNull
    @Override
    public RankListAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //  This is where you inflate the layout (Giving a look to our rows)
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.recyclerview_ranking_row, parent, false);
        return new RankListAdapter.MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RankListAdapter.MyViewHolder holder, int position) {
        RankModel rankModel = rankModels.get(position);
        // assigning values to the views based on the position of the recycler view
        holder.rankNo.setText(rankModels.get(position).getRankNo());
        holder.rankUserName.setText(rankModels.get(position).getRankUserName());
        holder.rankWorkoutTime.setText(rankModels.get(position).getRankWorkoutTime());
//        holder.rankUserImage.setImageResource(rankModels.get(position).getRankUserImage());
        holder.rankLikeNum.setText(rankModels.get(position).getRankLikeNum());

        // set rank user Image
        String gender = rankModels.get(position).getUserGender();
        FirebaseStorage.getInstance().getReference()
                .child("users")
                .child(rankModels.get(position).getRowUserId())
                .child("images/profile_image")
                .getDownloadUrl()
                .addOnSuccessListener(uri -> {
                    // 成功获取到图片 URL，设置用户自定义头像
//                    ProfileFragment.setProfilePic(getContext(), uri, profileImage)
                        Glide.with(context).load(uri).apply(RequestOptions.circleCropTransform()).into(holder.rankUserImage);
                        Log.d("UserImage", "userImage = yyyyyyyyyyyyyyyyyy");
                })
                .addOnFailureListener(exception -> {
                    Log.d("UserImage","userImage = nnnnnnnnnnnnnnnnnnnnnnn");
                    // 文件不存在，处理 StorageException，并根据性别设置默认头像
                    if (exception instanceof StorageException) {
                        StorageException storageException = (StorageException) exception;
                        if (storageException.getErrorCode() == StorageException.ERROR_OBJECT_NOT_FOUND) {
                            // 根据性别设置默认头像
                            if (gender != null) {
                                if (gender.equalsIgnoreCase("male")) {
                                    // 设置男性默认头像
                                    holder.rankUserImage.setImageResource(R.drawable.male_default_avatar);
                                } else if (gender.equalsIgnoreCase("female")) {
                                    // 设置女性默认头像
                                    holder.rankUserImage.setImageResource(R.drawable.female_default_avatar);
                                }else if (gender.equalsIgnoreCase("other")) {
                                    // 如果性别为other，设置通用默认头像
                                    holder.rankUserImage.setImageResource(R.drawable.default_avatar);
                                }
                            }
                        } else {
                            Log.e("ProfileFragment", "Error fetching profile image: " + exception.getMessage());
                        }
                    }
                });

        // Get current user id
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String currentUserId = currentUser.getUid();
        // get row user (target user)
        String updateUserId = rankModel.getRowUserId();

        String currentDateOrMonth = isMonthlyRank ? getCurrentMonth() : getCurrentDate();
        // Init like checkbox
        boolean isLiked = rankModel.getLikedUsers() != null && rankModel.getLikedUsers().contains(currentUserId);
        holder.btLike.setChecked(isLiked);

        // Set up like check box
        holder.btLike.setOnCheckedChangeListener((buttonView, isChecked) -> {

            if (isChecked) {
                // Add new like from firestore
                addLikeToFirestore(updateUserId, currentDateOrMonth, currentUserId, holder.rankLikeNum);
            } else {
                // Remove like from firestore
                removeLikeFromFirestore(updateUserId, currentDateOrMonth, currentUserId, holder.rankLikeNum);
            }
        });

        // Change CardView background to red if this is the current user
        // Check if the row corresponds to the current user
        if (updateUserId.equals(currentUserId)) {
            Log.d("rank row", "this is current user");
            // Set the tint color to red for the current user
            holder.rankRowView.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.user_rank_row_orange));
        }


        if(!isMonthlyRank) {
            // Set the click listener for the CardView
            holder.rankRowView.setOnClickListener(v -> {
                // Navigate to RankRowDetail activity when the row is clicked
                Intent intent = new Intent(context, RankRowDetail.class);
                intent.putExtra("userId", updateUserId);  // Pass the selected user's ID to the new activity
                context.startActivity(intent);
            });
        }
    }

    @Override
    public int getItemCount() {
//        count num of items
        return rankModels.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder{
        // grabbing the views from our layout file
        // kinda like in the onCreate method
        ImageView rankUserImage;
        TextView rankNo, rankUserName, rankWorkoutTime, rankLikeNum;
        CheckBox btLike;
        CardView rankRowView;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            rankUserImage = itemView.findViewById(R.id.rank_user_image);
            rankNo = itemView.findViewById(R.id.rank_num);
            rankUserName = itemView.findViewById(R.id.rank_user_name);
            rankWorkoutTime = itemView.findViewById(R.id.rank_workout_time);
            rankLikeNum = itemView.findViewById(R.id.num_like);
            btLike = itemView.findViewById(R.id.bt_like);
            rankRowView = itemView.findViewById(R.id.rank_row);
        }
    }

    /**
     * Add new like from firestore
     */
    private void addLikeToFirestore(String updateUserId, String currentDateOrMonth, String userId, TextView likeNumTextView) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String likeField = isMonthlyRank ? "monthlyLike" : "dailyLike";

        DocumentReference docRef = db.collection("users").document(updateUserId);
        docRef.update(likeField + "." + currentDateOrMonth, FieldValue.arrayUnion(userId))
                .addOnSuccessListener(aVoid -> {
                    // update like num display
                    updateLikeCount(docRef, currentDateOrMonth, likeField, likeNumTextView);
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error updating (increase like num) document", e);
                });
    }

    /**
     * Remove like from firestore
     */
    private void removeLikeFromFirestore(String updateUserId, String currentDateOrMonth, String userId, TextView likeNumTextView) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String likeField = isMonthlyRank ? "monthlyLike" : "dailyLike";

        DocumentReference docRef = db.collection("users").document(updateUserId);
        docRef.update(likeField + "." + currentDateOrMonth, FieldValue.arrayRemove(userId))
                .addOnSuccessListener(aVoid -> {
                    // update like num display
                    updateLikeCount(docRef, currentDateOrMonth, likeField, likeNumTextView);
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error updating (decrease like num) document", e);
                });
    }

    /**
     * Update like num display
     */
    private void updateLikeCount(DocumentReference docRef, String currentDateOrMonth, String likeField, TextView likeNumTextView) {
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            Map<String, ArrayList<String>> likeMap = (Map<String, ArrayList<String>>) documentSnapshot.get(likeField);
            if (likeMap != null && likeMap.containsKey(currentDateOrMonth)) {
                int likeCount = likeMap.get(currentDateOrMonth).size();
                likeNumTextView.setText(String.valueOf(likeCount));
            } else {
                likeNumTextView.setText("0");
            }
        });
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
        java.text.SimpleDateFormat monthFormat = new java.text.SimpleDateFormat("yyyy-MM", Locale.getDefault());
        return monthFormat.format(date);
    }

}
