package com.ase.wastems.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ase.wastems.domain.Agent;
import com.ase.wastems.domain.ResourceStatus;
import com.ase.wastems.domain.Vehicle;
import com.ase.wastems.repository.AgentRepository;
import com.ase.wastems.repository.VehicleRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Availability and identity rules of vehicles and agents (US-13 to US-17). */
@SpringBootTest
@ActiveProfiles("test")
class FleetAvailabilityServiceTest {

    @Autowired private VehicleService vehicleService;
    @Autowired private AgentService agentService;
    @Autowired private VehicleRepository vehicles;
    @Autowired private AgentRepository agents;

    @BeforeEach
    void setUp() {
        agents.deleteAll();
        vehicles.deleteAll();
    }

    @Test
    void unavailableVehicleIsNotUsable() {
        Vehicle a = vehicleService.create("A-1", BigDecimal.TEN);
        Vehicle b = vehicleService.create("B-1", BigDecimal.TEN);
        vehicleService.setStatus(b.getId(), ResourceStatus.UNAVAILABLE);
        assertEquals(List.of(a.getId()), ids(vehicleService.findUsable().stream().map(Vehicle::getId)));
        vehicleService.setStatus(b.getId(), ResourceStatus.AVAILABLE);
        assertEquals(
                List.of(a.getId(), b.getId()),
                ids(vehicleService.findUsable().stream().map(Vehicle::getId)));
    }

    @Test
    void unavailableAgentIsNotUsable() {
        Agent a = agentService.create("Anna");
        Agent b = agentService.create("Boris");
        agentService.setStatus(b.getId(), ResourceStatus.UNAVAILABLE);
        assertEquals(List.of(a.getId()), ids(agentService.findUsable().stream().map(Agent::getId)));
        agentService.setStatus(b.getId(), ResourceStatus.AVAILABLE);
        assertEquals(
                List.of(a.getId(), b.getId()),
                ids(agentService.findUsable().stream().map(Agent::getId)));
    }

    @Test
    void nonPositiveCapacityIsRejectedByTheService() {
        assertThrows(BusinessRuleException.class, () -> vehicleService.create("X-1", BigDecimal.ZERO));
        UUID id = vehicleService.create("X-1", BigDecimal.TEN).getId();
        assertThrows(
                BusinessRuleException.class,
                () -> vehicleService.update(id, "X-1", new BigDecimal("-1")));
    }

    @Test
    void editPreservesIdentity() {
        UUID id = vehicleService.create("X-1", BigDecimal.TEN).getId();
        Vehicle updated = vehicleService.update(id, "X-2", BigDecimal.ONE);
        assertEquals(id, updated.getId());
        assertEquals(1, vehicles.count());
    }

    private static List<UUID> ids(java.util.stream.Stream<UUID> s) {
        return s.toList();
    }
}
