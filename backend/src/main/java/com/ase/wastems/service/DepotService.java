package com.ase.wastems.service;

import com.ase.wastems.domain.Depot;
import com.ase.wastems.repository.DepotRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read access to depots (read-only in S2). */
@Service
public class DepotService {

    private final DepotRepository depots;

    public DepotService(DepotRepository depots) {
        this.depots = depots;
    }

    @Transactional(readOnly = true)
    public List<Depot> list() {
        return depots.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Depot get(UUID id) {
        return depots.findById(id).orElseThrow(() -> new NotFoundException("Depot not found: " + id));
    }
}
