package com.KEYSTONE.ServiceManagement.controller;

import com.KEYSTONE.ServiceManagement.dto.request.PartRequest;
import com.KEYSTONE.ServiceManagement.dto.request.PartRestockRequest;
import com.KEYSTONE.ServiceManagement.dto.response.PageResponse;
import com.KEYSTONE.ServiceManagement.dto.response.PartResponse;
import com.KEYSTONE.ServiceManagement.service.PartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/parts")
@RequiredArgsConstructor
public class PartController {

    private final PartService partService;

    @GetMapping
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER','TECHNICIAN')")
    public PageResponse<PartResponse> findAll(@RequestParam(required = false) String search,
                                               @RequestParam(required = false) Boolean lowStock,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("name"));
        return partService.findAll(search, lowStock, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER','TECHNICIAN')")
    public PartResponse findById(@PathVariable Long id) {
        return partService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
    public ResponseEntity<PartResponse> create(@Valid @RequestBody PartRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(partService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
    public PartResponse update(@PathVariable Long id, @Valid @RequestBody PartRequest request) {
        return partService.update(id, request);
    }

    @PostMapping("/{id}/restock")
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
    public PartResponse restock(@PathVariable Long id, @Valid @RequestBody PartRestockRequest request) {
        return partService.restock(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        partService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
