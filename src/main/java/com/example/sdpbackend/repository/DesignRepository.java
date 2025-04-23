package com.example.sdpbackend.repository;

import com.example.sdpbackend.entity.Category;
import com.example.sdpbackend.entity.Design;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DesignRepository extends JpaRepository<Design, Integer> {
    List<Design> findByCategory(Category category);
}