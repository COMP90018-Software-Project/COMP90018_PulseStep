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
    String gender;
//    int rankUserImageId;
//    String rankUserImageUrl;
//    boolean isUrl;

//    public RankModel(String rankNo, String rankUserName, String rankWorkoutTime, int rankUserImage, String rankLikeNum, List<String> likedUsers, String userId) {
//        this.rankNo = rankNo;
//        this.rankUserName = rankUserName;
//        this.rankWorkoutTime = rankWorkoutTime;
//        this.rankUserImage = rankUserImage;
//        this.rankLikeNum = rankLikeNum;
//        this.likedUsers = likedUsers;
//        this.rowUserId = userId;
//    }
    public RankModel(String rankNo, String rankUserName, String rankWorkoutTime, String rankLikeNum, List<String> likedUsers, String userId, String gender) {
        this.rankNo = rankNo;
        this.rankUserName = rankUserName;
        this.rankWorkoutTime = rankWorkoutTime;
        this.rankLikeNum = rankLikeNum;
        this.likedUsers = likedUsers;
        this.rowUserId = userId;
        this.gender = gender;
//        this.rankUserImage = rankUserImage;
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

//    public int getRankUserImage() {
//        return rankUserImage;
//    }
//    public boolean isUrl() {
//        return isUrl;
//    }
//
//    public String getRankUserImageUrl() {
//        return rankUserImageUrl;
//    }
//
//    public int getRankUserImageId() {
//        return rankUserImageId;
//    }

    public String getRankLikeNum() {return rankLikeNum; }

    public List<String> getLikedUsers() { return likedUsers; }

    public String getRowUserId() {return rowUserId; }

    public String getUserGender(){return gender;}
}
