package com.example.pulsestepapplication;

public class RankModel {
    String rankNo;
    String rankUserName;
    String rankWorkoutTime;
    int rankUserImage;

    public RankModel(String rankNo, String rankUserName, String rankWorkoutTime, int rankUserImage) {
        this.rankNo = rankNo;
        this.rankUserName = rankUserName;
        this.rankWorkoutTime = rankWorkoutTime;
        this.rankUserImage = rankUserImage;
    }

    public String getRankNo() {
        return rankNo;
    }

    public String getRankUserName() {
        return rankUserName;
    }

    public String getRankWorkoutTime() {
        return rankWorkoutTime;
    }

    public int getRankUserImage() {
        return rankUserImage;
    }
}
