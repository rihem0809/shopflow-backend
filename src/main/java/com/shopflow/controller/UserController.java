package com.shopflow.controller;

import com.shopflow.dto.response.UserResponse;
import com.shopflow.dto.request.UserUpdateRequest;
import com.shopflow.entity.User;
import com.shopflow.entity.enums.Role;
import com.shopflow.service.AuthService;
import com.shopflow.service.UserAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Utilisateurs", description = "Gestion des utilisateurs")
public class UserController {

    private final AuthService authService;
    private final UserAdminService userAdminService;

    // ========== PROFIL UTILISATEUR ==========

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Récupérer le profil de l'utilisateur connecté")
    public ResponseEntity<UserResponse> getCurrentUser() {
        User currentUser = authService.getCurrentUser();
        return ResponseEntity.ok(UserResponse.fromEntity(currentUser));
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mettre à jour le profil de l'utilisateur connecté")
    public ResponseEntity<UserResponse> updateCurrentUser(@Valid @RequestBody UserUpdateRequest request) {
        User currentUser = authService.getCurrentUser();
        User updatedUser = userAdminService.updateUser(currentUser.getId(), request);
        return ResponseEntity.ok(UserResponse.fromEntity(updatedUser));
    }

    // ========== ADMIN ==========

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Récupérer tous les utilisateurs")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userAdminService.getAllUsers().stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Récupérer un utilisateur par ID")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        User user = userAdminService.getUserById(id);
        return ResponseEntity.ok(UserResponse.fromEntity(user));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Mettre à jour un utilisateur")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        User updatedUser = userAdminService.updateUser(id, request);
        return ResponseEntity.ok(UserResponse.fromEntity(updatedUser));
    }

    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activer/désactiver un utilisateur")
    public ResponseEntity<UserResponse> toggleUserActive(@PathVariable Long id) {
        User updatedUser = userAdminService.toggleUserActive(id);
        return ResponseEntity.ok(UserResponse.fromEntity(updatedUser));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer un utilisateur")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userAdminService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Statistiques des utilisateurs")
    public ResponseEntity<Map<String, Long>> getUserStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", userAdminService.getTotalUsers());
        stats.put("active", userAdminService.getActiveUsers());
        stats.put("sellers", userAdminService.countByRole(Role.SELLER));  // ✅ Ajouté
        stats.put("customers", userAdminService.countByRole(Role.CUSTOMER)); // ✅ Ajouté
        stats.put("admins", userAdminService.countByRole(Role.ADMIN));      // ✅ Ajouté
        return ResponseEntity.ok(stats);
    }
}