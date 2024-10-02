package com.example.pulsestepapplication.calendar;

import java.util.Date;

public class Model {
    private Date date;
    private String day;
    private boolean isSelected;

    // Constructor
    public Model(Date date, String day, boolean isSelected) {
        this.date = date;
        this.day = day;
        this.isSelected = isSelected;
    }

    // Constructor with default isSelected value
    public Model(Date date, String day) {
        this(date, day, false); // Calls the main constructor with isSelected as false
    }

    // Getters and Setters
    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

}
