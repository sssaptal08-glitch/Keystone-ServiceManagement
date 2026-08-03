package com.KEYSTONE.ServiceManagement.dto.response;

import com.KEYSTONE.ServiceManagement.domain.Role;
import com.KEYSTONE.ServiceManagement.domain.User;

public record UserResponse(
        Long id,
        String name,
        String email,
        Role role,
        Long customerId
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getCustomer() != null ? user.getCustomer().getId() : null
        );
    }
}
