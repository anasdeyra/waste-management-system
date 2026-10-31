package com.ase.wastems.web;

import com.ase.wastems.domain.AgentStatus;
import com.ase.wastems.domain.CollectionPointStatus;
import com.ase.wastems.domain.VehicleStatus;
import com.ase.wastems.repository.AgentRepository;
import com.ase.wastems.repository.CollectionPointRepository;
import com.ase.wastems.repository.VehicleRepository;
import com.ase.wastems.service.TourService;
import com.ase.wastems.web.dto.DashboardSummaryResponse;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Aggregated operational figures for the dashboard. */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final CollectionPointRepository collectionPointRepository;
    private final VehicleRepository vehicleRepository;
    private final AgentRepository agentRepository;
    private final TourService tourService;

    public DashboardController(
            CollectionPointRepository collectionPointRepository,
            VehicleRepository vehicleRepository,
            AgentRepository agentRepository,
            TourService tourService) {
        this.collectionPointRepository = collectionPointRepository;
        this.vehicleRepository = vehicleRepository;
        this.agentRepository = agentRepository;
        this.tourService = tourService;
    }

    @GetMapping("/summary")
    public DashboardSummaryResponse summary() {
        return new DashboardSummaryResponse(
                collectionPointRepository.count(),
                collectionPointRepository.findByStatus(CollectionPointStatus.ACTIVE).size(),
                vehicleRepository.count(),
                vehicleRepository.findByStatus(VehicleStatus.AVAILABLE).size(),
                agentRepository.count(),
                agentRepository.findByStatus(AgentStatus.AVAILABLE).size(),
                tourService.countToursPlannedOn(LocalDate.now()),
                tourService.countToursInProgress(),
                tourService.totalCollectedKg());
    }
}
