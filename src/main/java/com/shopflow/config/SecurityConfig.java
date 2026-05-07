// SecurityConfig.java - Version complète et corrigée
package com.shopflow.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.shopflow.service.CustomUserDetailsService;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final CustomUserDetailsService customUserDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Désactiver CSRF (stateless API)
                .csrf(AbstractHttpConfigurer::disable)

                // Configuration CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Désactiver form login et HTTP Basic
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                // Session stateless (pas de session)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Ajouter le filtre JWT avant le filtre d'authentification standard
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

                // Permettre H2 console (dev only)
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()))

                // Configuration des autorisations
                .authorizeHttpRequests(auth -> auth
                        // ========== 1. URLs TOTALEMENT PUBLIQUES ==========
                        // Authentification
                        .requestMatchers("/api/auth/**").permitAll()

                        // Swagger / API Docs
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/swagger-ui.html").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .requestMatchers("/api-docs/**").permitAll()

                        // H2 Console (développement)
                        .requestMatchers("/h2-console/**").permitAll()

                        // Favicon
                        .requestMatchers("/favicon.ico").permitAll()

                        // ========== 2. GET PUBLICS (consultation) ==========
                        // Produits - consultation publique
                        .requestMatchers(HttpMethod.GET, "/api/products/all").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products/search").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products/top-selling").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products/{id}").permitAll()

                        // Catégories - consultation publique
                        .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()

                        // Coupons - validation publique
                        .requestMatchers(HttpMethod.GET, "/api/coupons/validate/**").permitAll()

                        // Avis - consultation publique
                        .requestMatchers(HttpMethod.GET, "/api/reviews/product/**").permitAll()

                        // ========== 3. ENDPOINTS SPÉCIFIQUES (ordre important: plus spécifiques d'abord) ==========

                        // ---------- PRODUITS (création/modification/suppression) ----------
                        .requestMatchers(HttpMethod.POST, "/api/products").hasAnyRole("ADMIN", "SELLER")
                        .requestMatchers(HttpMethod.PUT, "/api/products/**").hasAnyRole("ADMIN", "SELLER")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasAnyRole("ADMIN", "SELLER")
                        .requestMatchers(HttpMethod.PATCH, "/api/products/**").hasAnyRole("ADMIN", "SELLER")

                        // Produits du vendeur connecté
                        .requestMatchers(HttpMethod.GET, "/api/products/seller/my-products").hasAnyRole("SELLER", "ADMIN")

                        // ---------- COMMANDES ----------
                        // Admin: voir toutes les commandes
                        .requestMatchers(HttpMethod.GET, "/api/orders").hasRole("ADMIN")

                        // Vendeur: voir commandes de ses produits
                        .requestMatchers(HttpMethod.GET, "/api/orders/my-seller-orders").hasRole("SELLER")

                        // Client: voir ses propres commandes
                        .requestMatchers(HttpMethod.GET, "/api/orders/my").hasRole("CUSTOMER")

                        // Client: annuler une commande
                        .requestMatchers(HttpMethod.PUT, "/api/orders/{id}/cancel").hasRole("CUSTOMER")

                        // Admin/Vendeur: modifier le statut d'une commande
                        .requestMatchers(HttpMethod.PUT, "/api/orders/{id}/status").hasAnyRole("ADMIN", "SELLER")

                        // Créer une commande
                        .requestMatchers(HttpMethod.POST, "/api/orders").hasAnyRole("CUSTOMER", "ADMIN")

                        // Voir détail commande par ID (Admin, Vendeur, Client selon ownership)
                        .requestMatchers(HttpMethod.GET, "/api/orders/{id}").hasAnyRole("ADMIN", "SELLER", "CUSTOMER")

                        // ---------- CATÉGORIES (administration) ----------
                        .requestMatchers(HttpMethod.POST, "/api/categories").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/categories/**").hasRole("ADMIN")

                        // ---------- COUPONS (administration) ----------
                        .requestMatchers(HttpMethod.POST, "/api/coupons").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/coupons/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/coupons/**").hasRole("ADMIN")

                        // ---------- UTILISATEURS ----------
                        // Utilisateur connecté
                        .requestMatchers(HttpMethod.GET, "/api/users/me").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/users/me").authenticated()

                        // Administration des utilisateurs
                        .requestMatchers(HttpMethod.GET, "/api/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/users/stats").hasRole("ADMIN")

                        // ---------- AVIS ----------
                        // Client: ajouter un avis
                        .requestMatchers(HttpMethod.POST, "/api/reviews").hasRole("CUSTOMER")

                        // Admin: modération des avis
                        .requestMatchers(HttpMethod.PUT, "/api/reviews/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/reviews/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/reviews/pending").hasRole("ADMIN")

                        // ---------- DASHBOARD ----------
                        .requestMatchers(HttpMethod.GET, "/api/dashboard/admin").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/dashboard/seller").hasRole("SELLER")

                        // ---------- PANIER ----------
                        .requestMatchers("/api/cart/**").hasAnyRole("CUSTOMER", "ADMIN")

                        // ---------- ADRESSES ----------
                        .requestMatchers("/api/addresses/**").hasAnyRole("CUSTOMER", "ADMIN")

                        // ========== 4. TOUTES LES AUTRES REQUÊTES ==========
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Origines autorisées
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:4200",   // Angular dev
                "http://localhost:8080",   // Backend
                "http://localhost:3000"    // Alternative
        ));

        // Méthodes HTTP autorisées
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"
        ));

        // Headers autorisés
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));

        // Headers exposés au client
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Disposition"
        ));

        // Autoriser les credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);

        // Durée de cache CORS (1 heure)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}