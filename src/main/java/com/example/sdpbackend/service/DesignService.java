package com.example.sdpbackend.service;

import com.example.sdpbackend.dto.CategoryDTO;
import com.example.sdpbackend.dto.DesignDTO;
import com.example.sdpbackend.dto.DesignItemDTO;
import com.example.sdpbackend.dto.ItemDTO;
import com.example.sdpbackend.entity.Category;
import com.example.sdpbackend.entity.Design;
import com.example.sdpbackend.entity.DesignItem;
import com.example.sdpbackend.entity.Item;
import com.example.sdpbackend.repository.CategoryRepository;
import com.example.sdpbackend.repository.DesignItemRepository;
import com.example.sdpbackend.repository.DesignRepository;
import com.example.sdpbackend.repository.ItemRepository;


import jakarta.persistence.EntityNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DesignService {
    private final DesignRepository designRepository;
    private final DesignItemRepository designItemRepository;
    private final CategoryRepository categoryRepository;
    private final ItemRepository itemRepository;
    private final CloudinaryService cloudinaryService;

    @Autowired
    public DesignService(DesignRepository designRepository,
                         DesignItemRepository designItemRepository,
                         CategoryRepository categoryRepository,
                         ItemRepository itemRepository,
                         CloudinaryService cloudinaryService) {
        this.designRepository = designRepository;
        this.designItemRepository = designItemRepository;
        this.categoryRepository = categoryRepository;
        this.itemRepository = itemRepository;
        this.cloudinaryService = cloudinaryService;
    }

    public List<DesignDTO.DesignResponse> getAllDesigns() {
        List<Design> designs = designRepository.findAll();
        return designs.stream()
                .map(this::mapToDesignResponse)
                .collect(Collectors.toList());
    }

    public DesignDTO.DesignResponse getDesignById(Integer id) {
        Design design = designRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Design not found with id: " + id));
        return mapToDesignResponse(design);
    }

    public List<DesignDTO.DesignResponse> getDesignsByCategory(Integer categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + categoryId));
        List<Design> designs = designRepository.findByCategory(category);
        return designs.stream()
                .map(this::mapToDesignResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public DesignDTO.DesignResponse createDesign(DesignDTO.DesignRequest request, MultipartFile image) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + request.getCategoryId()));

        Design design = new Design();
        design.setName(request.getName());
        design.setCategory(category);
        design.setBasePrice(request.getBasePrice());
        design.setDescription(request.getDescription());
        design.setCreatedBy(request.getCreatedBy());

        // Handle image upload
        if (image != null && !image.isEmpty()) {
            // Upload to Cloudinary and get the URL
            String imageUrl = cloudinaryService.uploadImage(image);
            design.setImageUrl(imageUrl);
        } else if (request.getImageUrl() != null && !request.getImageUrl().isEmpty()) {
            // If image URL is provided directly (e.g., already uploaded via frontend)
            design.setImageUrl(request.getImageUrl());
        }

        Design savedDesign = designRepository.save(design);

        // Create design items
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (DesignItemDTO.DesignItemRequest itemRequest : request.getItems()) {
                Item item = itemRepository.findById(itemRequest.getItemId())
                        .orElseThrow(() -> new EntityNotFoundException("Item not found with id: " + itemRequest.getItemId()));

                DesignItem designItem = new DesignItem();
                designItem.setDesign(savedDesign);
                designItem.setItem(item);
                designItem.setDefaultQuantity(itemRequest.getDefaultQuantity());

                designItemRepository.save(designItem);
            }
        }

        return mapToDesignResponse(savedDesign);
    }

    @Transactional
    public DesignDTO.DesignResponse updateDesign(Integer id, DesignDTO.DesignRequest request, MultipartFile image) {
        Design design = designRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Design not found with id: " + id));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + request.getCategoryId()));

        design.setName(request.getName());
        design.setCategory(category);
        design.setBasePrice(request.getBasePrice());
        design.setDescription(request.getDescription());

        // Handle image update
        if (image != null && !image.isEmpty()) {
            // If there's an existing image, delete it from Cloudinary
            if (design.getImageUrl() != null && !design.getImageUrl().isEmpty()) {
                cloudinaryService.deleteImage(design.getImageUrl());
            }

            // Upload the new image to Cloudinary
            String imageUrl = cloudinaryService.uploadImage(image);
            design.setImageUrl(imageUrl);
        } else if (request.getImageUrl() != null && !request.getImageUrl().isEmpty()
                && !request.getImageUrl().equals(design.getImageUrl())) {
            // If a new URL is provided and it's different from the current one
            // Delete the old image if it exists
            if (design.getImageUrl() != null && !design.getImageUrl().isEmpty()) {
                cloudinaryService.deleteImage(design.getImageUrl());
            }
            design.setImageUrl(request.getImageUrl());
        }
        // If no new image or URL is provided, keep the existing one

        Design updatedDesign = designRepository.save(design);

        // Update design items
        // First, remove existing items
        List<DesignItem> existingItems = designItemRepository.findByDesign(design);
        designItemRepository.deleteAll(existingItems);

        // Then add new items
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (DesignItemDTO.DesignItemRequest itemRequest : request.getItems()) {
                Item item = itemRepository.findById(itemRequest.getItemId())
                        .orElseThrow(() -> new EntityNotFoundException("Item not found with id: " + itemRequest.getItemId()));

                DesignItem designItem = new DesignItem();
                designItem.setDesign(updatedDesign);
                designItem.setItem(item);
                designItem.setDefaultQuantity(itemRequest.getDefaultQuantity());

                designItemRepository.save(designItem);
            }
        }

        return mapToDesignResponse(updatedDesign);
    }

    @Transactional
    public void deleteDesign(Integer id) {
        Design design = designRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Design not found with id: " + id));

        // Delete the image from Cloudinary if it exists
        if (design.getImageUrl() != null && !design.getImageUrl().isEmpty()) {
            cloudinaryService.deleteImage(design.getImageUrl());
        }

        // Delete associated design items first
        designItemRepository.deleteByDesign(design);

        // Then delete the design
        designRepository.delete(design);
    }

    // Helper methods for DTO conversions
    private DesignDTO.DesignResponse mapToDesignResponse(Design design) {
        List<DesignItem> designItems = designItemRepository.findByDesign(design);
        List<DesignItemDTO.DesignItemResponse> itemResponses = designItems.stream()
                .map(this::mapToDesignItemResponse)
                .collect(Collectors.toList());

        CategoryDTO.CategoryResponse categoryResponse = new CategoryDTO.CategoryResponse(
                design.getCategory().getCategoryID(),
                design.getCategory().getName(),
                design.getCategory().getDescription()
        );

        return new DesignDTO.DesignResponse(
                design.getDesignID(),
                design.getName(),
                categoryResponse,
                design.getBasePrice(),
                design.getDescription(),
                design.getImageUrl(),
                itemResponses
        );
    }

    private DesignItemDTO.DesignItemResponse mapToDesignItemResponse(DesignItem designItem) {
        ItemDTO.ItemResponse itemResponse = new ItemDTO.ItemResponse(
                designItem.getItem().getItemID(),
                designItem.getItem().getName(),
                designItem.getItem().getDescription(),
                designItem.getItem().getUnitPrice()
        );

        return new DesignItemDTO.DesignItemResponse(
                designItem.getDesignItemID(),
                itemResponse,
                designItem.getDefaultQuantity()
        );
    }
}
