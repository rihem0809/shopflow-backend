package com.shopflow.service;

import com.shopflow.dto.request.ProductRequest;
import com.shopflow.dto.request.ProductVariantRequest;
import com.shopflow.dto.response.ProductResponse;
import com.shopflow.dto.response.ProductVariantResponse;
import com.shopflow.entity.*;
import com.shopflow.repository.*;
import com.shopflow.specification.ProductSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final UserRepository userRepository;

    // ✅ GET - Tous les produits actifs (sans pagination)
    public List<ProductResponse> getAllProducts() {
        log.info("Récupération de TOUS les produits actifs");
        List<Product> products = productRepository.findByActiveTrue();
        return products.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // ✅ GET - Liste paginée avec filtres (includeInactive pour admin/seller)
    public Page<ProductResponse> getProducts(
            Long categoryId,
            Double minPrice,
            Double maxPrice,
            Long sellerId,
            Boolean promo,
            boolean includeInactive,
            Pageable pageable) {

        log.info("Récupération des produits - includeInactive: {}", includeInactive);

        Specification<Product> spec = (root, query, cb) -> cb.conjunction();

        // ✅ Appliquer le filtre actif UNIQUEMENT si on ne veut pas les inactifs
        if (!includeInactive) {
            spec = spec.and(ProductSpecification.filterByActive(true));
        }

        if (categoryId != null) {
            spec = spec.and(ProductSpecification.filterByCategory(categoryId));
        }
        if (minPrice != null || maxPrice != null) {
            spec = spec.and(ProductSpecification.filterByPriceRange(minPrice, maxPrice));
        }
        if (sellerId != null) {
            spec = spec.and(ProductSpecification.filterBySeller(sellerId));
        }
        if (promo != null) {
            spec = spec.and(ProductSpecification.filterByPromo(promo));
        }

        return productRepository.findAll(spec, pageable)
                .map(this::convertToResponse);
    }

    // Dans ProductService.java - AJOUTER CETTE MÉTHODE
    // Dans ProductService.java - Vérifier/CORRIGER cette méthode
    // ProductService.java - MODIFIER la méthode getSellerProducts
    public Page<ProductResponse> getSellerProducts(
            String email,
            Long categoryId,
            String search,
            boolean includeInactive,
            Double minPrice,
            Double maxPrice,
            Boolean promo,
            Pageable pageable) {

        log.info("Récupération des produits du vendeur: {}, minPrice: {}, maxPrice: {}, promo: {}",
                email, minPrice, maxPrice, promo);

        // 1. Récupérer l'utilisateur
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé: " + email));

        // 2. Récupérer le SellerProfile
        SellerProfile sellerProfile = sellerProfileRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Profil vendeur non trouvé pour: " + email));

        // 3. Construire la spécification
        Specification<Product> spec = (root, query, cb) -> cb.equal(
                root.get("sellerProfile").get("id"),
                sellerProfile.getId()
        );

        // 4. Inclure ou non les produits inactifs
        if (!includeInactive) {
            spec = spec.and(ProductSpecification.filterByActive(true));
        }

        // 5. Filtrer par catégorie
        if (categoryId != null) {
            spec = spec.and(ProductSpecification.filterByCategory(categoryId));
        }

        // 6. Filtrer par prix
        if (minPrice != null || maxPrice != null) {
            spec = spec.and(ProductSpecification.filterByPriceRange(minPrice, maxPrice));
        }

        // 7. Filtrer par promotion
        if (promo != null) {
            if (promo) {
                spec = spec.and((root, query, cb) ->
                        cb.and(
                                cb.isNotNull(root.get("promoPrice")),
                                cb.greaterThan(root.get("promoPrice"), 0)
                        )
                );
            } else {
                spec = spec.and((root, query, cb) ->
                        cb.or(
                                cb.isNull(root.get("promoPrice")),
                                cb.lessThanOrEqualTo(root.get("promoPrice"), 0)
                        )
                );
            }
        }

        // 8. Recherche par mot-clé
        if (search != null && !search.trim().isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.or(
                            cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%"),
                            cb.like(cb.lower(root.get("description")), "%" + search.toLowerCase() + "%")
                    )
            );
        }

        Page<Product> products = productRepository.findAll(spec, pageable);
        log.info("📦 Produits trouvés: {}", products.getTotalElements());

        return products.map(this::convertToResponse);
    }

    // ✅ GET - Détail d'un produit
    public ProductResponse getProductById(Long id) {
        log.info("Récupération du produit avec l'id: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produit non trouvé avec l'id: " + id));
        return convertToResponse(product);
    }

    // ✅ POST - Créer un produit
    @Transactional
    public ProductResponse createProduct(ProductRequest request, Authentication authentication) {
        log.info("Création d'un nouveau produit: {}", request.getName());

        String email = authentication.getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé: " + email));

        SellerProfile sellerProfile = sellerProfileRepository.findByUser(currentUser)
                .orElseGet(() -> {
                    log.info("Création d'un SellerProfile pour: {}", currentUser.getEmail());
                    SellerProfile newProfile = SellerProfile.builder()
                            .user(currentUser)
                            .storeName(currentUser.getRole().name().equals("ADMIN") ?
                                    "ShopFlow Officiel" : "Boutique de " + currentUser.getFirstName())
                            .description("Boutique officielle")
                            .rating(0.0)
                            .build();
                    return sellerProfileRepository.save(newProfile);
                });

        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setPromoPrice(request.getPromoPrice());
        product.setStock(request.getStock());
        product.setImage(request.getImage() != null ? request.getImage() : "https://via.placeholder.com/300x200");
        product.setActive(true);
        product.setSellerProfile(sellerProfile);

        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            List<Category> categories = categoryRepository.findAllById(request.getCategoryIds());
            product.setCategories(categories);
        }

        if (request.getVariants() != null && !request.getVariants().isEmpty()) {
            List<ProductVariant> variants = request.getVariants().stream()
                    .map(v -> createVariant(v, product))
                    .collect(Collectors.toList());
            product.setVariants(variants);
        }

        Product savedProduct = productRepository.save(product);
        log.info("Produit créé avec succès par {} avec l'id: {}", currentUser.getEmail(), savedProduct.getId());

        return convertToResponse(savedProduct);
    }

    // ✅ PUT - Modifier un produit
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request, Authentication authentication) {
        log.info("Mise à jour du produit avec l'id: {}", id);

        String email = authentication.getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé: " + email));

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produit non trouvé avec l'id: " + id));

        // Vérifier les permissions
        if (!currentUser.getRole().name().equals("ADMIN") &&
                !product.getSellerProfile().getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Vous n'avez pas la permission de modifier ce produit");
        }

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setPromoPrice(request.getPromoPrice());
        product.setStock(request.getStock());

        if (request.getImage() != null) {
            product.setImage(request.getImage());
        }

        if (request.getCategoryIds() != null) {
            List<Category> categories = categoryRepository.findAllById(request.getCategoryIds());
            product.setCategories(categories);
        }

        Product updatedProduct = productRepository.save(product);
        log.info("Produit mis à jour avec succès: {}", updatedProduct.getId());

        return convertToResponse(updatedProduct);
    }

    // ✅ DELETE - Désactiver un produit (soft delete)
    @Transactional
    public void deactivateProduct(Long id, Authentication authentication) {
        log.info("Désactivation du produit avec l'id: {}", id);

        String email = authentication.getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé: " + email));

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produit non trouvé avec l'id: " + id));

        if (!currentUser.getRole().name().equals("ADMIN") &&
                !product.getSellerProfile().getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Vous n'avez pas la permission de désactiver ce produit");
        }

        product.setActive(false);
        productRepository.save(product);
        log.info("Produit désactivé avec succès: {}", id);
    }

    // ✅ PATCH - Réactiver un produit
    @Transactional
    public ProductResponse reactivateProduct(Long id, Authentication authentication) {
        log.info("Réactivation du produit avec l'id: {}", id);

        String email = authentication.getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé: " + email));

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produit non trouvé avec l'id: " + id));

        // Vérifier les permissions
        if (!currentUser.getRole().name().equals("ADMIN") &&
                !product.getSellerProfile().getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Vous n'avez pas la permission de réactiver ce produit");
        }

        product.setActive(true);
        Product saved = productRepository.save(product);
        log.info("Produit réactivé avec succès: {}", id);

        return convertToResponse(saved);
    }

    // ✅ GET - Recherche full-text
    public Page<ProductResponse> searchProducts(String keyword, Pageable pageable) {
        log.info("Recherche de produits: {}", keyword);
        Specification<Product> spec = ProductSpecification.filterByActive(true)
                .and(ProductSpecification.searchByKeyword(keyword));
        Page<Product> products = productRepository.findAll(spec, pageable);
        return products.map(this::convertToResponse);
    }

    // ✅ GET - Top 10 meilleures ventes
    public List<ProductResponse> getTopSellingProducts() {
        log.info("Top 10 meilleures ventes");
        List<Product> products = productRepository.findTopSellingProducts(Pageable.ofSize(10));
        return products.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // ✅ Méthodes privées
    private ProductVariant createVariant(ProductVariantRequest request, Product product) {
        ProductVariant variant = new ProductVariant();
        variant.setAttribute(request.getAttribute());
        variant.setValue(request.getValue());
        variant.setExtraStock(request.getExtraStock());
        variant.setPriceDelta(request.getPriceDelta());
        variant.setProduct(product);
        return variant;
    }

    private ProductResponse convertToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .promoPrice(product.getPromoPrice())
                .effectivePrice(product.getEffectivePrice())
                .stock(product.getStock())
                .image(product.getImage())
                .active(product.isActive())
                .sellerName(product.getSellerProfile().getUser().getFirstName() + " " +
                        product.getSellerProfile().getUser().getLastName())
                .sellerId(product.getSellerProfile().getUser().getId())
                .storeName(product.getSellerProfile().getStoreName())
                .categories(product.getCategories().stream()
                        .map(Category::getName)
                        .collect(Collectors.toList()))
                .variants(product.getVariants().stream()
                        .map(this::convertVariantToResponse)
                        .collect(Collectors.toList()))
                .averageRating(product.getAverageRating())
                .reviewCount(product.getReviewCount())
                .promotionPercentage(calculatePromotionPercentage(product))
                .createdAt(product.getCreatedAt())
                .build();
    }

    private ProductVariantResponse convertVariantToResponse(ProductVariant variant) {
        Double finalPrice = variant.getPriceDelta() != null
                ? variant.getProduct().getEffectivePrice() + variant.getPriceDelta()
                : variant.getProduct().getEffectivePrice();

        return ProductVariantResponse.builder()
                .id(variant.getId())
                .attribute(variant.getAttribute())
                .value(variant.getValue())
                .extraStock(variant.getExtraStock())
                .priceDelta(variant.getPriceDelta())
                .finalPrice(finalPrice)
                .build();
    }

    private Integer calculatePromotionPercentage(Product product) {
        if (product.getPromoPrice() != null && product.getPromoPrice() > 0 && product.getPrice() > 0) {
            return (int) ((product.getPrice() - product.getPromoPrice()) / product.getPrice() * 100);
        }
        return 0;
    }
}