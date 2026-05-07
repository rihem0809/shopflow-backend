package com.shopflow.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shopflow.dto.request.CartItemRequest;
import com.shopflow.dto.response.CartResponse;
import com.shopflow.dto.response.CartItemResponse;
import com.shopflow.entity.Cart;
import com.shopflow.entity.CartItem;
import com.shopflow.entity.Coupon;
import com.shopflow.entity.Product;
import com.shopflow.entity.ProductVariant;
import com.shopflow.entity.User;
import com.shopflow.entity.enums.CouponType;
import com.shopflow.repository.CartRepository;
import com.shopflow.repository.CouponRepository;
import com.shopflow.repository.ProductRepository;
import com.shopflow.repository.ProductVariantRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CouponRepository couponRepository;
    private final AuthService authService;

    private static final double DELIVERY_FEES = 5.99;
    private static final double FREE_DELIVERY_THRESHOLD = 100.0;

    public CartResponse getCart() {
        User currentUser = authService.getCurrentUser();
        Cart cart = getOrCreateCart(currentUser);
        return convertToResponse(cart);
    }

    public CartResponse addItem(CartItemRequest request) {
        User currentUser = authService.getCurrentUser();
        Cart cart = getOrCreateCart(currentUser);

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Produit non trouvé"));

        int availableStock = getAvailableStock(product, request.getVariantId());
        if (availableStock < request.getQuantity()) {
            throw new RuntimeException("Stock insuffisant. Disponible: " + availableStock);
        }

        CartItem existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(request.getProductId())
                        && (request.getVariantId() == null ? item.getVariant() == null
                        : item.getVariant() != null && item.getVariant().getId().equals(request.getVariantId())))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            int newQuantity = existingItem.getQuantity() + request.getQuantity();
            if (availableStock < newQuantity) {
                throw new RuntimeException("Stock insuffisant pour cette quantité");
            }
            existingItem.setQuantity(newQuantity);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();

            if (request.getVariantId() != null) {
                ProductVariant variant = productVariantRepository.findById(request.getVariantId())
                        .orElseThrow(() -> new RuntimeException("Variante non trouvée"));
                newItem.setVariant(variant);
            }

            cart.getItems().add(newItem);
        }

        cart.setLastModified(LocalDateTime.now());
        cart.setAppliedCoupon(null);
        Cart savedCart = cartRepository.save(cart);
        return convertToResponse(savedCart);
    }

    public CartResponse updateItemQuantity(Long itemId, Integer quantity) {
        User currentUser = authService.getCurrentUser();
        Cart cart = getOrCreateCart(currentUser);

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Article non trouvé"));

        if (quantity <= 0) {
            cart.getItems().remove(item);
        } else {
            int availableStock = getAvailableStock(item.getProduct(),
                    item.getVariant() != null ? item.getVariant().getId() : null);
            if (availableStock < quantity) {
                throw new RuntimeException("Stock insuffisant. Disponible: " + availableStock);
            }
            item.setQuantity(quantity);
        }

        cart.setLastModified(LocalDateTime.now());
        cart.setAppliedCoupon(null);
        Cart savedCart = cartRepository.save(cart);
        return convertToResponse(savedCart);
    }

    public CartResponse removeItem(Long itemId) {
        User currentUser = authService.getCurrentUser();
        Cart cart = getOrCreateCart(currentUser);

        cart.getItems().removeIf(item -> item.getId().equals(itemId));
        cart.setLastModified(LocalDateTime.now());
        cart.setAppliedCoupon(null);

        Cart savedCart = cartRepository.save(cart);
        return convertToResponse(savedCart);
    }

    public CartResponse clearCart() {
        User currentUser = authService.getCurrentUser();
        Cart cart = getOrCreateCart(currentUser);

        cart.getItems().clear();
        cart.setAppliedCoupon(null);
        cart.setLastModified(LocalDateTime.now());

        Cart savedCart = cartRepository.save(cart);
        return convertToResponse(savedCart);
    }

    public CartResponse applyCoupon(String code) {
        User currentUser = authService.getCurrentUser();
        Cart cart = getOrCreateCart(currentUser);

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Panier vide, impossible d'appliquer un code promo");
        }

        Coupon coupon = couponRepository.findByCodeAndActiveTrue(code.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Code promo invalide ou expiré"));

        if (!coupon.isValid()) {
            throw new RuntimeException("Code promo expiré ou déjà utilisé");
        }

        cart.setAppliedCoupon(coupon);
        cart.setLastModified(LocalDateTime.now());

        Cart savedCart = cartRepository.save(cart);
        return convertToResponse(savedCart);
    }

    public CartResponse removeCoupon() {
        User currentUser = authService.getCurrentUser();
        Cart cart = getOrCreateCart(currentUser);

        cart.setAppliedCoupon(null);
        cart.setLastModified(LocalDateTime.now());

        Cart savedCart = cartRepository.save(cart);
        return convertToResponse(savedCart);
    }

    // ✅ Calcul du stock total (produit + variantes)
    private int getAvailableStock(Product product, Long variantId) {
        if (variantId != null) {
            return productVariantRepository.findById(variantId)
                    .map(ProductVariant::getExtraStock)
                    .orElse(0);
        }
        int baseStock = product.getStock() != null ? product.getStock() : 0;
        int variantsStock = product.getVariants().stream()
                .mapToInt(v -> v.getExtraStock() != null ? v.getExtraStock() : 0)
                .sum();
        return baseStock + variantsStock;
    }

    // ✅ Calcul du prix unitaire (produit + variante)
    private double getUnitPrice(Product product, Long variantId) {
        double basePrice = product.getEffectivePrice() != null ? product.getEffectivePrice() : product.getPrice();
        if (variantId != null) {
            ProductVariant variant = productVariantRepository.findById(variantId).orElse(null);
            if (variant != null && variant.getPriceDelta() != null) {
                return basePrice + variant.getPriceDelta();
            }
        }
        return basePrice;
    }

    private Cart getOrCreateCart(User user) {
        return cartRepository.findByCustomer(user)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .customer(user)
                            .items(new ArrayList<>())
                            .lastModified(LocalDateTime.now())
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    private CartResponse convertToResponse(Cart cart) {
        double subtotal = cart.getItems().stream()
                .mapToDouble(item -> getUnitPrice(item.getProduct(),
                        item.getVariant() != null ? item.getVariant().getId() : null) * item.getQuantity())
                .sum();

        double discountAmount = 0.0;
        String couponCode = null;

        if (cart.getAppliedCoupon() != null) {
            Coupon coupon = cart.getAppliedCoupon();
            couponCode = coupon.getCode();

            if (coupon.getType() == CouponType.PERCENT) {
                discountAmount = subtotal * (coupon.getValeur() / 100);
            } else {
                discountAmount = Math.min(coupon.getValeur(), subtotal);
            }
        }

        double afterDiscount = subtotal - discountAmount;
        double deliveryFees = (afterDiscount >= FREE_DELIVERY_THRESHOLD) ? 0 : DELIVERY_FEES;
        double total = afterDiscount + deliveryFees;
        int itemCount = cart.getItems().stream().mapToInt(CartItem::getQuantity).sum();

        List<CartItemResponse> items = cart.getItems().stream()
                .map(item -> {
                    double unitPrice = getUnitPrice(item.getProduct(),
                            item.getVariant() != null ? item.getVariant().getId() : null);
                    int availableStock = getAvailableStock(item.getProduct(),
                            item.getVariant() != null ? item.getVariant().getId() : null);

                    return CartItemResponse.builder()
                            .id(item.getId())
                            .productId(item.getProduct().getId())
                            .productName(item.getProduct().getName())
                            .productImage(item.getProduct().getImage() != null ?
                                    item.getProduct().getImage() : "https://via.placeholder.com/80x80")
                            .variantId(item.getVariant() != null ? item.getVariant().getId() : null)
                            .variantName(item.getVariant() != null ?
                                    item.getVariant().getAttribute() + ": " + item.getVariant().getValue() : null)
                            .unitPrice(unitPrice)
                            .quantity(item.getQuantity())
                            .totalPrice(unitPrice * item.getQuantity())
                            .availableStock(availableStock)
                            .build();
                })
                .collect(Collectors.toList());

        return CartResponse.builder()
                .id(cart.getId())
                .items(items)
                .subtotal(subtotal)
                .discountAmount(discountAmount)
                .deliveryFees(deliveryFees)
                .total(total)
                .couponCode(couponCode)
                .itemCount(itemCount)
                .lastModified(cart.getLastModified())
                .build();
    }
}