package com.ase.wastems.web;

import com.ase.wastems.domain.WasteCategory;
import com.ase.wastems.service.WasteCategoryService;
import com.ase.wastems.web.dto.ActiveStatusRequest;
import com.ase.wastems.web.dto.WasteCategoryRequest;
import com.ase.wastems.web.dto.WasteCategoryResponse;
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

/** Waste category API: readable by any signed-in user, writable by ADMIN (SecurityConfig). */
@RestController
@RequestMapping("/api/waste-categories")
public class WasteCategoryController {

    private final WasteCategoryService service;

    public WasteCategoryController(WasteCategoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<WasteCategoryResponse> list(@RequestParam(required = false) Boolean active) {
        return service.list(active).stream().map(WasteCategoryResponse::from).toList();
    }

    @GetMapping("/{id}")
    public WasteCategoryResponse get(@PathVariable UUID id) {
        return WasteCategoryResponse.from(service.get(id));
    }

    @PostMapping
    public ResponseEntity<WasteCategoryResponse> create(
            @Valid @RequestBody WasteCategoryRequest req) {
        WasteCategory c = service.create(req.name(), req.description());
        return ResponseEntity.created(URI.create("/api/waste-categories/" + c.getId()))
                .body(WasteCategoryResponse.from(c));
    }

    @PutMapping("/{id}")
    public WasteCategoryResponse update(
            @PathVariable UUID id, @Valid @RequestBody WasteCategoryRequest req) {
        return WasteCategoryResponse.from(service.update(id, req.name(), req.description()));
    }

    @PatchMapping("/{id}/status")
    public WasteCategoryResponse setStatus(
            @PathVariable UUID id, @Valid @RequestBody ActiveStatusRequest req) {
        return WasteCategoryResponse.from(service.setActive(id, req.active()));
    }
}
