package com.ase.wastems.web;

import com.ase.wastems.domain.AppUser;
import com.ase.wastems.service.UserAdminService;
import com.ase.wastems.web.dto.ActiveStatusRequest;
import com.ase.wastems.web.dto.CreateUserRequest;
import com.ase.wastems.web.dto.UserResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Account administration API. Restricted to ADMIN by the URL rules of SecurityConfig. */
@RestController
@RequestMapping("/api/users")
public class UserAdminController {

    private final UserAdminService service;

    public UserAdminController(UserAdminService service) {
        this.service = service;
    }

    @GetMapping
    public List<UserResponse> list() {
        return service.list().stream().map(UserResponse::from).toList();
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable UUID id) {
        return UserResponse.from(service.get(id));
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest req) {
        AppUser created = service.create(req.username(), req.password(), req.role());
        return ResponseEntity.created(URI.create("/api/users/" + created.getId()))
                .body(UserResponse.from(created));
    }

    @PatchMapping("/{id}/status")
    public UserResponse setStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ActiveStatusRequest req,
            Authentication auth) {
        return UserResponse.from(service.setActive(id, req.active(), auth.getName()));
    }
}
