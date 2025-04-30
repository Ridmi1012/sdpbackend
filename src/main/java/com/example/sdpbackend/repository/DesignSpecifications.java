package com.example.sdpbackend.repository;

import com.example.sdpbackend.dto.DesignSearchDTO;
import com.example.sdpbackend.entity.Design;
import com.example.sdpbackend.entity.DesignItem;
import com.example.sdpbackend.entity.Item;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class DesignSpecifications {
    public static Specification<Design> withDynamicQuery(DesignSearchDTO searchDTO) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Keyword search (in name or description)
            if (searchDTO.getKeyword() != null && !searchDTO.getKeyword().trim().isEmpty()) {
                String keyword = "%" + searchDTO.getKeyword().toLowerCase() + "%";

                Predicate namePredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("name")), keyword);

                Predicate descriptionPredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("description")), keyword);

                predicates.add(criteriaBuilder.or(namePredicate, descriptionPredicate));
            }

            // Category filter
            if (searchDTO.getCategoryId() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("category").get("categoryID"), searchDTO.getCategoryId()));
            }

            // Price range filter
            if (searchDTO.getMinPrice() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("basePrice"), searchDTO.getMinPrice()));
            }

            if (searchDTO.getMaxPrice() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("basePrice"), searchDTO.getMaxPrice()));
            }

            // Creator filter
            if (searchDTO.getCreatedBy() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("createdBy"), searchDTO.getCreatedBy()));
            }

            // Theme/concept search in description
            if (searchDTO.getThemes() != null && !searchDTO.getThemes().isEmpty()) {
                List<Predicate> themePredicates = new ArrayList<>();

                for (String theme : searchDTO.getThemes()) {
                    String themePattern = "%" + theme.toLowerCase() + "%";
                    themePredicates.add(criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("description")), themePattern));
                    themePredicates.add(criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("name")), themePattern));
                }

                predicates.add(criteriaBuilder.or(themePredicates.toArray(new Predicate[0])));
            }

            // Filter by items
            if (searchDTO.getItemIds() != null && !searchDTO.getItemIds().isEmpty()) {
                // Use distinct to avoid duplicate results
                query.distinct(true);

                // Join with design items
                Join<Design, DesignItem> designItemJoin = root.join("items");
                Join<DesignItem, Item> itemJoin = designItemJoin.join("item");

                // Create a predicate for each item ID
                List<Predicate> itemPredicates = new ArrayList<>();
                for (Integer itemId : searchDTO.getItemIds()) {
                    itemPredicates.add(criteriaBuilder.equal(itemJoin.get("itemID"), itemId));
                }

                // If we want designs that contain ANY of the specified items (OR condition)
                if (itemPredicates.size() > 0) {
                    predicates.add(criteriaBuilder.or(itemPredicates.toArray(new Predicate[0])));
                }

                // Alternatively, if we want designs that contain ALL of the specified items:
                // We would need a more complex approach with subqueries or post-filtering
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
