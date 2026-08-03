package com.KEYSTONE.ServiceManagement.dto.request;

/** F6.2: "Time entries record minutes and an optional note." Minutes are computed automatically
 * from the clock-in/clock-out timestamps; the technician can add a free-text note describing
 * what was done during that session. */
public record ClockOutRequest(String note) {
}
