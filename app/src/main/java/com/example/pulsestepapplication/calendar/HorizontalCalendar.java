package com.example.pulsestepapplication.calendar;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class HorizontalCalendar {

    private ArrayList<Model> mModelItems = new ArrayList<>();
    private SimpleDateFormat mFormatter;
    private AdapterForDates adapterForDates;
    private Context context;
    private LinearSnapHelper snapHelper;
    private RecyclerView dates_rv;
    private TextView monthTextView;  // For displaying the month and year
    private boolean isUserScroll = false;  // Add flag to differentiate scroll types

    public HorizontalCalendar(DateItemClickListener dateItemClickListener, RecyclerView dates_rv, TextView monthTextView, Context context) {
        this.context = context;
        this.dates_rv = dates_rv;  // Initialize dates_rv in the constructor
        this.monthTextView = monthTextView;  // Initialize the monthTextView for month/year display
        mFormatter = new SimpleDateFormat("dd-MM-yyyy", Locale.US);

        // Initialize the Model list with 6 months of dates before and after today
        initializeDates();

        // Initialize the adapter with the Model list
        adapterForDates = new AdapterForDates(mModelItems, context, new ArrayList<>(), dateItemClickListener);
        dates_rv.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));

        // Attach the adapter to the RecyclerView
        dates_rv.setAdapter(adapterForDates);

        // Attach LinearSnapHelper to ensure items snap to the center
        snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(dates_rv);

        // Add scroll listener to select the centered item and update the month/year
        dates_rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE && isUserScroll) {
                    isUserScroll = false;  // Reset the flag after user scroll is handled
                    View centerView = snapHelper.findSnapView(recyclerView.getLayoutManager());
                    if (centerView != null) {
                        int position = recyclerView.getLayoutManager().getPosition(centerView);
                        adapterForDates.updatePosition(position);  // Center and highlight the selected date
                        // Automatically select the center date and trigger the listener
                        selectDate(position, null);
                        // Update the month and year in the TextView
                        updateMonthYear(position);  // Update the month/year display
                    }
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                isUserScroll = true;  // Detect if user is manually scrolling
            }
        });

        // Scroll to today's date after layout is fully initialized
        dates_rv.post(() -> scrollToToday(dates_rv));  // Ensures it's done after layout is ready
    }

    // Initialize dates with 6 months before and after today
    private void initializeDates() {
        Calendar calendar = Calendar.getInstance();

        // Start 6 months in the past
        calendar.add(Calendar.MONTH, -6);

        // Add dates for 12 months (6 months before and 6 months after today)
        for (int i = 0; i < 365; i++) {
            Date date = calendar.getTime();
            String day = new SimpleDateFormat("EEEE", Locale.US).format(date); // Get day name like Monday, Tuesday
            mModelItems.add(new Model(date, day));
            calendar.add(Calendar.DAY_OF_MONTH, 1); // Move to the next day
        }
    }

    // Helper method to scroll to today's date and center it
    private void scrollToToday(RecyclerView dates_rv) {
        Calendar today = Calendar.getInstance();
        int position = getDatePosition(today.getTime());

        if (position != -1) {
            dates_rv.scrollToPosition(position); // Scroll to today's position

            // Wait until the scroll completes and ensure snapping
            dates_rv.post(() -> {
                RecyclerView.LayoutManager layoutManager = dates_rv.getLayoutManager();
                View view = layoutManager.findViewByPosition(position);
                if (view != null) {
                    int[] snapDistances = snapHelper.calculateDistanceToFinalSnap(layoutManager, view);
                    if (snapDistances != null && (snapDistances[0] != 0 || snapDistances[1] != 0)) {
                        dates_rv.smoothScrollBy(snapDistances[0], snapDistances[1]);
                    }
                    // Highlight and select today's date after snapping
                    adapterForDates.updatePosition(position);
                    selectDate(position, null); // Automatically select the date
                }
            });
        }
    }

    // Method to update month and year dynamically
    private void updateMonthYear(int position) {
        // Get the date at the current position
        Date date = mModelItems.get(position).getDate();

        // Format the month and year (e.g., February 2024)
        SimpleDateFormat monthYearFormatter = new SimpleDateFormat("MMMM yyyy", Locale.US);
        String monthYearText = monthYearFormatter.format(date);

        // Update the TextView with the new month and year
        monthTextView.setText(monthYearText);
    }

    // Method to highlight and select the center date
    private void selectDate(int position, DateItemClickListener dateItemClickListener) {
        // Highlight the selected position
        adapterForDates.updatePosition(position);

        // Trigger the click listener if available (for scroll case, pass null to avoid a second callback)
        if (dateItemClickListener != null) {
            Model selectedModel = mModelItems.get(position);
            String formattedDate = mFormatter.format(selectedModel.getDate());
//            dateItemClickListener.onDateClick(formattedDate, position);
        }
    }

    // Method to highlight a selected date by position and smoothly scroll it to the center
    public void highlightSelectedDate(int position) {
        if (position != -1) {
            isUserScroll = false;  // Disable user scroll flag for click-triggered actions
            smoothScrollToCenter(dates_rv, position);
        }
    }

     // Smooth scrolls the RecyclerView to make the selected date appear in the center
    private void smoothScrollToCenter(RecyclerView recyclerView, int position) {
        recyclerView.smoothScrollToPosition(position);  // Scroll to position

        // Wait for the scroll to finish and adjust the final position to center
        recyclerView.post(() -> {
            RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
            View view = layoutManager.findViewByPosition(position);
            if (view != null) {
                int[] snapDistances = snapHelper.calculateDistanceToFinalSnap(layoutManager, view);
                if (snapDistances != null && (snapDistances[0] != 0 || snapDistances[1] != 0)) {
                    recyclerView.smoothScrollBy(snapDistances[0], snapDistances[1]);
                }
            }
        });
    }

    // Find the position of a specific date
    public int getDatePosition(Date selectedDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
        for (int i = 0; i < mModelItems.size(); i++) {
            if (sdf.format(mModelItems.get(i).getDate()).equals(sdf.format(selectedDate))) {
                return i;
            }
        }
        return -1;
    }
}
