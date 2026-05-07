package com.shopflow.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shopflow.dto.request.CategoryRequest;
import com.shopflow.dto.response.CategoryResponse;
import com.shopflow.entity.Category;
import com.shopflow.repository.CategoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryResponse> getCategoryTree() {
        log.info("Récupération de l'arbre des catégories");
        
        List<Category> rootCategories = categoryRepository.findByParentIsNull();
        
        return rootCategories.stream()
                .map(this::convertToTree)
                .collect(Collectors.toList());
    }

    public CategoryResponse createCategory(CategoryRequest request) {
        log.info("Création d'une nouvelle catégorie: {}", request.getName());
        
        Category category = new Category();
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        
        if (request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("Catégorie parente non trouvée"));
            category.setParent(parent);
        }
        
        Category saved = categoryRepository.save(category);
        return convertToResponse(saved);
    }

    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        log.info("Mise à jour de la catégorie: {}", id);
        
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Catégorie non trouvée"));
        
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        
        if (request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("Catégorie parente non trouvée"));
            category.setParent(parent);
        } else {
            category.setParent(null);
        }
        
        Category updated = categoryRepository.save(category);
        return convertToResponse(updated);
    }

    public void deleteCategory(Long id) {
        log.info("Suppression de la catégorie: {}", id);
        
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Catégorie non trouvée"));
        
        categoryRepository.delete(category);
    }

    private CategoryResponse convertToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .parentName(category.getParent() != null ? category.getParent().getName() : null)
                .subcategories(new ArrayList<>())
                .build();
    }

    private CategoryResponse convertToTree(Category category) {
        CategoryResponse response = convertToResponse(category);
        
        if (category.getSubcategories() != null && !category.getSubcategories().isEmpty()) {
            List<CategoryResponse> children = category.getSubcategories().stream()
                    .map(this::convertToTree)
                    .collect(Collectors.toList());
            response.setSubcategories(children);
        }
        
        return response;
    }
}