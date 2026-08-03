package com.KEYSTONE.ServiceManagement.service;

import com.KEYSTONE.ServiceManagement.domain.Part;
import com.KEYSTONE.ServiceManagement.dto.request.PartRequest;
import com.KEYSTONE.ServiceManagement.dto.request.PartRestockRequest;
import com.KEYSTONE.ServiceManagement.dto.response.PageResponse;
import com.KEYSTONE.ServiceManagement.dto.response.PartResponse;
import com.KEYSTONE.ServiceManagement.exception.DuplicateResourceException;
import com.KEYSTONE.ServiceManagement.exception.NotFoundException;
import com.KEYSTONE.ServiceManagement.repository.PartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartService {

    private final PartRepository partRepository;

    public PageResponse<PartResponse> findAll(String search, Boolean lowStockOnly, Pageable pageable) {
        Page<Part> page = StringUtils.hasText(search)
                ? partRepository.findByNameContainingIgnoreCaseOrSkuContainingIgnoreCase(search.trim(), search.trim(), pageable)
                : partRepository.findAll(pageable);

        if (Boolean.TRUE.equals(lowStockOnly)) {
            List<PartResponse> filtered = page.getContent().stream()
                    .filter(p -> p.getQuantityOnHand() <= p.getReorderThreshold())
                    .map(PartResponse::from)
                    .toList();
            return new PageResponse<>(filtered, page.getNumber(), page.getSize(), filtered.size(), 1, true);
        }

        return PageResponse.from(page.map(PartResponse::from));
    }

    public List<PartResponse> findLowStock() {
        return partRepository.findAll().stream()
                .filter(p -> p.getQuantityOnHand() <= p.getReorderThreshold())
                .map(PartResponse::from)
                .toList();
    }

    public PartResponse findById(Long id) {
        return PartResponse.from(getOrThrow(id));
    }

    @Transactional
    public PartResponse create(PartRequest request) {
        if (partRepository.existsBySku(request.sku())) {
            throw new DuplicateResourceException("A part with SKU " + request.sku() + " already exists");
        }
        Part part = Part.builder()
                .sku(request.sku())
                .name(request.name())
                .unitCost(request.unitCost())
                .quantityOnHand(request.quantityOnHand() != null ? request.quantityOnHand() : 0)
                .reorderThreshold(request.reorderThreshold() != null ? request.reorderThreshold() : 5)
                .build();
        return PartResponse.from(partRepository.save(part));
    }

    @Transactional
    public PartResponse update(Long id, PartRequest request) {
        Part part = getOrThrow(id);
        part.setName(request.name());
        part.setUnitCost(request.unitCost());
        if (request.reorderThreshold() != null) {
            part.setReorderThreshold(request.reorderThreshold());
        }
        return PartResponse.from(part);
    }

    @Transactional
    public PartResponse restock(Long id, PartRestockRequest request) {
        Part part = getOrThrow(id);
        part.setQuantityOnHand(part.getQuantityOnHand() + request.quantity());
        return PartResponse.from(part);
    }

    @Transactional
    public void delete(Long id) {
        partRepository.delete(getOrThrow(id));
    }

    private Part getOrThrow(Long id) {
        return partRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Part not found: " + id));
    }
}
