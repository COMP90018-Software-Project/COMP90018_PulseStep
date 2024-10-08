package com.example.pulsestepapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class RankListAdapter extends RecyclerView.Adapter<RankListAdapter.MyViewHolder> {

    Context context;
    ArrayList<RankModel> rankModels;

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
//        assigning values to the views based on the position of the recycler view
        holder.rankNo.setText(rankModels.get(position).getRankNo());
        holder.rankUserName.setText(rankModels.get(position).getRankUserName());
        holder.rankWorkoutTime.setText(rankModels.get(position).getRankWorkoutTime());
        holder.rankUserImage.setImageResource(rankModels.get(position).getRankUserImage());
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
        TextView rankNo, rankUserName, rankWorkoutTime;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            rankUserImage = itemView.findViewById(R.id.rank_user_image);
            rankNo = itemView.findViewById(R.id.rank_num);
            rankUserName = itemView.findViewById(R.id.rank_user_name);
            rankWorkoutTime = itemView.findViewById(R.id.rank_workout_time);

        }
    }
}
