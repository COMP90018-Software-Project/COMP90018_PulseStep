//package com.example.pulsestepapplication.calendar;
//
//import android.content.Context;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.TextView;
//
//import androidx.core.content.ContextCompat;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.example.pulsestepapplication.R;
//import com.example.pulsestepapplication.databinding.DateItemBinding;
//
//import java.util.ArrayList;
//import java.util.Calendar;
//import java.util.Date;
//
//
//public class AdapterForDates extends RecyclerView.Adapter<AdapterForDates.ViewHolder> {
//
//    private ArrayList<Model> mModelItems;
//    private Context mContext;
//    private DateItemClickListener dateItemClickListener;
//    private int pos = -1; // Keeps track of the selected position
//    public static String mMonth;
//
//    // Constructor
//    public AdapterForDates(ArrayList<Model> mModelItems, Context mContext, ArrayList<String> mDatesList, DateItemClickListener dateItemClickListener) {
//        this.mModelItems = mModelItems;  // List of Models
//        this.mContext = mContext;
//        this.dateItemClickListener = dateItemClickListener;
//    }
//
//    @Override
//    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
//        DateItemBinding binding = DateItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
//        return new ViewHolder(binding);
//    }
//
//    @Override
//    public int getItemCount() {
//        return mModelItems.size();
//    }
//
//    @Override
//    public void onBindViewHolder(ViewHolder viewHolder, int position) {
//        // Get the Model data
//        Model model = mModelItems.get(position);
//
//        // Get the day and date from the model
//        Date date = model.getDate();
//        Calendar calendar = Calendar.getInstance();
//        calendar.setTime(date);
//        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
//
//        // Set the day and date to the viewHolder
//        viewHolder.date.setText(String.valueOf(dayOfMonth));  // Numeric day
//        viewHolder.day.setText(model.getDay().substring(0, 3));  // Day of the week (e.g., Mon)
//
//        // Handle click event to center the clicked date and select it
//        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                dateItemClickListener.onDateClick(model.getDate().toString(), position);  // Trigger click listener
//                pos = position;  // Update the selected position
//                notifyDataSetChanged();  // Refresh the list to highlight the selected date
//            }
//        });
//
//        // Update the selection state
//        if (pos == position) {
//            viewHolder.date.setTextColor(ContextCompat.getColor(mContext, R.color.black));
//            viewHolder.imageView.setBackground(ContextCompat.getDrawable(mContext, R.drawable.date_highlighter));
//        } else {
//            viewHolder.date.setTextColor(ContextCompat.getColor(mContext, R.color.gray));
//            viewHolder.imageView.setBackground(null);
//        }
//    }
//
//    // ViewHolder class
//    public static class ViewHolder extends RecyclerView.ViewHolder {
//        public final TextView date;
//        public final TextView day;
//        public final View imageView;
//
//        public ViewHolder(DateItemBinding binding) {
//            super(binding.getRoot());
//            date = binding.date;
//            day = binding.day;
//            imageView = binding.circleImageView;
//        }
//    }
//
//    // Method to update selected position
//    public void updatePosition(int pos) {
//        this.pos = pos;
//        notifyDataSetChanged();
//    }
//}

package com.example.pulsestepapplication.calendar;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pulsestepapplication.R;
import com.example.pulsestepapplication.databinding.DateItemBinding;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;


public class AdapterForDates extends RecyclerView.Adapter<AdapterForDates.ViewHolder> {

    private ArrayList<Model> mModelItems;
    private Context mContext;
    private DateItemClickListener dateItemClickListener;
    private int pos = -1; // Keeps track of the selected position

    // Constructor
    public AdapterForDates(ArrayList<Model> mModelItems, Context mContext, ArrayList<String> mDatesList, DateItemClickListener dateItemClickListener) {
        this.mModelItems = mModelItems;  // List of Models
        this.mContext = mContext;
        this.dateItemClickListener = dateItemClickListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        DateItemBinding binding = DateItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public int getItemCount() {
        return mModelItems.size();
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        // Get the Model data
        Model model = mModelItems.get(position);

        // Get the day and date from the model
        Date date = model.getDate();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

        // Set the day and date to the viewHolder
        viewHolder.date.setText(String.valueOf(dayOfMonth));  // Numeric day
        viewHolder.day.setText(model.getDay().substring(0, 3));  // Day of the week (e.g., Mon)

        // Handle click event to center the clicked date and select it
        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dateItemClickListener.onDateClick(model.getDate().toString(), position);  // Trigger click listener
                pos = position;  // Update the selected position
                notifyDataSetChanged();  // Refresh the list to highlight the selected date
            }
        });

        // Update the selection state
        if (pos == position) {
            viewHolder.date.setTextColor(ContextCompat.getColor(mContext, R.color.black));
            viewHolder.imageView.setBackground(ContextCompat.getDrawable(mContext, R.drawable.date_highlighter));
        } else {
            viewHolder.date.setTextColor(ContextCompat.getColor(mContext, R.color.gray));
            viewHolder.imageView.setBackground(null);
        }
    }

    // ViewHolder class
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView date;
        public final TextView day;
        public final View imageView;

        public ViewHolder(DateItemBinding binding) {
            super(binding.getRoot());
            date = binding.date;
            day = binding.day;
            imageView = binding.circleImageView;
        }
    }

    // Method to update selected position
    public void updatePosition(int pos) {
        this.pos = pos;
        notifyDataSetChanged();
    }
}