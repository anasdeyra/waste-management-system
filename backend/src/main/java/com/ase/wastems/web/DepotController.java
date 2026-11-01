package com.ase.wastems.web;

import com.ase.wastems.service.DepotService;
import com.ase.wastems.web.dto.DepotResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read-only depot API; PLANNER and ADMIN only (SecurityConfig). */
@RestController
@RequestMapping("/api/depots")
public class DepotController {

    private final DepotService service;

    public DepotController(DepotService service) {
        this.service = service;
    }

    @GetMapping
    public List<DepotResponse> list() {
        return service.list().stream().map(DepotResponse::from).toList();
    }

    @GetMapping("/{id}")
    public DepotResponse get(@PathVariable UUID id) {
        return DepotResponse.from(service.get(id));
    }
}
