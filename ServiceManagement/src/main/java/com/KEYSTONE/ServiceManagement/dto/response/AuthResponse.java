package com.KEYSTONE.ServiceManagement.dto.response;

public record AuthResponse(
        String token,
        UserResponse user
) {
}
