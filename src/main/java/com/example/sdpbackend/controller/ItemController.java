package com.example.sdpbackend.controller;


import com.example.sdpbackend.dto.ItemDTO;
import com.example.sdpbackend.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/items")
@CrossOrigin
public class ItemController {
    private final ItemService itemService;

    @Autowired
    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @GetMapping
    public ResponseEntity<List<ItemDTO.ItemResponse>> getAllItems() {
        return ResponseEntity.ok(itemService.getAllItems());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItemDTO.ItemResponse> getItemById(@PathVariable Integer id) {
        return ResponseEntity.ok(itemService.getItemById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ItemDTO.ItemResponse>> searchItems(@RequestParam(required = false) String query) {
        return ResponseEntity.ok(itemService.searchItems(query));
    }

    @PostMapping
    public ResponseEntity<?> createItem(@RequestBody ItemDTO.ItemRequest request) {
        try {
            System.out.println("Received request: " + request);

            // Defensive programming
            if (request == null) {
                return ResponseEntity.badRequest().body("Request body cannot be null");
            }

            if (request.getName() == null || request.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Item name is required");
            }

            if (request.getUnitPrice() == null) {
                System.out.println("Unit price is null, setting default");
                request.setUnitPrice(BigDecimal.ZERO);
            } else {
                System.out.println("Unit price value: " + request.getUnitPrice());
            }

            ItemDTO.ItemResponse result = itemService.createItem(request);
            return new ResponseEntity<>(result, HttpStatus.CREATED);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating item: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ItemDTO.ItemResponse> updateItem(
            @PathVariable Integer id,
            @RequestBody ItemDTO.ItemRequest request) {
        return ResponseEntity.ok(itemService.updateItem(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Integer id) {
        itemService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }
}
