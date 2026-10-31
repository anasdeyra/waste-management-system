package com.ase.wastems.bootstrap;

import com.ase.wastems.domain.Agent;
import com.ase.wastems.domain.AgentRole;
import com.ase.wastems.domain.AgentStatus;
import com.ase.wastems.domain.AppRole;
import com.ase.wastems.domain.AppUser;
import com.ase.wastems.domain.CollectionPoint;
import com.ase.wastems.domain.Vehicle;
import com.ase.wastems.domain.VehicleStatus;
import com.ase.wastems.domain.WasteCategory;
import com.ase.wastems.repository.AgentRepository;
import com.ase.wastems.repository.AppUserRepository;
import com.ase.wastems.repository.CollectionPointRepository;
import com.ase.wastems.repository.VehicleRepository;
import com.ase.wastems.repository.WasteCategoryRepository;
import java.util.EnumSet;
import java.util.List;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads the demonstration data set so that a reviewer can start the prototype and use it
 * immediately (Part 4, "seed/demo data").
 *
 * <p>The seeding is idempotent: it only runs when the database is empty.
 */
@Component
public class DemoDataSeeder implements ApplicationRunner {

    private final AppUserRepository userRepository;
    private final WasteCategoryRepository wasteCategoryRepository;
    private final CollectionPointRepository collectionPointRepository;
    private final VehicleRepository vehicleRepository;
    private final AgentRepository agentRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(
            AppUserRepository userRepository,
            WasteCategoryRepository wasteCategoryRepository,
            CollectionPointRepository collectionPointRepository,
            VehicleRepository vehicleRepository,
            AgentRepository agentRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.wasteCategoryRepository = wasteCategoryRepository;
        this.collectionPointRepository = collectionPointRepository;
        this.vehicleRepository = vehicleRepository;
        this.agentRepository = agentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }

        seedUsers();
        seedWasteCategories();
        seedVehicles();
        seedAgents();
        seedCollectionPoints();
    }

    private void seedUsers() {
        userRepository.save(
                new AppUser(
                        "admin",
                        passwordEncoder.encode("admin123"),
                        "Amina Administrator",
                        EnumSet.of(AppRole.ADMIN, AppRole.MANAGER)));
        userRepository.save(
                new AppUser(
                        "manager",
                        passwordEncoder.encode("manager123"),
                        "Yassine Manager",
                        EnumSet.of(AppRole.MANAGER)));
        userRepository.save(
                new AppUser(
                        "agent",
                        passwordEncoder.encode("agent123"),
                        "Salma Agent",
                        EnumSet.of(AppRole.AGENT)));
    }

    private void seedWasteCategories() {
        wasteCategoryRepository.saveAll(
                List.of(
                        new WasteCategory("Household waste", "Residual waste collected from households", false),
                        new WasteCategory("Packaging", "Plastic, metal and cardboard packaging", true),
                        new WasteCategory("Glass", "Glass containers collected separately", true),
                        new WasteCategory("Organic waste", "Biodegradable waste from markets and restaurants", true),
                        new WasteCategory("Bulky waste", "Furniture, mattresses and other large items", false)));
    }

    private void seedVehicles() {
        vehicleRepository.saveAll(
                List.of(
                        new Vehicle("WT-101-A", "Small compactor", 1800, VehicleStatus.AVAILABLE),
                        new Vehicle("WT-102-B", "Small compactor", 1800, VehicleStatus.AVAILABLE),
                        new Vehicle("WT-201-A", "Large rear loader", 6500, VehicleStatus.AVAILABLE),
                        new Vehicle("WT-301-C", "Glass collection truck", 3200, VehicleStatus.MAINTENANCE)));
    }

    private void seedAgents() {
        agentRepository.saveAll(
                List.of(
                        new Agent("Karim Belhadj", "+212 600 100 001", AgentRole.DRIVER, AgentStatus.AVAILABLE),
                        new Agent("Nadia Tazi", "+212 600 100 002", AgentRole.DRIVER, AgentStatus.AVAILABLE),
                        new Agent("Omar Idrissi", "+212 600 100 003", AgentRole.COLLECTOR, AgentStatus.AVAILABLE),
                        new Agent("Laila Amrani", "+212 600 100 004", AgentRole.COLLECTOR, AgentStatus.AVAILABLE),
                        new Agent("Hicham Ouazzani", "+212 600 100 005", AgentRole.COLLECTOR, AgentStatus.AVAILABLE),
                        new Agent("Fatima Zahra", "+212 600 100 006", AgentRole.DRIVER, AgentStatus.UNAVAILABLE),
                        new Agent("Rachid Alaoui", "+212 600 100 007", AgentRole.COLLECTOR, AgentStatus.AVAILABLE)));
    }

    private void seedCollectionPoints() {
        List<WasteCategory> categories = wasteCategoryRepository.findAllByOrderByNameAsc();
        WasteCategory household = category(categories, "Household waste");
        WasteCategory packaging = category(categories, "Packaging");
        WasteCategory glass = category(categories, "Glass");
        WasteCategory organic = category(categories, "Organic waste");
        WasteCategory bulky = category(categories, "Bulky waste");

        collectionPointRepository.saveAll(
                List.of(
                        point("Market Square", "Downtown, market entrance", 2.0, 3.0, 1200, 820, household),
                        point("Central Post Office", "Downtown, Avenue Hassan II", 3.5, 1.5, 900, 610, packaging),
                        point("University Residence", "North district, campus gate", 6.0, 8.0, 1500, 1490, household),
                        point("Glass Bank", "North district, industrial road", 8.5, 9.5, 700, 690, glass),
                        point("Grand Mosque", "Old town, historical quarter", 1.0, 6.5, 800, 340, household),
                        point("City Hospital", "West district, hospital road", 9.0, 3.5, 2200, 1750, household),
                        point("Food Court", "West district, business park", 7.5, 1.0, 1300, 980, organic),
                        point("Riverside Park", "South bank, park entrance", 4.0, 10.0, 600, 210, organic),
                        point("Secondary School", "East district, school street", 11.0, 6.0, 850, 520, packaging),
                        point("Warehouse District", "East district, logistics zone", 13.0, 9.0, 2000, 1750, packaging),
                        point("Bulky Waste Depot", "South district, transfer site", 6.0, 12.5, 3000, 2650, bulky),
                        point("Farmers Market", "Outer ring, exit 4", 12.0, 1.5, 750, 430, organic)));
    }

    private CollectionPoint point(
            String name,
            String address,
            double x,
            double y,
            double capacityKg,
            double currentLoadKg,
            WasteCategory category) {
        return new CollectionPoint(
                name, address, x, y, capacityKg, currentLoadKg, category);
    }

    private WasteCategory category(List<WasteCategory> categories, String name) {
        return categories.stream()
                .filter(category -> category.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Unknown waste category: " + name));
    }
}
