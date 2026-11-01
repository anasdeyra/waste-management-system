package com.ase.wastems.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

/**
 * An operational person who can drive or collect (D-011/D-014). The optional one-to-one login
 * link to an AGENT account (D-030) is owned by this side.
 */
@Entity
@Table(
        name = "operational_agent",
        uniqueConstraints =
                @UniqueConstraint(name = "uk_agent_user_account", columnNames = "user_account_id"))
public class Agent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResourceStatus status = ResourceStatus.AVAILABLE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DataOrigin origin;

    @OneToOne
    @JoinColumn(name = "user_account_id")
    private AppUser userAccount;

    protected Agent() {}

    public Agent(String displayName, DataOrigin origin) {
        this.displayName = displayName;
        this.origin = origin;
    }

    public UUID getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public ResourceStatus getStatus() {
        return status;
    }

    public void setStatus(ResourceStatus status) {
        this.status = status;
    }

    public DataOrigin getOrigin() {
        return origin;
    }

    public boolean isAvailable() {
        return status == ResourceStatus.AVAILABLE;
    }

    public AppUser getUserAccount() {
        return userAccount;
    }

    public void linkUserAccount(AppUser user) {
        this.userAccount = user;
    }

    public void unlinkUserAccount() {
        this.userAccount = null;
    }
}
