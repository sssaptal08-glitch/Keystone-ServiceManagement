package com.KEYSTONE.ServiceManagement.dto.response;

import com.KEYSTONE.ServiceManagement.domain.User;

/**
 * Technician list entry used on the assignment dropdown, annotated with how many open jobs
 * they're currently carrying — the lightweight "skill/availability" signal the brief lists as
 * an optional stretch goal (13.1), so a dispatcher can balance load at a glance without a full
 * scheduling engine.
 */
public record TechnicianWorkloadResponse(
        Long id,
        String name,
        String email,
        long openJobCount
) {
    public static TechnicianWorkloadResponse from(User user, long openJobCount) {
        return new TechnicianWorkloadResponse(user.getId(), user.getName(), user.getEmail(), openJobCount);
    }
}
