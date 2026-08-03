package com.KEYSTONE.ServiceManagement.dto.response;

import com.KEYSTONE.ServiceManagement.domain.Part;

import java.math.BigDecimal;

public record PartResponse(
        Long id,
        String sku,
        String name,
        BigDecimal unitCost,
        Integer quantityOnHand,
        Integer reorderThreshold,
        boolean lowStock
) {
    public static PartResponse from(Part p) {
        return new PartResponse(
                p.getId(), p.getSku(), p.getName(), p.getUnitCost(),
                p.getQuantityOnHand(), p.getReorderThreshold(),
                p.getQuantityOnHand() <= p.getReorderThreshold()
        );
    }
}
