package com.example.sdpbackend.repository;

import com.example.sdpbackend.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository  extends JpaRepository<Category, Integer> {
    boolean existsByName(String name);
}
