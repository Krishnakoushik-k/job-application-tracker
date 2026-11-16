package com.jobtracker.controller;

import com.jobtracker.dto.DashboardResponse;
import com.jobtracker.security.UserPrincipal;
import com.jobtracker.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Statistics calculated from the authenticated user's records")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Operation(summary = "Get dashboard statistics",
            description = "Totals per status, success rate (offers / total), 12-month trend, "
                    + "upcoming interviews and recent applications.")
    @GetMapping
    public DashboardResponse getStatistics(@AuthenticationPrincipal UserPrincipal principal) {
        return dashboardService.getStatistics(principal.getId());
    }
}
