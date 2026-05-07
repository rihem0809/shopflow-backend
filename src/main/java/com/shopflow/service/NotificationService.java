package com.shopflow.service;

import com.shopflow.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    public void notifyOrderCreated(Order order) {
        log.info("📧 NOTIFICATION: Nouvelle commande #{} créée par {}",
                order.getOrderNumber(), order.getCustomer().getEmail());
        // Ici, vous pouvez ajouter l'envoi d'email réel
    }

    public void notifyOrderStatusChanged(Order order) {
        log.info("📧 NOTIFICATION: Commande #{} - Statut changé vers {}",
                order.getOrderNumber(), order.getStatus().getDescription());
        // Ici, vous pouvez ajouter l'envoi d'email réel
    }

    public void notifyOrderCancelled(Order order, double refundAmount) {
        log.info("📧 NOTIFICATION: Commande #{} annulée - Remboursement de {} TND",
                order.getOrderNumber(), refundAmount);
        // Ici, vous pouvez ajouter l'envoi d'email réel
    }
}