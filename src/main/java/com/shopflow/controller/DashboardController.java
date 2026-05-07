package com.shopflow.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shopflow.dto.response.AdminDashboardResponse;
import com.shopflow.dto.response.SellerDashboardResponse;
import com.shopflow.service.DashboardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Dashboard", description = "Tableaux de bord ADMIN et SELLER")
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Dashboard ADMIN - Chiffre d'affaires, top produits, top vendeurs, commandes récentes")
    public ResponseEntity<AdminDashboardResponse> getAdminDashboard() {
        log.info("GET /api/dashboard/admin - Récupération du dashboard ADMIN");
        AdminDashboardResponse response = dashboardService.getAdminDashboard();
        log.info("Dashboard ADMIN généré avec succès");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/seller")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Dashboard SELLER - Stats du vendeur connecté")
    public ResponseEntity<SellerDashboardResponse> getSellerDashboard() {
        log.info("GET /api/dashboard/seller - Récupération du dashboard SELLER");
        return ResponseEntity.ok(dashboardService.getSellerDashboard());
    }
}