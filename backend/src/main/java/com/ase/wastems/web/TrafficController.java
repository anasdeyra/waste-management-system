package com.ase.wastems.web;

import com.ase.wastems.traffic.TrafficOverlayService;
import com.ase.wastems.web.dto.TrafficOverlayResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Display-only NYC DOT traffic overlay (US-34); PLANNER and ADMIN only. Always 200. */
@RestController
public class TrafficController {

    private final TrafficOverlayService traffic;

    public TrafficController(TrafficOverlayService traffic) {
        this.traffic = traffic;
    }

    @GetMapping("/api/traffic/overlay")
    public TrafficOverlayResponse overlay() {
        return TrafficOverlayResponse.from(traffic.overlay());
    }
}
