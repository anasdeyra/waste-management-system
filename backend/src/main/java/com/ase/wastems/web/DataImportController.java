package com.ase.wastems.web;

import com.ase.wastems.service.ImportTrigger;
import com.ase.wastems.service.Mn01ImportReport;
import com.ase.wastems.service.Mn01ImportService;
import com.ase.wastems.service.NotFoundException;
import com.ase.wastems.web.dto.ImportReportResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** MN01 import administration; ADMIN only (SecurityConfig). */
@RestController
@RequestMapping("/api/admin/import")
public class DataImportController {

    private final Mn01ImportService service;

    public DataImportController(Mn01ImportService service) {
        this.service = service;
    }

    @PostMapping
    public ImportReportResponse run() {
        Mn01ImportReport report = service.importAll(ImportTrigger.MANUAL);
        return ImportReportResponse.from(report, service.snapshots());
    }

    @GetMapping("/report")
    public ImportReportResponse report() {
        Mn01ImportReport report =
                service.lastReport()
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                "No import has run since the application started."));
        return ImportReportResponse.from(report, service.snapshots());
    }
}
