package com.shopflow.service;

import com.shopflow.dto.request.AddressRequest;
import com.shopflow.dto.response.AddressResponse;
import com.shopflow.entity.Address;
import com.shopflow.entity.User;
import com.shopflow.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AddressService {

    private final AddressRepository addressRepository;
    private final AuthService authService;

    public List<AddressResponse> getUserAddresses() {
        User currentUser = authService.getCurrentUser();
        return addressRepository.findByUser(currentUser).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public AddressResponse createAddress(AddressRequest request) {
        User currentUser = authService.getCurrentUser();

        // Si c'est la première adresse ou si elle est marquée comme principale
        boolean isPrincipal = request.isPrincipal();
        if (isPrincipal) {
            // Retirer le statut principal des autres adresses
            addressRepository.findByUser(currentUser).forEach(addr -> {
                addr.setPrincipal(false);
                addressRepository.save(addr);
            });
        }

        Address address = Address.builder()
                .user(currentUser)
                .street(request.getStreet())
                .city(request.getCity())
                .postalCode(request.getPostalCode())
                .country(request.getCountry())
                .principal(isPrincipal)
                .build();

        Address saved = addressRepository.save(address);
        log.info("Adresse créée pour l'utilisateur: {}", currentUser.getEmail());
        return toResponse(saved);
    }

    public AddressResponse updateAddress(Long id, AddressRequest request) {
        User currentUser = authService.getCurrentUser();
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Adresse non trouvée"));

        if (!address.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Vous n'avez pas accès à cette adresse");
        }

        address.setStreet(request.getStreet());
        address.setCity(request.getCity());
        address.setPostalCode(request.getPostalCode());
        address.setCountry(request.getCountry());

        if (request.isPrincipal() && !address.isPrincipal()) {
            // Retirer le statut principal des autres adresses
            addressRepository.findByUser(currentUser).forEach(addr -> {
                addr.setPrincipal(false);
                addressRepository.save(addr);
            });
            address.setPrincipal(true);
        }

        Address saved = addressRepository.save(address);
        return toResponse(saved);
    }

    public void deleteAddress(Long id) {
        User currentUser = authService.getCurrentUser();
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Adresse non trouvée"));

        if (!address.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Vous n'avez pas accès à cette adresse");
        }

        addressRepository.delete(address);
        log.info("Adresse supprimée pour l'utilisateur: {}", currentUser.getEmail());
    }

    public AddressResponse setPrincipalAddress(Long id) {
        User currentUser = authService.getCurrentUser();

        // Retirer le statut principal de toutes les adresses
        addressRepository.findByUser(currentUser).forEach(addr -> {
            addr.setPrincipal(false);
            addressRepository.save(addr);
        });

        // Définir la nouvelle adresse principale
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Adresse non trouvée"));
        address.setPrincipal(true);

        Address saved = addressRepository.save(address);
        return toResponse(saved);
    }

    private AddressResponse toResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .street(address.getStreet())
                .city(address.getCity())
                .postalCode(address.getPostalCode())
                .country(address.getCountry())
                .principal(address.isPrincipal())
                .build();
    }
}