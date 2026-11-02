package com.ase.wastems.web;

import com.ase.wastems.service.DashboardService;
import com.ase.wastems.web.dto.OperationsDashboardResponse;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Operational dashboard API (US-35); PLANNER and ADMIN only (SecurityConfig). */
@RestController
public class DashboardController {

    private final DashboardService dashboard;

    public DashboardController(DashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping("/api/dashboard/operations")
    public OperationsDashboardResponse operations(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return OperationsDashboardResponse.from(dashboard.operations(from, to));
    }
}
