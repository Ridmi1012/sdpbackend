package com.example.sdpbackend.dto;

public class ReviewRequest {
    private Integer rating;  // Rating should be an Integer
    private String review;   // Review text



    // Getters and Setters
    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getReview() {
        return review;
    }

    public void setReview(String review) {
        this.review = review;
    }
}
