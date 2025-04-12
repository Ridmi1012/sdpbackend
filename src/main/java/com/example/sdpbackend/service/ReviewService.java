package com.example.sdpbackend.service;

import com.example.sdpbackend.dto.ReviewRequest;
import com.example.sdpbackend.entity.Customer;
import com.example.sdpbackend.entity.Review;
import com.example.sdpbackend.repository.CustomerRepository;
import com.example.sdpbackend.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@Service
public class ReviewService {

    private static final Logger logger = Logger.getLogger(ReviewService.class.getName());

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private CustomerRepository customerRepository;

    public Review saveReview(ReviewRequest reviewRequest, String username) {
        logger.info("Saving review for user: " + username);
        Customer customer = customerRepository.findByUsername(username);

        if (customer == null) {
            logger.warning("Customer not found: " + username);
            return null;
        }

        Review review = new Review();
        review.setRating(reviewRequest.getRating());
        review.setReviewText(reviewRequest.getReview());
        review.setCustomer(customer);

        return reviewRepository.save(review);
    }

    public List<Review> getAllReviews() {
        return reviewRepository.findAll();
    }
}


