package com.ase.wastems.repository;

import com.ase.wastems.domain.Agent;
import com.ase.wastems.domain.AppUser;
import com.ase.wastems.domain.DataOrigin;
import com.ase.wastems.domain.ResourceStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence of {@link Agent}. */
public interface AgentRepository extends JpaRepository<Agent, UUID> {

    List<Agent> findAllByOrderByDisplayNameAscIdAsc();

    List<Agent> findByStatusOrderByDisplayNameAscIdAsc(ResourceStatus status);

    long countByStatus(ResourceStatus status);

    Optional<Agent> findByUserAccount(AppUser user);

    List<Agent> findByUserAccountIsNotNull();

    List<Agent> findByOrigin(DataOrigin origin);
}
