package com.ase.wastems.web;

import com.ase.wastems.domain.CollectionPoint;
import com.ase.wastems.domain.CollectionPointStatus;
import com.ase.wastems.optimization.PlanningComparisonService;
import com.ase.wastems.optimization.PlanningMethod;
import com.ase.wastems.optimization.PlanningScenario;
import com.ase.wastems.optimization.ScenarioPoint;
import com.ase.wastems.optimization.TourPlan;
import com.ase.wastems.repository.CollectionPointRepository;
import com.ase.wastems.web.dto.OptimizationComparisonResponse;
import com.ase.wastems.web.dto.OptimizationRunResponse;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints of the intelligent module and of the impact measurement (FR-19, FR-20).
 *
 * <p>The controller builds a planning scenario from the active collection points and runs the
 * three planning methods on it. The recorded history is filled by the evaluation harness of
 * Part 6, which persists the runs in a file.
 */
@RestController
@RequestMapping("/api/optimization")
public class OptimizationController {

    private final PlanningComparisonService comparisonService;
    private final CollectionPointRepository collectionPointRepository;
    private final double depotX;
    private final double depotY;
    private final double vehicleCapacityKg;
    private final double averageSpeedKmh;
    private final double serviceTimeMinutes;

    public OptimizationController(
            PlanningComparisonService comparisonService,
            CollectionPointRepository collectionPointRepository,
            @Value("${wastems.planning.depot-x:0.0}") double depotX,
            @Value("${wastems.planning.depot-y:0.0}") double depotY,
            @Value("${wastems.planning.vehicle-capacity-kg:6500}") double vehicleCapacityKg,
            @Value("${wastems.planning.average-speed-kmh:40}") double averageSpeedKmh,
            @Value("${wastems.planning.service-time-minutes:10}") double serviceTimeMinutes) {
        this.comparisonService = comparisonService;
        this.collectionPointRepository = collectionPointRepository;
        this.depotX = depotX;
        this.depotY = depotY;
        this.vehicleCapacityKg = vehicleCapacityKg;
        this.averageSpeedKmh = averageSpeedKmh;
        this.serviceTimeMinutes = serviceTimeMinutes;
    }

    /** Compares the planning methods on the current collection points. */
    @GetMapping("/comparison")
    public OptimizationComparisonResponse comparison(
            @RequestParam(defaultValue = "demo-scenario") String scenario) {

        PlanningScenario planningScenario = buildScenario(scenario);
        List<TourPlan> plans = comparisonService.compare(planningScenario);

        TourPlan baseline = find(plans, PlanningMethod.BASELINE);
        TourPlan optimized = find(plans, PlanningMethod.OPTIMIZED);
        Instant now = Instant.now();

        List<OptimizationRunResponse> results =
                plans.stream().map(plan -> toResponse(plan, scenario, now)).toList();

        return new OptimizationComparisonResponse(
                scenario,
                planningScenario.points().size(),
                results,
                baseline == null ? 0 : baseline.totalDistanceKm(),
                optimized == null ? 0 : optimized.totalDistanceKm(),
                improvement(baseline, optimized, true),
                baseline == null ? 0 : baseline.estimatedDurationMinutes(),
                optimized == null ? 0 : optimized.estimatedDurationMinutes(),
                improvement(baseline, optimized, false));
    }

    /** Recorded evaluation runs; the Part 6 harness writes this history. */
    @GetMapping("/runs")
    public List<OptimizationRunResponse> runs() {
        return List.of();
    }

    private PlanningScenario buildScenario(String name) {
        List<ScenarioPoint> points =
                collectionPointRepository.findByStatus(CollectionPointStatus.ACTIVE).stream()
                        .map(this::toScenarioPoint)
                        .toList();
        return new PlanningScenario(
                name, depotX, depotY, points, vehicleCapacityKg, averageSpeedKmh, serviceTimeMinutes);
    }

    private ScenarioPoint toScenarioPoint(CollectionPoint point) {
        return new ScenarioPoint(
                point.getId(), point.getX(), point.getY(), point.getCurrentLoadKg());
    }

    private TourPlan find(List<TourPlan> plans, PlanningMethod method) {
        return plans.stream().filter(plan -> plan.method() == method).findFirst().orElse(null);
    }

    private OptimizationRunResponse toResponse(TourPlan plan, String scenario, Instant now) {
        return new OptimizationRunResponse(
                null,
                now,
                scenario,
                plan.coveredPoints(),
                plan.method().name(),
                plan.totalDistanceKm(),
                plan.estimatedDurationMinutes(),
                0,
                plan.constraintViolations());
    }

    /** Percentage improvement of the optimized plan over the baseline, or zero when unknown. */
    private double improvement(TourPlan baseline, TourPlan optimized, boolean distance) {
        if (baseline == null || optimized == null) {
            return 0;
        }
        double base = distance ? baseline.totalDistanceKm() : baseline.estimatedDurationMinutes();
        double best = distance ? optimized.totalDistanceKm() : optimized.estimatedDurationMinutes();
        if (base <= 0) {
            return 0;
        }
        return (base - best) / base * 100.0;
    }
}
