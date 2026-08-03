package com.KEYSTONE.ServiceManagement.dto.response;

/**
 * The "at risk" window used by WorkOrderResponse's static factory to flag a work order as
 * approaching its SLA due date. WorkOrderResponse.from() is used as a bare method reference
 * (e.g. page.map(WorkOrderResponse::from)) all over the codebase, so it can't take the live
 * SlaProperties bean as a parameter without breaking every call site — this constant is the
 * pragmatic tradeoff. Keep it in sync with the default in application.properties
 * (app.sla.at-risk-hours).
 */
public final class SlaThresholds {
    public static final long AT_RISK_HOURS = 2;

    private SlaThresholds() {
    }
}
