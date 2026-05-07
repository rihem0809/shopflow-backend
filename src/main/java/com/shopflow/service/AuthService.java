package com.shopflow.service;

import com.shopflow.dto.request.LoginRequest;
import com.shopflow.config.JwtService;
import com.shopflow.dto.request.RegisterRequest;
import com.shopflow.dto.response.AuthResponse;
import com.shopflow.entity.RefreshToken;
import com.shopflow.entity.SellerProfile;
import com.shopflow.entity.enums.Role;
import com.shopflow.entity.User;
import com.shopflow.repository.RefreshTokenRepository;
import com.shopflow.repository.SellerProfileRepository;
import com.shopflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenService refreshTokenService;
    private final SellerProfileRepository sellerProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    // ==================== INSCRIPTION ====================
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Tentative d'inscription: {} avec rôle: {}", request.getEmail(), request.getRole());

        // 1. Vérifier si l'email existe déjà
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email déjà utilisé");
        }

        // 2. Déterminer le rôle à attribuer
        Role role = Role.CUSTOMER; // Par défaut
        if (request.getRole() != null) {
            if (request.getRole().equalsIgnoreCase("SELLER")) {
                role = Role.SELLER;
            } else if (request.getRole().equalsIgnoreCase("ADMIN")) {
                role = Role.ADMIN;
            }
        }

        // 3. Créer l'utilisateur
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(role)
                .active(true)
                .build();

        user = userRepository.save(user);
        log.info("Utilisateur créé: {} avec rôle: {}", user.getEmail(), user.getRole());

        // 4. Si c'est un vendeur, créer son profil SellerProfile
        if (role == Role.SELLER) {
            SellerProfile sellerProfile = SellerProfile.builder()
                    .user(user)
                    .storeName(request.getStoreName() != null ? request.getStoreName() : "Boutique de " + user.getFirstName())
                    .description(request.getStoreDescription() != null ? request.getStoreDescription() : "")
                    .logo(request.getStoreLogo() != null ? request.getStoreLogo() : "https://via.placeholder.com/150")
                    .rating(0.0)
                    .build();
            sellerProfileRepository.save(sellerProfile);
            user.setSellerProfile(sellerProfile);
            log.info("Profil vendeur créé pour: {}", user.getEmail());
        }

        // 5. Générer les tokens JWT
        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // 6. Stocker le refresh token en base
        refreshTokenService.createRefreshToken(user, refreshToken);

        // 7. Retourner la réponse
        return buildAuthResponse(user, accessToken, refreshToken);
    }

    // ==================== CONNEXION ====================
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Tentative de connexion: {}", request.getEmail());

        // 1. Vérifier email + mot de passe
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 2. Récupérer l'utilisateur
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // 3. Vérifier si le compte est actif
        if (!user.getActive()) {
            throw new RuntimeException("Compte désactivé");
        }

        // 4. Générer les tokens
        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // 5. Sauvegarder le refresh token
        refreshTokenService.createRefreshToken(user, refreshToken);

        // 6. Retourner la réponse
        return buildAuthResponse(user, accessToken, refreshToken);
    }

    // ==================== RAFRAÎCHIR LE TOKEN ====================
    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        log.info("Rafraîchissement du token");

        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new RuntimeException("Refresh token manquant");
        }

        RefreshToken storedToken = refreshTokenService.findByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh token invalide"));

        if (storedToken.isRevoked()) {
            throw new RuntimeException("Refresh token révoqué");
        }

        if (storedToken.isExpired()) {
            refreshTokenService.deleteByUser(storedToken.getUser());
            throw new RuntimeException("Refresh token expiré");
        }

        User user = storedToken.getUser();

        if (!user.getActive()) {
            throw new RuntimeException("Compte désactivé");
        }

        String newAccessToken = jwtService.generateToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        refreshTokenService.createRefreshToken(user, newRefreshToken);

        return buildAuthResponse(user, newAccessToken, newRefreshToken);
    }

    // ==================== DÉCONNEXION ====================
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isEmpty()) {
            refreshTokenService.revokeToken(refreshToken);
            log.info("Déconnexion réussie");
        }
        SecurityContextHolder.clearContext();
    }

    // ==================== UTILISATEUR CONNECTÉ ====================
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Utilisateur non authentifié");
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }

    // ==================== CONSTRUCTION DE LA RÉPONSE ====================
    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        AuthResponse response = AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(86400L)
                .email(user.getEmail())
                .role(user.getRole().name())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .active(user.getActive())
                .build();

        // Ajouter le profil vendeur si nécessaire
        if (user.getRole() == Role.SELLER && user.getSellerProfile() != null) {
            response.setSellerProfile(AuthResponse.SellerProfileDto.builder()
                    .id(user.getSellerProfile().getId())
                    .storeName(user.getSellerProfile().getStoreName())
                    .description(user.getSellerProfile().getDescription())
                    .rating(user.getSellerProfile().getRating())
                    .logo(user.getSellerProfile().getLogo())
                    .build());
        }

        return response;
    }
}