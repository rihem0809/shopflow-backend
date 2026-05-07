package com.shopflow.entity;

import com.shopflow.entity.enums.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

// ===== ANNOTATIONS LOMBOK =====
@Data          // Génère getters, setters, toString, equals, hashCode
@Builder       // Permet de créer des objets avec User.builder().email("x").build()
@NoArgsConstructor  // Constructeur sans paramètres
@AllArgsConstructor // Constructeur avec tous les paramètres

// ===== ANNOTATIONS JPA =====
@Entity        // Cette classe est une table dans la base de données
@Table(name = "users")  // Nom de la table dans la BDD
public class User implements UserDetails {  // Implémente UserDetails pour Spring Security

    // ===== ID (CLÉ PRIMAIRE) =====
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // Auto-incrément (MySQL, PostgreSQL)
    private Long id;

    // ===== CHAMPS DE BASE =====
    @Column(unique = true, nullable = false)  // Email unique et obligatoire
    private String email;

    @Column(nullable = false)  // Mot de passe obligatoire
    private String password;

    @Column(name = "first_name")  // Colonne "first_name" dans BDD
    private String firstName;

    @Column(name = "last_name")   // Colonne "last_name" dans BDD
    private String lastName;

    @Enumerated(EnumType.STRING)  // Stocke le rôle comme texte ("CUSTOMER", "ADMIN") et non nombre
    @Column(nullable = false)
    private Role role;            // CUSTOMER, SELLER, ADMIN

    @Column(nullable = false)
    private Boolean active = true;  // Compte actif ou désactivé par admin

    // ===== DATES AUTOMATIQUES =====
    @Column(name = "created_at")
    private LocalDateTime createdAt;  // Date d'inscription

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;  // Date de dernière modification

    // ===== INFORMATIONS DE CONTACT =====
    @Column(name = "phone")
    private String phone;

    @Column(name = "address")
    private String address;

    @Column(name = "city")
    private String city;

    @Column(name = "region")
    private String region;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(name = "country")
    private String country;

    // ===== RELATION AVEC SELLER_PROFILE =====
    // Un utilisateur (User) a UN profil vendeur (SellerProfile)
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    // mappedBy = "user" : la clé étrangère est dans SellerProfile (côté propriétaire)
    // cascade = CascadeType.ALL : si on supprime User, on supprime aussi SellerProfile
    // fetch = FetchType.LAZY : charge SellerProfile seulement quand on y accède
    private SellerProfile sellerProfile;

    // ===== MÉTHODES EXÉCUTÉES AUTOMATIQUEMENT PAR JPA =====
    @PrePersist  // Avant l'insertion en BDD
    protected void onCreate() {
        createdAt = LocalDateTime.now();  // Date d'inscription = maintenant
        updatedAt = LocalDateTime.now();   // Date de mise à jour = maintenant
    }

    @PreUpdate   // Avant chaque mise à jour en BDD
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();   // Met à jour la date
    }

    // Getter manuel pour sellerProfile (Lombok @Data le génère normalement)
    public SellerProfile getSellerProfile() {
        return sellerProfile;
    }

    // ===== MÉTHODES DE SPRING SECURITY (interface UserDetails) =====

    // Retourne les rôles de l'utilisateur (ex: "ROLE_ADMIN")
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Transforme "ADMIN" → "ROLE_ADMIN" (format attendu par Spring Security)
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    // Spring Security utilise cette méthode pour l'identifiant
    @Override
    public String getUsername() {
        return email;  // On utilise l'email comme nom d'utilisateur
    }

    // Le compte n'expire jamais
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    // Le compte n'est jamais bloqué (on utilise le champ "active" pour ça)
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    // Les identifiants (mot de passe) n'expirent jamais
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    // Compte activé ou désactivé (Spring Security utilise cette méthode)
    @Override
    public boolean isEnabled() {
        return active;  // active = true → compte actif, false → compte désactivé
    }
}