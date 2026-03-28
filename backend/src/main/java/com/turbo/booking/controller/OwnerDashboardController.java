package com.turbo.booking.controller;

import com.turbo.booking.controller.mapper.OwnerDashboardControllerMapper;
import com.turbo.booking.dto.OwnerDashboardResponse;
import com.turbo.booking.service.BookingService;
import com.turbo.booking.service.result.OwnerDashboardStats;
import com.turbo.config.SecurityHelper;
import com.turbo.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/owner/dashboard")
@RequiredArgsConstructor
public class OwnerDashboardController {

    private final BookingService bookingService;
    private final OwnerDashboardControllerMapper controllerMapper;
    private final SecurityHelper securityHelper;

    @GetMapping
    public ResponseEntity<OwnerDashboardResponse> getDashboardStats() {
        User user = securityHelper.getCurrentUser();
        OwnerDashboardStats stats = bookingService.getOwnerDashboardStats(user.getUserId());
        return ResponseEntity.ok(controllerMapper.toOwnerDashboardResponse(stats));
    }
}
