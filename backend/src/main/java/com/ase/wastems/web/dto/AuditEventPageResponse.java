package com.ase.wastems.web.dto;

import com.ase.wastems.domain.AuditEvent;
import java.util.List;
import org.springframework.data.domain.Page;

/** One page of the audit log, newest first (US-39). */
public record AuditEventPageResponse(
        List<AuditEventResponse> items, int page, int size, long totalElements, int totalPages) {

    public static AuditEventPageResponse from(Page<AuditEvent> p) {
        return new AuditEventPageResponse(
                p.getContent().stream().map(AuditEventResponse::from).toList(),
                p.getNumber(),
                p.getSize(),
                p.getTotalElements(),
                p.getTotalPages());
    }
}
