package com.KEYSTONE.ServiceManagement.dto.response;

import com.KEYSTONE.ServiceManagement.domain.Site;

public record SiteResponse(
        Long id,
        Long customerId,
        String customerName,
        String name,
        String address,
        String city,
        String state,
        String postalCode
) {
    public static SiteResponse from(Site s) {
        return new SiteResponse(
                s.getId(),
                s.getCustomer().getId(),
                s.getCustomer().getName(),
                s.getName(),
                s.getAddress(),
                s.getCity(),
                s.getState(),
                s.getPostalCode()
        );
    }
}
