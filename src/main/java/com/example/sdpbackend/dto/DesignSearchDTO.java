package com.example.sdpbackend.dto;

import java.math.BigDecimal;
import java.util.List;

public class DesignSearchDTO {
    private String keyword;            // Search in name, description
    private Integer categoryId;        // Filter by category
    private BigDecimal minPrice;       // Price range - minimum
    private BigDecimal maxPrice;       // Price range - maximum
    private List<String> themes;       // Themes/concepts to search for
    private Integer createdBy;         // Filter by creator (admin)
    private List<Integer> itemIds;     // Filter designs containing specific items

    // Pagination and sorting
    private Integer page;              // Page number (0-based)
    private Integer size;              // Page size
    private String sortBy;             // Field to sort by
    private String sortDirection;      // ASC or DESC

    // Getters and Setters
    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
    }

    public List<String> getThemes() {
        return themes;
    }

    public void setThemes(List<String> themes) {
        this.themes = themes;
    }

    public Integer getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
    }

    public List<Integer> getItemIds() {
        return itemIds;
    }

    public void setItemIds(List<Integer> itemIds) {
        this.itemIds = itemIds;
    }

    public Integer getPage() {
        return page != null ? page : 0;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size != null ? size : 10;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String getSortBy() {
        return sortBy != null ? sortBy : "designID";
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortDirection() {
        return sortDirection != null ? sortDirection.toUpperCase() : "ASC";
    }

    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }
}
