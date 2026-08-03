package com.KEYSTONE.ServiceManagement.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CustomerRequest(
        @NotBlank String name,
        String contactEmail,
        String contactPhone
) {
}
