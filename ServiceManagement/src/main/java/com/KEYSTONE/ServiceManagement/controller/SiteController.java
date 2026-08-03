package com.KEYSTONE.ServiceManagement.controller;

import com.KEYSTONE.ServiceManagement.dto.request.SiteRequest;
import com.KEYSTONE.ServiceManagement.dto.response.SiteResponse;
import com.KEYSTONE.ServiceManagement.service.SiteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sites")
@RequiredArgsConstructor
public class SiteController {

    private final SiteService siteService;

    @GetMapping
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER','TECHNICIAN')")
    public List<SiteResponse> findAll(@RequestParam(required = false) Long customerId) {
        return siteService.findAll(customerId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER','TECHNICIAN')")
    public SiteResponse findById(@PathVariable Long id) {
        return siteService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
    public ResponseEntity<SiteResponse> create(@Valid @RequestBody SiteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(siteService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
    public SiteResponse update(@PathVariable Long id, @Valid @RequestBody SiteRequest request) {
        return siteService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        siteService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
