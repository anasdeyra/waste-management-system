package com.ase.wastems.web;

import com.ase.wastems.domain.ResourceStatus;
import com.ase.wastems.domain.Vehicle;
import com.ase.wastems.service.VehicleService;
import com.ase.wastems.web.dto.ResourceStatusRequest;
import com.ase.wastems.web.dto.VehicleRequest;
import com.ase.wastems.web.dto.VehicleResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Vehicle API; PLANNER and ADMIN only (SecurityConfig). There is no delete: vehicles are made unavailable. */
@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleService service;

    public VehicleController(VehicleService service) {
        this.service = service;
    }

    @GetMapping
    public List<VehicleResponse> list(@RequestParam(required = false) ResourceStatus status) {
        return service.list(status).stream().map(VehicleResponse::from).toList();
    }

    @GetMapping("/{id}")
    public VehicleResponse get(@PathVariable UUID id) {
        return VehicleResponse.from(service.get(id));
    }

    @PostMapping
    public ResponseEntity<VehicleResponse> create(@Valid @RequestBody VehicleRequest req) {
        Vehicle v = service.create(req.registration(), req.capacityKg());
        return ResponseEntity.created(URI.create("/api/vehicles/" + v.getId()))
                .body(VehicleResponse.from(v));
    }

    @PutMapping("/{id}")
    public VehicleResponse update(@PathVariable UUID id, @Valid @RequestBody VehicleRequest req) {
        return VehicleResponse.from(service.update(id, req.registration(), req.capacityKg()));
    }

    @PatchMapping("/{id}/status")
    public VehicleResponse setStatus(
            @PathVariable UUID id, @Valid @RequestBody ResourceStatusRequest req) {
        return VehicleResponse.from(service.setStatus(id, req.status()));
    }
}
