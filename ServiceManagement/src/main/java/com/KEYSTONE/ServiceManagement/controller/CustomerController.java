package com.KEYSTONE.ServiceManagement.controller;

import com.KEYSTONE.ServiceManagement.dto.request.CustomerRequest;
import com.KEYSTONE.ServiceManagement.dto.response.CustomerResponse;
import com.KEYSTONE.ServiceManagement.dto.response.PageResponse;
import com.KEYSTONE.ServiceManagement.service.CustomerService;
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
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER','TECHNICIAN')")
    public PageResponse<CustomerResponse> findAll(@RequestParam(required = false) String search,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("name"));
        return customerService.findAll(search, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER','TECHNICIAN')")
    public CustomerResponse findById(@PathVariable Long id) {
        return customerService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
    public CustomerResponse update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        return customerService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
