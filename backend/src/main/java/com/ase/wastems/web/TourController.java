package com.ase.wastems.web;

import com.ase.wastems.service.TourService;
import com.ase.wastems.web.dto.TourResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read endpoints of the collection tours. */
@RestController
@RequestMapping("/api/tours")
public class TourController {

    private final TourService tourService;

    public TourController(TourService tourService) {
        this.tourService = tourService;
    }

    @GetMapping
    public List<TourResponse> tours() {
        return tourService.findTours();
    }
}
