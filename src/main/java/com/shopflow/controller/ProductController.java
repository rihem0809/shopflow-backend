package com.shopflow.controller;

import com.shopflow.dto.request.ProductRequest;
import com.shopflow.dto.response.ProductResponse;
import com.shopflow.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController                    // Indique que c'est un contrôleur REST (retourne du JSON)
@RequestMapping("/api/products")   // URL de base pour tous les endpoints
@RequiredArgsConstructor          // Génère un constructeur avec les final fields (injection)
@Slf4j                           // Fournit un logger (log.info, etc.)
@Tag(name = "02 - Produits", description = "Endpoints pour la gestion des produits")
public class ProductController {

    private final ProductService productService;

    // ==================== ENDPOINTS PUBLICS ====================

    @GetMapping("/all")
    @Operation(summary = "Tous les produits actifs")
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        log.info("GET /api/products/all");
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping
    @Operation(summary = "Liste paginée des produits")
    public ResponseEntity<Page<ProductResponse>> getProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Long sellerId,
            @RequestParam(required = false) Boolean promo,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            Authentication authentication) {

        log.info("GET /api/products - page: {}, size: {}, includeInactive: {}", page, size, includeInactive);

        // Validation du tri
        String validSortBy = validateSortBy(sortBy);
        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc")
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, validSortBy));

        // Recherche full-text
        if (search != null && !search.trim().isEmpty()) {
            return ResponseEntity.ok(productService.searchProducts(search, pageable));
        }

        // Vérifier si l'utilisateur peut voir les produits inactifs
        boolean canViewInactive = false;
        if (authentication != null) {
            String role = authentication.getAuthorities().iterator().next().getAuthority();
            canViewInactive = role.equals("ROLE_ADMIN") || role.equals("ROLE_SELLER");
        }

        boolean showInactive = canViewInactive && includeInactive;

        Page<ProductResponse> result = productService.getProducts(
                categoryId, minPrice, maxPrice, sellerId, promo, showInactive, pageable);

        return ResponseEntity.ok(result);
    }

    // ProductController.java - AJOUTER les paramètres minPrice, maxPrice, promo
    @GetMapping("/seller/my-products")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Récupérer les produits du vendeur connecté")
    public ResponseEntity<Page<ProductResponse>> getMyProducts(
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "true") boolean includeInactive,
            // ✅ AJOUTER CES PARAMÈTRES
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Boolean promo,
            Authentication authentication) {

        log.info("GET /api/products/seller/my-products - email: {}, minPrice: {}, maxPrice: {}, promo: {}",
                authentication.getName(), minPrice, maxPrice, promo);

        String email = authentication.getName();

        String validSortBy = validateSortBy(sortBy);
        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc")
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, validSortBy));

        Page<ProductResponse> result = productService.getSellerProducts(
                email, categoryId, search, includeInactive, minPrice, maxPrice, promo, pageable);

        return ResponseEntity.ok(result);
    }


    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un produit")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        log.info("GET /api/products/{}", id);
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping("/search")
    @Operation(summary = "Recherche full-text")
    public ResponseEntity<Page<ProductResponse>> searchProducts(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        log.info("GET /api/products/search - q: {}", q);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(productService.searchProducts(q, pageable));
    }

    @GetMapping("/top-selling")
    @Operation(summary = "Top 10 meilleures ventes")
    public ResponseEntity<List<ProductResponse>> getTopSellingProducts() {
        log.info("GET /api/products/top-selling");
        return ResponseEntity.ok(productService.getTopSellingProducts());
    }

    // ==================== ENDPOINTS PROTÉGÉS ====================

    @PostMapping
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductRequest request,
            Authentication authentication) {

        log.info("POST /api/products - Création: {}", request.getName());
        ProductResponse created = productService.createProduct(request, authentication);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request,
            Authentication authentication) {

        log.info("PUT /api/products/{}", id);
        ProductResponse updated = productService.updateProduct(id, request, authentication);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Void> deactivateProduct(
            @PathVariable Long id,
            Authentication authentication) {

        log.info("DELETE /api/products/{} - Désactivation", id);
        productService.deactivateProduct(id, authentication);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reactivate")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Réactiver un produit désactivé")
    public ResponseEntity<ProductResponse> reactivateProduct(
            @PathVariable Long id,
            Authentication authentication) {

        log.info("PATCH /api/products/{}/reactivate", id);
        ProductResponse updated = productService.reactivateProduct(id, authentication);
        return ResponseEntity.ok(updated);
    }

    // Helper pour valider les champs de tri
    private String validateSortBy(String sortBy) {
        List<String> validFields = List.of("createdAt", "price", "name", "averageRating");
        if (validFields.contains(sortBy)) {
            return sortBy;
        }
        return "createdAt";
    }
}