package com.example.pulsestepapplication;

import android.content.Context;
import android.icu.text.SimpleDateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RankListAdapter extends RecyclerView.Adapter<RankListAdapter.MyViewHolder> {

    Context context;
    public static ArrayList<RankModel> rankModels;

    public RankListAdapter(Context context, ArrayList<RankModel> rankModels){
        this.context = context;
        this.rankModels = rankModels;
    }

    @NonNull
    @Override
    public RankListAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        This is where you inflate the layout (Giving a look to our rows)
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.recyclerview_ranking_row, parent, false);
        return new RankListAdapter.MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RankListAdapter.MyViewHolder holder, int position) {
        RankModel rankModel = rankModels.get(position);
//        assigning values to the views based on the position of the recycler view
        holder.rankNo.setText(rankModels.get(position).getRankNo());
        holder.rankUserName.setText(rankModels.get(position).getRankUserName());
        holder.rankWorkoutTime.setText(rankModels.get(position).getRankWorkoutTime());
        holder.rankUserImage.setImageResource(rankModels.get(position).getRankUserImage());
        holder.rankLikeNum.setText(rankModels.get(position).getRankLikeNum());

//        // 取得当前用户的 userId
//        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        String currentUserId = currentUser.getUid();
//
//        // 初始化 CheckBox 的选中状态
//        boolean isLiked = rankModel.getLikedUsers() != null && rankModel.getLikedUsers().contains(currentUserId);
//        holder.btLike.setChecked(isLiked);
//
//        // 设置 CheckBox 的点击事件
//        holder.btLike.setOnCheckedChangeListener((buttonView, isChecked) -> {
//            String currentDate = getCurrentDate();  // 获取当前日期
//            String userName = rankModel.getRankUserName();  // 使用用户名或其他唯一标识符
//
//            if (isChecked) {
//                // 添加 userId 到 Firestore 数组
//                addLikeToFirestore(userName, currentDate, currentUserId);
//            } else {
//                // 从 Firestore 数组中移除 userId
//                removeLikeFromFirestore(userName, currentDate, currentUserId);
//            }
//        });
    }

    @Override
    public int getItemCount() {
//        count num of items
        return rankModels.size();
    }

    // 更新数据并通知适配器数据变化
    public void updateData(List<String> newData) {
        List<String> mData = newData;
        notifyDataSetChanged();  // 通知适配器数据已更改
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder{
//      grabbing the views from our layout file
//      kinda like in the onCreate method

        ImageView rankUserImage;
        TextView rankNo, rankUserName, rankWorkoutTime, rankLikeNum;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            rankUserImage = itemView.findViewById(R.id.rank_user_image);
            rankNo = itemView.findViewById(R.id.rank_num);
            rankUserName = itemView.findViewById(R.id.rank_user_name);
            rankWorkoutTime = itemView.findViewById(R.id.rank_workout_time);
            rankLikeNum = itemView.findViewById(R.id.num_like);
//            btLike = itemView.findViewById(R.id.bt_like);
        }
    }

}
