package com.example.dodam.data;

public class ReviewItemData {
    private String userName;    // 유저 이름
    private String userInfo;    // 유저 정보
    private String writeDate;   // 작성일
    private float rate;        // 평점
    private String content;     // 리뷰 내용
    private int like;           // 좋아요 수
    private int dislike;        // 싫어요 수

    // Firebase용
    public ReviewItemData() {

    }

    public ReviewItemData(String userName, String userInfo, String writeDate, float rate, String content) {
        this.userName = userName;
        this.userInfo = userInfo;
        this.writeDate = writeDate;
        this.rate = rate;
        this.content = content;

        like = 0;
        dislike = 0;
    }

    // 유저 이름 설정
    public void setUserName(String userName) {
        this.userName = userName;
    }

    // 유저 이름 반환
    public String getUserName() {
        return userName;
    }

    // 유저 정보 설정
    public void setUserInfo(String userInfo) { this.userInfo = userInfo; }

    // 유저 정보 반환
    public String getUserInfo() { return userInfo; }

    // 작성일 설정
    public void setWriteDate(String writeDate) { this.writeDate = writeDate; }

    // 작성일 반환
    public String getWriteDate() { return writeDate; }

    // 평점 설정
    public void setRate(float rate) { this.rate = rate; }

    // 평점 반환
    public float getRate() { return rate; }

    // 리뷰 내용 설정
    public void setContent(String content) { this.content = content; }

    // 리뷰 내용 반환
    public String getContent() { return content; }

    // 좋아요 수 설정
    public void setLike(int like) { this.like = like; }

    // 좋아요 수 반환
    public int getLike() { return like; }

    // 싫어요 수 설정
    public void setDislike(int dislike) { this.dislike = dislike; }

    // 싫어요 수 반환
    public int getDislike() { return dislike; }
}
