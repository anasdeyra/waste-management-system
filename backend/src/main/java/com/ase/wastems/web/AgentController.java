package com.ase.wastems.web;

import com.ase.wastems.domain.Agent;
import com.ase.wastems.domain.ResourceStatus;
import com.ase.wastems.service.AgentService;
import com.ase.wastems.web.dto.AgentRequest;
import com.ase.wastems.web.dto.AgentResponse;
import com.ase.wastems.web.dto.LinkUserAccountRequest;
import com.ase.wastems.web.dto.ResourceStatusRequest;
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

/** Operational agent API; PLANNER and ADMIN only (SecurityConfig). There is no delete. */
@RestController
@RequestMapping("/api/agents")
public class AgentController {

    private final AgentService service;

    public AgentController(AgentService service) {
        this.service = service;
    }

    @GetMapping
    public List<AgentResponse> list(@RequestParam(required = false) ResourceStatus status) {
        return service.list(status).stream().map(AgentResponse::from).toList();
    }

    @GetMapping("/{id}")
    public AgentResponse get(@PathVariable UUID id) {
        return AgentResponse.from(service.get(id));
    }

    @PostMapping
    public ResponseEntity<AgentResponse> create(@Valid @RequestBody AgentRequest req) {
        Agent a = service.create(req.displayName());
        return ResponseEntity.created(URI.create("/api/agents/" + a.getId()))
                .body(AgentResponse.from(a));
    }

    @PutMapping("/{id}")
    public AgentResponse update(@PathVariable UUID id, @Valid @RequestBody AgentRequest req) {
        return AgentResponse.from(service.update(id, req.displayName()));
    }

    @PatchMapping("/{id}/status")
    public AgentResponse setStatus(
            @PathVariable UUID id, @Valid @RequestBody ResourceStatusRequest req) {
        return AgentResponse.from(service.setStatus(id, req.status()));
    }

    /** ADMIN only (SecurityConfig): links the agent to an AGENT account. */
    @PutMapping("/{id}/user-account")
    public AgentResponse linkUserAccount(
            @PathVariable UUID id, @Valid @RequestBody LinkUserAccountRequest req) {
        return AgentResponse.from(service.linkUserAccount(id, req.userId()));
    }

    /** ADMIN only (SecurityConfig): removes the link; idempotent. */
    @DeleteMapping("/{id}/user-account")
    public ResponseEntity<Void> unlinkUserAccount(@PathVariable UUID id) {
        service.unlinkUserAccount(id);
        return ResponseEntity.noContent().build();
    }
}
