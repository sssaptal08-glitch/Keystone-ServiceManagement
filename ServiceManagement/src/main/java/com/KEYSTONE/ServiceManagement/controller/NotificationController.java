package com.KEYSTONE.ServiceManagement.controller;

import com.KEYSTONE.ServiceManagement.dto.response.NotificationResponse;
import com.KEYSTONE.ServiceManagement.dto.response.PageResponse;
import com.KEYSTONE.ServiceManagement.security.SecurityUser;
import com.KEYSTONE.ServiceManagement.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public PageResponse<NotificationResponse> findMine(@RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size,
                                                        Authentication authentication) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return notificationService.findForUser(userId(authentication), pageable);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(Authentication authentication) {
        return Map.of("unreadCount", notificationService.unreadCount(userId(authentication)));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id, Authentication authentication) {
        notificationService.markRead(userId(authentication), id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead(Authentication authentication) {
        notificationService.markAllRead(userId(authentication));
        return ResponseEntity.noContent().build();
    }

    private Long userId(Authentication authentication) {
        return ((SecurityUser) authentication.getPrincipal()).getId();
    }
}
