package com.credlink.dashboard;

import com.credlink.common.ApiResponse;
import com.credlink.dashboard.dto.DashboardSummaryResponse;
import com.credlink.dashboard.dto.TrendPoint;
import com.credlink.security.CurrentMerchant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final CurrentMerchant currentMerchant;

    public DashboardController(DashboardService dashboardService, CurrentMerchant currentMerchant) {
        this.dashboardService = dashboardService;
        this.currentMerchant = currentMerchant;
    }

    @GetMapping("/summary")
    public ApiResponse<DashboardSummaryResponse> summary() {
        return ApiResponse.ok(dashboardService.summary(currentMerchant.id()));
    }

    @GetMapping("/trend")
    public ApiResponse<List<TrendPoint>> trend(@RequestParam(defaultValue = "7") int days) {
        return ApiResponse.ok(dashboardService.trend(currentMerchant.id(), days));
    }
}
