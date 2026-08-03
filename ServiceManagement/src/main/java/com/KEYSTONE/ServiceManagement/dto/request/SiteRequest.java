package com.KEYSTONE.ServiceManagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SiteRequest(
        @NotNull Long customerId,
        @NotBlank String name,
        @NotBlank String address,
        String city,
        String state,
        String postalCode
) {
}
