package com.ase.wastems.web;

import com.ase.wastems.service.CatalogService;
import com.ase.wastems.web.dto.AgentResponse;
import com.ase.wastems.web.dto.CollectionPointResponse;
import com.ase.wastems.web.dto.VehicleResponse;
import com.ase.wastems.web.dto.WasteCategoryResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read endpoints of the operational reference data.
 *
 * <p>The path segments match the entity names used in the UML class diagram.
 */
@RestController
@RequestMapping("/api")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/collection-points")
    public List<CollectionPointResponse> collectionPoints() {
        return catalogService.findCollectionPoints();
    }

    @GetMapping("/vehicles")
    public List<VehicleResponse> vehicles() {
        return catalogService.findVehicles();
    }

    @GetMapping("/agents")
    public List<AgentResponse> agents() {
        return catalogService.findAgents();
    }

    @GetMapping("/waste-categories")
    public List<WasteCategoryResponse> wasteCategories() {
        return catalogService.findWasteCategories();
    }
}
