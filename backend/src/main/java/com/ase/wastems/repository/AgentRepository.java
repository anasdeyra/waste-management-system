package com.ase.wastems.repository;

import com.ase.wastems.domain.Agent;
import com.ase.wastems.domain.AgentRole;
import com.ase.wastems.domain.AgentStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence access to {@link Agent} records. */
public interface AgentRepository extends JpaRepository<Agent, Long> {

    List<Agent> findByStatus(AgentStatus status);

    List<Agent> findByRole(AgentRole role);

    List<Agent> findAllByOrderByFullNameAsc();
}
