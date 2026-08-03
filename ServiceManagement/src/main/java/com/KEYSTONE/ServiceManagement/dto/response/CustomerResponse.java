package com.KEYSTONE.ServiceManagement.dto.response;

import com.KEYSTONE.ServiceManagement.domain.Customer;

public record CustomerResponse(
        Long id,
        String name,
        String contactEmail,
        String contactPhone
) {
    public static CustomerResponse from(Customer c) {
        return new CustomerResponse(c.getId(), c.getName(), c.getContactEmail(), c.getContactPhone());
    }
}
