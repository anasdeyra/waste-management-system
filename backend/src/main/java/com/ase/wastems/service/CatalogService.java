package com.ase.wastems.service;

import com.ase.wastems.domain.AgentStatus;
import com.ase.wastems.domain.CollectionPointStatus;
import com.ase.wastems.domain.VehicleStatus;
import com.ase.wastems.repository.AgentRepository;
import com.ase.wastems.repository.CollectionPointRepository;
import com.ase.wastems.repository.VehicleRepository;
import com.ase.wastems.repository.WasteCategoryRepository;
import com.ase.wastems.web.dto.AgentResponse;
import com.ase.wastems.web.dto.CollectionPointResponse;
import com.ase.wastems.web.dto.VehicleResponse;
import com.ase.wastems.web.dto.WasteCategoryResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read operations on the operational reference data.
 *
 * <p>The write operations (create, update, deactivate) are implemented in the feature sprints
 * described in {@code docs/05-scrum.md}.
 */
@Service
@Transactional(readOnly = true)
public class CatalogService {

    private final CollectionPointRepository collectionPointRepository;
    private final VehicleRepository vehicleRepository;
    private final AgentRepository agentRepository;
    private final WasteCategoryRepository wasteCategoryRepository;

    public CatalogService(
            CollectionPointRepository collectionPointRepository,
            VehicleRepository vehicleRepository,
            AgentRepository agentRepository,
            WasteCategoryRepository wasteCategoryRepository) {
        this.collectionPointRepository = collectionPointRepository;
        this.vehicleRepository = vehicleRepository;
        this.agentRepository = agentRepository;
        this.wasteCategoryRepository = wasteCategoryRepository;
    }

    public List<CollectionPointResponse> findCollectionPoints() {
        return collectionPointRepository.findAllByOrderByNameAsc().stream()
                .map(CollectionPointResponse::from)
                .toList();
    }

    public List<VehicleResponse> findVehicles() {
        return vehicleRepository.findAllByOrderByPlateAsc().stream().map(VehicleResponse::from).toList();
    }

    public List<AgentResponse> findAgents() {
        return agentRepository.findAllByOrderByFullNameAsc().stream().map(AgentResponse::from).toList();
    }

    public List<WasteCategoryResponse> findWasteCategories() {
        return wasteCategoryRepository.findAllByOrderByNameAsc().stream()
                .map(WasteCategoryResponse::from)
                .toList();
    }

    public long countActiveCollectionPoints() {
        return collectionPointRepository.findByStatus(CollectionPointStatus.ACTIVE).size();
    }

    public long countAvailableVehicles() {
        return vehicleRepository.findByStatus(VehicleStatus.AVAILABLE).size();
    }

    public long countAvailableAgents() {
        return agentRepository.findByStatus(AgentStatus.AVAILABLE).size();
    }
}
