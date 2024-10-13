package com.example.pulsestepapplication.calendar;

import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import android.view.ViewTreeObserver;

import com.example.pulsestepapplication.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

//public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder> {
//
//    private List<String> dates;
//    private int selectedPosition;
//    private RecyclerView recyclerView;
//    private LinearLayoutManager layoutManager;
//    private OnDateClickListener onDateClickListener;
//
//    // 接口用于回调点击的日期
//    public interface OnDateClickListener {
//        void onDateClick(String date);
//    }
//
//    public CalendarAdapter(List<String> dates, int selectedPosition, RecyclerView recyclerView, LinearLayoutManager layoutManager, OnDateClickListener listener) {
//        this.dates = dates;
//        this.selectedPosition = selectedPosition;
//        this.recyclerView = recyclerView;
//        this.layoutManager = layoutManager;
//        this.onDateClickListener = listener;
//
//        // 滚动到选择的日期
//        if (selectedPosition != -1) {
//            recyclerView.post(() -> {
//                View itemView = layoutManager.findViewByPosition(selectedPosition);
//                if (itemView != null) {
//                    int offset = recyclerView.getWidth() - itemView.getWidth();
//                    layoutManager.scrollToPositionWithOffset(selectedPosition, offset);
//                }
//            });
//        }
//    }
//
//    @NonNull
//    @Override
//    public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.calendar_item, parent, false);
//        return new CalendarViewHolder(view);
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull CalendarViewHolder holder, int position) {
//        String date = dates.get(position);
//        holder.tvDate.setText(date);
//
//        // 处理选择状态的背景和颜色
//        if (position == selectedPosition) {
//            holder.tvDate.setBackgroundResource(R.drawable.calendar_item_background);
//            holder.tvDate.setPadding(0, 14, 0, 14);  // Ensure consistent padding
//            holder.tvDate.setTextColor(Color.WHITE);
//        } else {
//            holder.tvDate.setBackgroundResource(0);
//            holder.tvDate.setPadding(0, 14, 0, 14);  // Ensure consistent padding
//            holder.tvDate.setTextColor(Color.BLACK);
//        }
//
//        // 点击事件：通知 ProfileFragment 选中的日期
//        holder.itemView.setOnClickListener(v -> {
//            selectedPosition = position;
//            notifyDataSetChanged();
//
//            // 使用 SimpleDateFormat 格式化为 yyyy-MM-dd
//            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
//            Calendar cal = Calendar.getInstance();
//            cal.add(Calendar.DAY_OF_MONTH, position - 15); // 日期相对偏移量
//
//            String formattedDate = sdf.format(cal.getTime());
//            onDateClickListener.onDateClick(formattedDate);  // 回调，传递正确格式的日期
//        });
//    }
//
//
//    @Override
//    public int getItemCount() {
//        return dates.size();
//    }
//
//    static class CalendarViewHolder extends RecyclerView.ViewHolder {
//        TextView tvDate;
//
//        public CalendarViewHolder(@NonNull View itemView) {
//            super(itemView);
//            tvDate = itemView.findViewById(R.id.tv_date);
//        }
//    }
//}

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder> {

    private List<String> dates;
    private int selectedPosition;
    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private OnDateClickListener onDateClickListener;
    private LinearSnapHelper snapHelper;
    // 接口用于回调点击的日期
    public interface OnDateClickListener {
        void onDateClick(String date);
    }


    public CalendarAdapter(List<String> dates, int selectedPosition, RecyclerView recyclerView, LinearLayoutManager layoutManager, OnDateClickListener listener) {
        this.dates = dates;
        this.selectedPosition = selectedPosition;
        this.recyclerView = recyclerView;
        this.layoutManager = layoutManager;
        this.onDateClickListener = listener;


        // Attach SnapHelper to center items
        snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(recyclerView);

        // Scroll to the selected date (today) on initialization
        if (selectedPosition != -1 && selectedPosition < dates.size()) {
            recyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    // Ensure that the RecyclerView's width is ready
                    if (recyclerView.getWidth() > 0) {
                        int recyclerViewWidth = recyclerView.getWidth(); // Get the width of RecyclerView
                        Log.d("recyclerViewWidth", String.valueOf(recyclerViewWidth));
                        recyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this); // Remove listener after first trigger

                        // Ensure itemView has been measured and is ready
                        recyclerView.post(() -> {
                            // First scroll to the selected position to ensure the view is laid out
                           layoutManager.scrollToPosition(selectedPosition);

                            // Use post again to ensure the itemView is now ready after the scroll
                            recyclerView.postDelayed(() -> {
                                View itemView = layoutManager.findViewByPosition(selectedPosition);
                                if (itemView != null) {
                                    // Get the width of the selected item
                                    int itemViewWidth = itemView.getWidth();
                                    Log.d("itemWidth", String.valueOf(itemViewWidth));

                                    // Calculate the offset to center the itemView
                                    int offset = recyclerViewWidth - itemViewWidth;
                                    Log.d("CalculatedOffset", String.valueOf(offset));
                                    layoutManager.scrollToPositionWithOffset(selectedPosition, offset);

                                    // 这里延迟调用 SnapHelper 的调整
//                                        recyclerView.postDelayed(() -> {
//                                            int[] snapDistances = snapHelper.calculateDistanceToFinalSnap(layoutManager, itemView);
//                                            if (snapDistances != null) {
//                                                recyclerView.smoothScrollBy(snapDistances[0], snapDistances[1]);
//                                                Log.d("AAAAAAAAAAAAAAAAAA", "YYYYYYYYYYYYY");
//                                            }
//                                        }, 100);  // 100ms 延迟，确保之前的滚动完成
                                } else {
                                    Log.e("CalendarAdapter", "Item view is still null after scrolling");
                                }
                            }, 100);  // Add a slight delay to give the scroll time to complete
                        });

                    }
                }
            });
        }

    }




    @NonNull
    @Override
    public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.calendar_item, parent, false);
        return new CalendarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarViewHolder holder, int position) {
        String date = dates.get(position);
        holder.tvDate.setText(date);

        // 处理选择状态的背景和颜色
        if (position == selectedPosition) {
            holder.tvDate.setBackgroundResource(R.drawable.calendar_item_background);
            holder.tvDate.setPadding(0, 14, 0, 14);  // Ensure consistent padding
            holder.tvDate.setTextColor(Color.WHITE);
        } else {
            holder.tvDate.setBackgroundResource(0);
            holder.tvDate.setPadding(0, 14, 0, 14);  // Ensure consistent padding
            holder.tvDate.setTextColor(Color.BLACK);
        }

        // 点击事件：通知 ProfileFragment 选中的日期
        holder.itemView.setOnClickListener(v -> {
            selectedPosition = position;
            notifyDataSetChanged();

            // 使用 SimpleDateFormat 格式化为 yyyy-MM-dd
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, position - 15); // 日期相对偏移量

            String formattedDate = sdf.format(cal.getTime());
            onDateClickListener.onDateClick(formattedDate);  // 回调，传递正确格式的日期
        });
    }

    @Override
    public int getItemCount() {
        return dates.size();
    }

    static class CalendarViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate;

        public CalendarViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tv_date);
        }
    }
}


