package com.example.sdpbackend.controller;


import com.example.sdpbackend.dto.DesignDTO;

import com.example.sdpbackend.dto.DesignSearchDTO;
import com.example.sdpbackend.dto.PagedResponseDTO;
import com.example.sdpbackend.service.DesignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/designs")
@CrossOrigin(origins = "*")
public class DesignController {
    private final DesignService designService;

    @Autowired
    public DesignController(DesignService designService) {
        this.designService = designService;
    }

    @GetMapping
    public ResponseEntity<List<DesignDTO.DesignResponse>> getAllDesigns() {
        return ResponseEntity.ok(designService.getAllDesigns());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DesignDTO.DesignResponse> getDesignById(@PathVariable Integer id) {
        return ResponseEntity.ok(designService.getDesignById(id));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<DesignDTO.DesignResponse>> getDesignsByCategory(@PathVariable Integer categoryId) {
        return ResponseEntity.ok(designService.getDesignsByCategory(categoryId));
    }

    @PostMapping("/search")
    public ResponseEntity<PagedResponseDTO<DesignDTO.DesignResponse>> searchDesigns(@RequestBody DesignSearchDTO searchDTO) {
        return ResponseEntity.ok(designService.searchDesigns(searchDTO));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DesignDTO.DesignResponse> createDesign(
            @RequestPart("design") DesignDTO.DesignRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        return new ResponseEntity<>(designService.createDesign(request, image), HttpStatus.CREATED);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DesignDTO.DesignResponse> updateDesign(
            @PathVariable Integer id,
            @RequestPart("design") DesignDTO.DesignRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        return ResponseEntity.ok(designService.updateDesign(id, request, image));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDesign(@PathVariable Integer id) {
        designService.deleteDesign(id);
        return ResponseEntity.noContent().build();
    }
}