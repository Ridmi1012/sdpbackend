package com.example.sdpbackend.service;

import com.example.sdpbackend.dto.ItemDTO;
import com.example.sdpbackend.entity.Item;
import com.example.sdpbackend.repository.ItemRepository;
import jakarta.persistence.EntityNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ItemService {
        private final ItemRepository itemRepository;

        @Autowired
        public ItemService(ItemRepository itemRepository) {
            this.itemRepository = itemRepository;
        }

        @Transactional(readOnly = true)
        public List<ItemDTO.ItemResponse> getAllItems() {
            return itemRepository.findAll().stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        public ItemDTO.ItemResponse getItemById(Integer id) {
            Item item = itemRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Item not found with id: " + id));
            return mapToResponse(item);
        }

        @Transactional(readOnly = true)
        public List<ItemDTO.ItemResponse> searchItems(String query) {
            if (query == null || query.isEmpty()) {
                return getAllItems();
            }
            return itemRepository.findByNameContainingIgnoreCase(query).stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }

        @Transactional
        public ItemDTO.ItemResponse createItem(ItemDTO.ItemRequest request) {
            Item item = new Item();
            item.setName(request.getName());
            item.setDescription(request.getDescription());

            // CRITICAL FIX: Ensure unitPrice is never null
            if (request.getUnitPrice() == null) {
                item.setUnitPrice(new BigDecimal("0.00"));
            } else {
                item.setUnitPrice(request.getUnitPrice());
            }

            // Set timestamp fields
            LocalDateTime now = LocalDateTime.now();
            item.setCreatedAt(now);
            item.setUpdatedAt(now);

            Item savedItem = itemRepository.save(item);
            return mapToResponse(savedItem);
        }

        @Transactional
        public ItemDTO.ItemResponse updateItem(Integer id, ItemDTO.ItemRequest request) {
            Item item = itemRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Item not found with id: " + id));

            item.setName(request.getName());
            item.setDescription(request.getDescription());

            // CRITICAL FIX: Ensure unitPrice is never null
            if (request.getUnitPrice() == null) {
                item.setUnitPrice(new BigDecimal("0.00"));
            } else {
                item.setUnitPrice(request.getUnitPrice());
            }

            // Update timestamp
            item.setUpdatedAt(LocalDateTime.now());

            Item updatedItem = itemRepository.save(item);
            return mapToResponse(updatedItem);
        }

        @Transactional
        public void deleteItem(Integer id) {
            if (!itemRepository.existsById(id)) {
                throw new EntityNotFoundException("Item not found with id: " + id);
            }
            itemRepository.deleteById(id);
        }

        private ItemDTO.ItemResponse mapToResponse(Item item) {
            return new ItemDTO.ItemResponse(
                    item.getItemID(),
                    item.getName(),
                    item.getDescription(),
                    item.getUnitPrice()
            );
        }
    }

