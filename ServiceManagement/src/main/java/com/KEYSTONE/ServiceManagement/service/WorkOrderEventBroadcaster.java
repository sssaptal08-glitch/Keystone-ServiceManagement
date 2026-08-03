package com.KEYSTONE.ServiceManagement.service;

import com.KEYSTONE.ServiceManagement.dto.response.WorkOrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Publishes work order lifecycle events over STOMP so connected clients (dashboards, technician
 * job boards) update live instead of waiting on the next poll/refresh.
 *
 * Frontend subscribes to /topic/work-orders and receives a WorkOrderEvent payload on every
 * create, assignment, or status change.
 */
@Service
@RequiredArgsConstructor
public class WorkOrderEventBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcast(String eventType, WorkOrderResponse workOrder) {
        messagingTemplate.convertAndSend("/topic/work-orders", new WorkOrderEvent(eventType, workOrder));
    }

    public record WorkOrderEvent(String type, WorkOrderResponse workOrder) {
    }
}
