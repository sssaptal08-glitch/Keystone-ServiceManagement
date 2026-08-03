package com.KEYSTONE.ServiceManagement.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PartRequest(
        @NotBlank String sku,
        @NotBlank String name,
        @NotNull @DecimalMin("0.0") BigDecimal unitCost,
        Integer quantityOnHand,
        Integer reorderThreshold
) {
}
