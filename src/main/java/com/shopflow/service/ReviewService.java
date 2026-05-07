package com.shopflow.service;

import com.shopflow.dto.request.ReviewRequest;
import com.shopflow.dto.response.ReviewResponse;
import com.shopflow.entity.Order;
import com.shopflow.entity.enums.OrderStatus;
import com.shopflow.entity.Product;
import com.shopflow.entity.Review;
import com.shopflow.entity.User;
import com.shopflow.repository.OrderRepository;
import com.shopflow.repository.ProductRepository;
import com.shopflow.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final AuthService authService;

    // ✅ POST /api/reviews - Poster un avis (achat vérifié)
    public ReviewResponse createReview(ReviewRequest request) {
        User customer = authService.getCurrentUser();
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Produit non trouvé"));

        // ✅ Vérifier si le client a acheté ET reçu le produit
        boolean hasPurchased = orderRepository.findAll().stream()
                .anyMatch(order -> order.getCustomer().getId().equals(customer.getId())
                        && order.getStatus() == OrderStatus.DELIVERED
                        && order.getItems().stream()
                        .anyMatch(item -> item.getProduct().getId().equals(product.getId())));

        if (!hasPurchased) {
            throw new RuntimeException("Vous ne pouvez laisser un avis que sur un produit que vous avez acheté et reçu");
        }

        // ✅ Vérifier si l'utilisateur a déjà laissé un avis
        if (reviewRepository.existsByCustomerAndProduct(customer, product)) {
            throw new RuntimeException("Vous avez déjà laissé un avis pour ce produit");
        }

        Review review = Review.builder()
                .customer(customer)
                .product(product)
                .rating(request.getRating())
                .comment(request.getComment())
                .approved(false)  // En attente de modération
                .build();

        Review saved = reviewRepository.save(review);
        log.info("Avis créé pour le produit: {} par {}", product.getName(), customer.getEmail());

        // ✅ Mettre à jour la note moyenne du produit
        updateProductAverageRating(product);

        return toResponse(saved);
    }

    // ✅ GET /api/reviews/product/{productId} - Avis approuvés d'un produit
    public Page<ReviewResponse> getProductReviews(Long productId, Pageable pageable) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Produit non trouvé"));

        return reviewRepository.findByProductAndApprovedTrue(product, pageable)
                .map(this::toResponse);
    }

    // ✅ PUT /api/reviews/{id}/approve - Approuver (ADMIN)
    public ReviewResponse approveReview(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Avis non trouvé"));

        review.setApproved(true);
        Review saved = reviewRepository.save(review);
        log.info("Avis approuvé pour le produit: {}", review.getProduct().getName());

        // ✅ Mettre à jour la note moyenne
        updateProductAverageRating(review.getProduct());

        return toResponse(saved);
    }

    // ✅ DELETE /api/reviews/{id} - Supprimer un avis (ADMIN)
    public void deleteReview(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Avis non trouvé"));

        Product product = review.getProduct();
        reviewRepository.delete(review);
        log.info("Avis supprimé pour le produit: {}", product.getName());

        // ✅ Mettre à jour la note moyenne
        updateProductAverageRating(product);
    }

    // ✅ GET /api/reviews/pending - Avis en attente (ADMIN)
    public Page<ReviewResponse> getPendingReviews(Pageable pageable) {
        return reviewRepository.findByApprovedFalse(pageable)
                .map(this::toResponse);
    }

    // ✅ Mettre à jour la note moyenne du produit
    private void updateProductAverageRating(Product product) {
        Double averageRating = reviewRepository.getAverageRatingByProduct(product);
        // La note moyenne est calculée dynamiquement via la méthode getAverageRating()
        // Pas besoin de stocker dans la base
        log.debug("Note moyenne mise à jour pour {}: {}", product.getName(), averageRating);
    }

    private ReviewResponse toResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .customerName(review.getCustomer().getFirstName() + " " + review.getCustomer().getLastName())
                .customerId(review.getCustomer().getId())
                .productId(review.getProduct().getId())
                .productName(review.getProduct().getName())
                .rating(review.getRating())
                .comment(review.getComment())
                .approved(review.isApproved())
                .createdAt(review.getCreatedAt())
                .build();
    }
}