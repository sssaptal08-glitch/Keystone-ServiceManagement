package com.KEYSTONE.ServiceManagement.service;

import com.KEYSTONE.ServiceManagement.domain.Customer;
import com.KEYSTONE.ServiceManagement.dto.request.CustomerRequest;
import com.KEYSTONE.ServiceManagement.dto.response.CustomerResponse;
import com.KEYSTONE.ServiceManagement.dto.response.PageResponse;
import com.KEYSTONE.ServiceManagement.exception.NotFoundException;
import com.KEYSTONE.ServiceManagement.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customerRepository;

    public PageResponse<CustomerResponse> findAll(String search, Pageable pageable) {
        Page<Customer> page = StringUtils.hasText(search)
                ? customerRepository.findByNameContainingIgnoreCase(search.trim(), pageable)
                : customerRepository.findAll(pageable);
        return PageResponse.from(page.map(CustomerResponse::from));
    }

    public CustomerResponse findById(Long id) {
        return CustomerResponse.from(getOrThrow(id));
    }

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        Customer customer = Customer.builder()
                .name(request.name())
                .contactEmail(request.contactEmail())
                .contactPhone(request.contactPhone())
                .build();
        return CustomerResponse.from(customerRepository.save(customer));
    }

    @Transactional
    public CustomerResponse update(Long id, CustomerRequest request) {
        Customer customer = getOrThrow(id);
        customer.setName(request.name());
        customer.setContactEmail(request.contactEmail());
        customer.setContactPhone(request.contactPhone());
        return CustomerResponse.from(customer);
    }

    @Transactional
    public void delete(Long id) {
        Customer customer = getOrThrow(id);
        customerRepository.delete(customer);
    }

    private Customer getOrThrow(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer not found: " + id));
    }
}
