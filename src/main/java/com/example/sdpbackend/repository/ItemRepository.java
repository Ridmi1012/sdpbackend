package com.example.sdpbackend.repository;

import com.example.sdpbackend.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<Item, Integer> {
    // Method for searching items by name (case-insensitive)
    List<Item> findByNameContainingIgnoreCase(String name);
}
