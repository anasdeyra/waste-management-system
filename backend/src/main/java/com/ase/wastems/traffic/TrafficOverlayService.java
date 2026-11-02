package com.ase.wastems.traffic;

import com.ase.wastems.config.ServiceAreaProperties;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

/**
 * Serves the committed NYC DOT traffic snapshot as a display-only overlay (US-34, D-021). The
 * snapshot is read once; a missing or unreadable snapshot yields an unavailable overlay, never an
 * exception. Nothing else in the application references this package.
 */
@Service
public class TrafficOverlayService {

    public static final String COVERAGE_NOTE = "NYC DOT sensors cover mostly major arterials and highways. "
            + "Streets without a drawn traffic link have no traffic data; they are not free-flowing.";
    public static final String DISPLAY_ONLY_NOTE =
            "Display only: traffic never changes route costs, optimization results or tours (D-021).";
    public static final String ATTRIBUTION = "Traffic speeds: NYC DOT, NYC Open Data (i4gi-tjb9)";
    public static final String DISCLAIMER = "This application may contain and/or utilize information which was "
            + "originally compiled by the New York City Department of Transportation (NYC DOT) for governmental "
            + "purposes; the information may subsequently been modified by entity/entities other than NYC DOT. "
            + "NYC DOT and the City of New York make no representation as to the accuracy or usefulness of the "
            + "information provided by this application or the information's suitability for any purpose and "
            + "disclaim any liability for omissions or errors that may be contained therein. The public is advised "
            + "to observe posted signage for compliance with applicable laws and regulations.";

    private final TrafficProperties props;
    private final ServiceAreaProperties area;
    private final ResourceLoader loader;
    private final Clock clock;
    private TrafficSnapshot snapshot;
    private String unavailableReason;
    private boolean loaded;

    @Autowired
    public TrafficOverlayService(TrafficProperties props, ServiceAreaProperties area, ResourceLoader loader) {
        this(props, area, loader, Clock.systemUTC());
    }

    TrafficOverlayService(TrafficProperties props, ServiceAreaProperties area, ResourceLoader loader, Clock clock) {
        this.props = props;
        this.area = area;
        this.loader = loader;
        this.clock = clock;
    }

    public TrafficOverlay overlay() {
        load();
        if (snapshot == null) {
            return new TrafficOverlay(false, unavailableReason, null, null, props.staleAfterMinutes());
        }
        long age = Math.max(0, Duration.between(snapshot.observedAt(), Instant.now(clock)).toMinutes());
        return new TrafficOverlay(true, null, snapshot, age, props.staleAfterMinutes());
    }

    private synchronized void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        String location = props.snapshotLocation();
        Resource resource = loader.getResource(location);
        if (!resource.exists()) {
            unavailableReason = "Traffic snapshot not found: " + location;
            return;
        }
        byte[] bytes;
        try (InputStream in = resource.getInputStream()) {
            bytes = in.readAllBytes();
        } catch (IOException e) {
            unavailableReason = "Traffic snapshot is not valid JSON: " + location;
            return;
        }
        try {
            snapshot = new TrafficSnapshotParser(area, props.staleAfterMinutes(), props.maxPlausibleSpeedMph())
                    .parse(bytes);
        } catch (IllegalArgumentException e) {
            unavailableReason = "no records".equals(e.getMessage())
                    ? "Traffic snapshot has no records: " + location
                    : "Traffic snapshot is not valid JSON: " + location;
        } catch (RuntimeException e) {
            unavailableReason = "Traffic snapshot is not valid JSON: " + location;
        }
    }
}
