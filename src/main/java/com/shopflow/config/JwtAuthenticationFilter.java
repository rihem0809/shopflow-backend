// JwtAuthenticationFilter.java - VERSION CORRIGÉE
package com.shopflow.config;

import com.shopflow.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String path = request.getServletPath();
        final String method = request.getMethod();

        log.debug("🔍 Requête: {} {}", method, path);

        // ========== URLs TOTALEMENT PUBLIQUES (sans token) ==========
        if (path.startsWith("/api/auth") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/api-docs") ||
                path.startsWith("/h2-console")) {
            log.debug("✅ URL publique (auth/swagger): {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        // ========== GET PUBLICS (sans token) - MAIS EXCLURE les endpoints protégés ==========
        boolean isPublicGet = "GET".equals(method) &&
                (path.startsWith("/api/products") || path.startsWith("/api/categories"));

        // ✅ EXCLURE les endpoints qui nécessitent une authentification même en GET
        boolean isProtectedGet = path.startsWith("/api/products/seller/") ||
                path.equals("/api/orders/my") ||
                path.startsWith("/api/orders/my-seller-orders") ||
                path.startsWith("/api/users/me");

        if (isPublicGet && !isProtectedGet) {
            log.debug("✅ GET public: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        // ========== TOUTES LES AUTRES REQUÊTES NÉCESSITENT UN TOKEN ==========
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.error("❌ Token manquant pour: {} {}", method, path);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Missing or invalid Authorization header\"}");
            return;
        }

        final String jwt = authHeader.substring(7);

        try {
            final String userEmail = jwtService.extractUsername(jwt);
            log.debug("📧 Email extrait du token: {}", userEmail);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.info("✅ Utilisateur authentifié: {} avec rôles: {}",
                            userEmail, userDetails.getAuthorities());
                } else {
                    log.warn("❌ Token invalide pour: {}", userEmail);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Invalid token\"}");
                    return;
                }
            }
        } catch (Exception e) {
            log.error("❌ Erreur JWT: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Invalid token: " + e.getMessage() + "\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}