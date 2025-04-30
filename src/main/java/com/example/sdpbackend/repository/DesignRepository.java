package com.example.sdpbackend.repository;

import com.example.sdpbackend.entity.Category;
import com.example.sdpbackend.entity.Design;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface DesignRepository extends JpaRepository<Design, Integer>, JpaSpecificationExecutor<Design> {
    List<Design> findByCategory(Category category);

    List<Design> findByCreatedBy(Integer createdBy);

    // Search by name or description
    @Query("SELECT d FROM Design d WHERE LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(d.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Design> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // Search by price range
    Page<Design> findByBasePriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    // Search by name or description and filter by category
    @Query("SELECT d FROM Design d WHERE (LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(d.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND d.category.categoryID = :categoryId")
    Page<Design> searchByKeywordAndCategory(@Param("keyword") String keyword, @Param("categoryId") Integer categoryId, Pageable pageable);

    // Find designs that contain all of the specified items
    @Query("SELECT DISTINCT d FROM Design d JOIN d.items di WHERE di.item.itemID IN :itemIds GROUP BY d.designID HAVING COUNT(DISTINCT di.item.itemID) = :itemCount")
    Page<Design> findByItemsContaining(@Param("itemIds") List<Integer> itemIds, @Param("itemCount") long itemCount, Pageable pageable);
}