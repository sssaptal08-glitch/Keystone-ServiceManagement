package com.KEYSTONE.ServiceManagement.repository;

import com.KEYSTONE.ServiceManagement.domain.Part;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PartRepository extends JpaRepository<Part, Long> {
    Optional<Part> findBySku(String sku);
    boolean existsBySku(String sku);
    Page<Part> findByNameContainingIgnoreCaseOrSkuContainingIgnoreCase(String name, String sku, Pageable pageable);
}
