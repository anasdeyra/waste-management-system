package com.ase.wastems.web;

import com.ase.wastems.service.DemoDataService;
import com.ase.wastems.service.DemoTrigger;
import com.ase.wastems.service.NotFoundException;
import com.ase.wastems.web.dto.DemoLoadReportResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Manual trigger and report of the MN01 demonstration load; ADMIN only (SecurityConfig). */
@RestController
@RequestMapping("/api/admin/demo")
public class DemoDataController {

    private final DemoDataService service;

    public DemoDataController(DemoDataService service) {
        this.service = service;
    }

    @PostMapping
    public DemoLoadReportResponse load() {
        return DemoLoadReportResponse.from(service.load(DemoTrigger.MANUAL));
    }

    /** Deletes the demonstration data and reloads it; user accounts and MN01 data are kept. */
    @PostMapping("/reset")
    public DemoLoadReportResponse reset() {
        return DemoLoadReportResponse.from(service.reset(DemoTrigger.RESET));
    }

    @GetMapping("/report")
    public DemoLoadReportResponse report() {
        return DemoLoadReportResponse.from(service.lastReport().orElseThrow(
                () -> new NotFoundException("No demo load has run since the application started.")));
    }
}
