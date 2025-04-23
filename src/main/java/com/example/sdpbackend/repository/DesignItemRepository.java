package com.example.sdpbackend.repository;

import com.example.sdpbackend.entity.Design;
import com.example.sdpbackend.entity.DesignItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DesignItemRepository extends JpaRepository<DesignItem,Integer>{
    List<DesignItem> findByDesign(Design design);
    void deleteByDesign(Design design);
}
