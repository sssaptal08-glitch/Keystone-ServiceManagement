package com.KEYSTONE.ServiceManagement.service;

import com.KEYSTONE.ServiceManagement.domain.Customer;
import com.KEYSTONE.ServiceManagement.domain.Site;
import com.KEYSTONE.ServiceManagement.dto.request.SiteRequest;
import com.KEYSTONE.ServiceManagement.dto.response.SiteResponse;
import com.KEYSTONE.ServiceManagement.exception.NotFoundException;
import com.KEYSTONE.ServiceManagement.repository.CustomerRepository;
import com.KEYSTONE.ServiceManagement.repository.SiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SiteService {

    private final SiteRepository siteRepository;
    private final CustomerRepository customerRepository;

    public List<SiteResponse> findAll(Long customerId) {
        List<Site> sites = customerId != null
                ? siteRepository.findByCustomerId(customerId)
                : siteRepository.findAll();
        return sites.stream().map(SiteResponse::from).toList();
    }

    public SiteResponse findById(Long id) {
        return SiteResponse.from(getOrThrow(id));
    }

    @Transactional
    public SiteResponse create(SiteRequest request) {
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new NotFoundException("Customer not found: " + request.customerId()));

        Site site = Site.builder()
                .customer(customer)
                .name(request.name())
                .address(request.address())
                .city(request.city())
                .state(request.state())
                .postalCode(request.postalCode())
                .build();
        return SiteResponse.from(siteRepository.save(site));
    }

    @Transactional
    public SiteResponse update(Long id, SiteRequest request) {
        Site site = getOrThrow(id);
        if (!site.getCustomer().getId().equals(request.customerId())) {
            Customer customer = customerRepository.findById(request.customerId())
                    .orElseThrow(() -> new NotFoundException("Customer not found: " + request.customerId()));
            site.setCustomer(customer);
        }
        site.setName(request.name());
        site.setAddress(request.address());
        site.setCity(request.city());
        site.setState(request.state());
        site.setPostalCode(request.postalCode());
        return SiteResponse.from(site);
    }

    @Transactional
    public void delete(Long id) {
        siteRepository.delete(getOrThrow(id));
    }

    private Site getOrThrow(Long id) {
        return siteRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Site not found: " + id));
    }
}
