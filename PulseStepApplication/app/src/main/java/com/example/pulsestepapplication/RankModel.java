package com.example.pulsestepapplication;

import java.util.List;

public class
RankModel {
    String rankNo;
    String rankUserName;
    String rankWorkoutTime;
    int rankUserImage;
    String rankLikeNum;
    List<String> likedUsers;
    String rowUserId;

    public RankModel(String rankNo, String rankUserName, String rankWorkoutTime, int rankUserImage, String rankLikeNum, List<String> likedUsers, String userId) {
        this.rankNo = rankNo;
        this.rankUserName = rankUserName;
        this.rankWorkoutTime = rankWorkoutTime;
        this.rankUserImage = rankUserImage;
        this.rankLikeNum = rankLikeNum;
        this.likedUsers = likedUsers;
        this.rowUserId = userId;
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

    public String getRankLikeNum() {return rankLikeNum; }

    public List<String> getLikedUsers() { return likedUsers; }

    public String getRowUserId() {return rowUserId; }
}
