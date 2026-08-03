package com.KEYSTONE.ServiceManagement.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PartRestockRequest(
        @NotNull @Min(1) Integer quantity
) {
}
