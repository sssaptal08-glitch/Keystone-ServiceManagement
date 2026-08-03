package com.KEYSTONE.ServiceManagement.service;

import com.KEYSTONE.ServiceManagement.domain.Priority;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Central place for SLA timing rules: how many hours out a due date should default to per
 * priority (F7.1: "Each work order gets an SLA due date based on priority"), and how many hours
 * out counts as "at risk" of breaching before it actually does (F7.2).
 *
 * Kept as its own small component (rather than @Value fields directly on WorkOrderService) so it
 * can be constructed with plain values in unit tests without needing a Spring context.
 */
@Component
public class SlaProperties {

    private final long criticalHours;
    private final long highHours;
    private final long mediumHours;
    private final long lowHours;
    private final long atRiskHours;

    public SlaProperties(
            @Value("${app.sla.critical-hours:4}") long criticalHours,
            @Value("${app.sla.high-hours:8}") long highHours,
            @Value("${app.sla.medium-hours:24}") long mediumHours,
            @Value("${app.sla.low-hours:72}") long lowHours,
            @Value("${app.sla.at-risk-hours:2}") long atRiskHours) {
        this.criticalHours = criticalHours;
        this.highHours = highHours;
        this.mediumHours = mediumHours;
        this.lowHours = lowHours;
        this.atRiskHours = atRiskHours;
    }

    /** Default SLA due date for a newly-created work order that didn't specify one explicitly. */
    public Instant defaultDueDate(Priority priority, Instant from) {
        return from.plus(hoursFor(priority), ChronoUnit.HOURS);
    }

    public long hoursFor(Priority priority) {
        return switch (priority) {
            case CRITICAL -> criticalHours;
            case HIGH -> highHours;
            case MEDIUM -> mediumHours;
            case LOW -> lowHours;
        };
    }

    public long getAtRiskHours() {
        return atRiskHours;
    }
}
