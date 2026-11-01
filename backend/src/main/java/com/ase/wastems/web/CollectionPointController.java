package com.ase.wastems.web;

import com.ase.wastems.domain.CollectionPoint;
import com.ase.wastems.domain.CollectionPointStatus;
import com.ase.wastems.domain.DemandEntry;
import com.ase.wastems.service.CollectionPointService;
import com.ase.wastems.service.DemandInput;
import com.ase.wastems.web.dto.CollectionPointResponse;
import com.ase.wastems.web.dto.CollectionPointStatusRequest;
import com.ase.wastems.web.dto.CreateCollectionPointRequest;
import com.ase.wastems.web.dto.DemandEntryRequest;
import com.ase.wastems.web.dto.DemandEntryResponse;
import com.ase.wastems.web.dto.UpdateCollectionPointRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Collection point and demand API; PLANNER and ADMIN only (SecurityConfig). */
@RestController
@RequestMapping("/api/collection-points")
public class CollectionPointController {

    private final CollectionPointService service;

    public CollectionPointController(CollectionPointService service) {
        this.service = service;
    }

    @GetMapping
    public List<CollectionPointResponse> list(
            @RequestParam(required = false) CollectionPointStatus status,
            @RequestParam(required = false) UUID categoryId) {
        return service.search(status, categoryId).stream().map(CollectionPointResponse::from).toList();
    }

    @GetMapping("/{id}")
    public CollectionPointResponse get(@PathVariable UUID id) {
        return CollectionPointResponse.from(service.get(id));
    }

    @PostMapping
    public ResponseEntity<CollectionPointResponse> create(
            @Valid @RequestBody CreateCollectionPointRequest req) {
        List<DemandInput> demands =
                req.demands().stream()
                        .map(d -> new DemandInput(d.categoryId(), d.quantityKg()))
                        .toList();
        CollectionPoint p = service.create(req.name(), req.longitude(), req.latitude(), demands);
        return ResponseEntity.created(URI.create("/api/collection-points/" + p.getId()))
                .body(CollectionPointResponse.from(p));
    }

    @PutMapping("/{id}")
    public CollectionPointResponse update(
            @PathVariable UUID id, @Valid @RequestBody UpdateCollectionPointRequest req) {
        return CollectionPointResponse.from(
                service.update(id, req.name(), req.longitude(), req.latitude()));
    }

    @PatchMapping("/{id}/status")
    public CollectionPointResponse setStatus(
            @PathVariable UUID id, @Valid @RequestBody CollectionPointStatusRequest req) {
        return CollectionPointResponse.from(service.setStatus(id, req.status()));
    }

    @PostMapping("/{id}/demands")
    public ResponseEntity<DemandEntryResponse> addDemand(
            @PathVariable UUID id, @Valid @RequestBody DemandEntryRequest req) {
        DemandEntry d = service.addDemand(id, req.categoryId(), req.quantityKg());
        return ResponseEntity.created(
                        URI.create("/api/collection-points/" + id + "/demands/" + d.getId()))
                .body(DemandEntryResponse.from(d));
    }

    @PutMapping("/{id}/demands/{demandId}")
    public DemandEntryResponse updateDemand(
            @PathVariable UUID id,
            @PathVariable UUID demandId,
            @Valid @RequestBody DemandEntryRequest req) {
        return DemandEntryResponse.from(
                service.updateDemand(id, demandId, req.categoryId(), req.quantityKg()));
    }

    @DeleteMapping("/{id}/demands/{demandId}")
    public ResponseEntity<Void> removeDemand(@PathVariable UUID id, @PathVariable UUID demandId) {
        service.removeDemand(id, demandId);
        return ResponseEntity.noContent().build();
    }
}
