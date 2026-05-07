package com.shopflow.config;

import com.shopflow.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service  // 📌 Dit à Spring : "ceci est un service, crée-moi un objet"
public class JwtService {

    // 📌 Les valeurs sont lues depuis application.properties
    @Value("${jwt.secret}")
    private String secretKey;        // 🔐 La clé secrète pour signer les tokens

    @Value("${jwt.expiration}")
    private long jwtExpiration;      // ⏰ Durée de vie du token principal (ex: 1h)

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;  // 🔄 Durée de vie du refresh token (ex: 24h)

    // 🔐 Transforme la clé texte en clé cryptographique
    private Key getSigningKey() {
        byte[] keyBytes = secretKey.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // 📧 Extraire l'email depuis le token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);  // "subject" = email
    }

    // 👑 Extraire le rôle depuis le token
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    // ⏰ Temps restant avant expiration (en millisecondes)
    public Long extractExpirationTime(String token) {
        Date expiration = extractExpiration(token);
        return expiration.getTime() - System.currentTimeMillis();
    }

    // 🔧 Méthode générique pour extraire N'IMPORTE QUELLE info du token
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // 📖 Lit TOUTES les données du token (payload complet)
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())    // Vérifie la signature
                .build()
                .parseClaimsJws(token)             // Parse le token
                .getBody();                        // Retourne les données
    }

    // ========== 🎫 GÉNÉRATION DES TOKENS ==========

    // 📌 Génère un token à partir de l'objet Spring Security (UserDetails)
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        // Récupère le rôle (ex: "ROLE_ADMIN")
        String role = userDetails.getAuthorities().iterator().next().getAuthority();
        if (role != null && role.startsWith("ROLE_")) {
            role = role.substring(5);  // Enlève "ROLE_" → garde juste "ADMIN"
        }
        claims.put("role", role);
        claims.put("email", userDetails.getUsername());
        return generateToken(claims, userDetails);
    }

    // 📌 Génère un token à partir de ton entité User (@Entity) - Version principale
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().name());  // Ajoute le rôle
        claims.put("email", user.getEmail());       // Ajoute l'email
        claims.put("userId", user.getId());         // Ajoute l'ID (pratique !)

        return Jwts.builder()
                .setClaims(claims)                   // Toutes les données perso
                .setSubject(user.getEmail())         // Sujet = email
                .setIssuedAt(new Date())             // Date de création
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration)) // Date d'expiration
                .signWith(getSigningKey(), SignatureAlgorithm.HS256) // Signature
                .compact();                          // Génère la chaîne finale
    }

    // 🔧 Méthode privée qui fait le vrai travail de génération
    private String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // 🔄 Génère un refresh token (dure plus longtemps) depuis UserDetails
    public String generateRefreshToken(UserDetails userDetails) {
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // 🔄 Génère un refresh token depuis ton entité User
    public String generateRefreshToken(User user) {
        return Jwts.builder()
                .setSubject(user.getEmail())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // ========== ✅ VALIDATION DES TOKENS ==========

    // ✅ Vérifie si le token est valide (non expiré + email correspond)
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    // ✅ Version avec ton entité User
    public boolean isTokenValid(String token, User user) {
        final String username = extractUsername(token);
        return (username.equals(user.getEmail())) && !isTokenExpired(token);
    }

    // ⏰ Vérifie si le token a expiré
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());  // Date expiration < maintenant ?
    }

    // 📅 Extrait la date d'expiration du token
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
}