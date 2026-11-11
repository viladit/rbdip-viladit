package com.rbdip.bookstore.review;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final PurchaseVerification purchaseVerification;

    public ReviewService(ReviewRepository reviewRepository, PurchaseVerification purchaseVerification) {
        this.reviewRepository = reviewRepository;
        this.purchaseVerification = purchaseVerification;
    }

    public Review addReview(Long productId, String authorName, Integer rating, String comment) {
        purchaseVerification.hasRecordedPurchases();
        Review review = new Review(productId, authorName == null ? "anonymous" : authorName, rating, comment);
        return reviewRepository.save(review);
    }

    public List<Review> listReviews(Long productId) {
        return reviewRepository.findByProductId(productId);
    }
}
