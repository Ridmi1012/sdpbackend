package com.example.sdpbackend.controller;


import com.example.sdpbackend.dto.ReviewRequest;
import com.example.sdpbackend.entity.Customer;
import com.example.sdpbackend.entity.Review;
import com.example.sdpbackend.repository.CustomerRepository;
import com.example.sdpbackend.repository.ReviewRepository;
import com.example.sdpbackend.service.JWTService;
import com.example.sdpbackend.service.ReviewService;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = "http://localhost:4200")
public class ReviewController {
    private static final Logger logger = Logger.getLogger(ReviewController.class.getName());

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private JWTService jwtService;

    @PostMapping
    public ResponseEntity<?> createReview(
            @RequestBody ReviewRequest reviewRequest,
            @RequestHeader("Authorization") String authHeader) {

        try {
            logger.info("Received review request: " + reviewRequest.getRating() + ", " + reviewRequest.getReview());

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                logger.warning("Invalid authorization header");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid authorization header");
            }

            String token = authHeader.replace("Bearer ", "");

            if (!jwtService.validateToken(token)) {
                logger.warning("Invalid token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
            }

            Claims claims = jwtService.extractClaims(token);
            String username = claims.getSubject();

            logger.info("Extracted username from token: " + username);

            Review savedReview = reviewService.saveReview(reviewRequest, username);

            if (savedReview == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Customer not found");
            }

            logger.info("Review saved successfully with ID: " + savedReview.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(savedReview);

        } catch (Exception e) {
            logger.severe("Error in createReview: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllReviews() {
        try {
            return ResponseEntity.ok(reviewService.getAllReviews());
        } catch (Exception e) {
            logger.severe("Error retrieving reviews: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }
    }
