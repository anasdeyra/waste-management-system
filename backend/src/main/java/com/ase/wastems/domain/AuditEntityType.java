package com.ase.wastems.domain;

/** Kind of entity an audit event refers to (US-39). */
public enum AuditEntityType {
    USER,
    WASTE_CATEGORY,
    COLLECTION_POINT,
    VEHICLE,
    AGENT,
    TOUR,
    DATA_IMPORT,
    DEMO_DATA
}
